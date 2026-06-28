package org.dromara.carbon.enterprise.license.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.carbon.enterprise.dimension.domain.CeDimensionSyncResponse;
import org.dromara.carbon.enterprise.factor.domain.CeFactorSyncResponse;
import org.dromara.carbon.enterprise.license.domain.CeLicenseState;
import org.dromara.carbon.enterprise.license.domain.CeLicenseEnvelope;
import org.dromara.carbon.enterprise.license.domain.CeLicenseImportResult;
import org.dromara.carbon.enterprise.license.domain.CeLicensePayload;
import org.dromara.carbon.enterprise.license.mapper.CeLicenseStateMapper;
import org.dromara.carbon.enterprise.shared.service.ICeDimensionSyncService;
import org.dromara.carbon.enterprise.shared.service.ICeFactorSyncService;
import org.dromara.carbon.enterprise.shared.service.ICeLicenseImportService;
import org.dromara.common.core.utils.StringUtils;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Comparator;
import java.util.Date;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

/**
 * Enterprise-side license import and verification service implementation.
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class CeLicenseImportServiceImpl implements ICeLicenseImportService {

    private static final String SCHEMA_VERSION = "license.v1";
    private static final String ALGORITHM = "RS256";
    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";
    private static final String PENDING_INSTALL_ID = "__PENDING_ENTERPRISE_ACTIVATION__";
    private static final String PEM_PUBLIC_KEY_BEGIN = "-----BEGIN PUBLIC KEY-----";
    private static final String PEM_PUBLIC_KEY_END = "-----END PUBLIC KEY-----";

    private final CeLicenseStateMapper licenseStateMapper;
    private final ObjectMapper objectMapper;
    private final ICeDimensionSyncService dimensionSyncService;
    private final ICeFactorSyncService factorSyncService;

    @Override
    public CeLicenseImportResult verifyLicense(String licenseContent, String publicKeyPem, String expectedInstallId,
                                               Date verificationTime, Date maxObservedTime) {
        try {
            CeLicenseEnvelope envelope = objectMapper.readValue(licenseContent, CeLicenseEnvelope.class);
            CeLicenseImportResult structureResult = validateEnvelope(envelope);
            if (!structureResult.isValid()) {
                return structureResult;
            }

            byte[] canonicalPayload = objectMapper.writeValueAsString(envelope.getPayload())
                .getBytes(StandardCharsets.UTF_8);
            if (!verifySignature(publicKeyPem, canonicalPayload, envelope.getSignature())) {
                return CeLicenseImportResult.invalid("SIGNATURE_INVALID", "授权文件签名校验失败");
            }

            CeLicensePayload payload = objectMapper.treeToValue(envelope.getPayload(), CeLicensePayload.class);
            CeLicenseImportResult payloadResult = validatePayload(envelope, payload);
            if (!payloadResult.isValid()) {
                return payloadResult;
            }

            Date checkedAt = Objects.requireNonNullElseGet(verificationTime, Date::new);
            Date validFrom = parseInstant(payload.getValidFrom(), "validFrom");
            Date validTo = parseInstant(payload.getValidTo(), "validTo");

            if (checkedAt.before(validFrom)) {
                return CeLicenseImportResult.invalid("NOT_YET_VALID", "授权尚未到生效时间");
            }
            if (checkedAt.after(validTo)) {
                return CeLicenseImportResult.invalid("EXPIRED", "授权已过期");
            }
            String effectiveInstallId = resolveEffectiveInstallId(payload.getInstallId(), expectedInstallId);
            if (!Objects.equals(expectedInstallId, effectiveInstallId)) {
                return CeLicenseImportResult.invalid("INSTALL_ID_MISMATCH", "授权文件的部署指纹与本机不匹配");
            }
            if (maxObservedTime != null && checkedAt.before(maxObservedTime)) {
                return CeLicenseImportResult.invalid("CLOCK_ROLLBACK", "系统时间早于最近授权校验时间");
            }

            return CeLicenseImportResult.valid(buildLicenseState(envelope, payload, effectiveInstallId, canonicalPayload, validFrom, validTo, checkedAt, maxObservedTime));
        } catch (Exception e) {
            return CeLicenseImportResult.invalid("MALFORMED_LICENSE", "授权文件内容格式不正确");
        }
    }

    @Override
    public CeLicenseImportResult importLicense(String licenseContent, String publicKeyPem, String expectedInstallId,
                                                Date verificationTime) {
        CeLicenseImportResult result = verifyLicense(licenseContent, publicKeyPem, expectedInstallId,
            verificationTime, findMaxObservedTime());
        if (result.isValid()) {
            licenseStateMapper.insert(result.getLicenseState());

            String dimensionSyncMessage;
            try {
                List<CeDimensionSyncResponse> dimensionResults = dimensionSyncService.syncAllVendorDimensions();
                int successDimensions = (int) dimensionResults.stream().filter(CeDimensionSyncResponse::isSuccess).count();
                int syncedRows = dimensionResults.stream().mapToInt(CeDimensionSyncResponse::getRecordCount).sum();
                dimensionSyncMessage = "维度数据同步完成：" + successDimensions + " 类维度，" + syncedRows + " 条记录";
                log.info("授权导入成功，{}", dimensionSyncMessage);
            } catch (Exception e) {
                dimensionSyncMessage = "维度数据同步失败，请稍后在维度管理中手动同步";
                log.warn("授权导入成功，但维度数据同步失败", e);
            }

            String factorSyncMessage;
            try {
                CeFactorSyncResponse factorResult = factorSyncService.syncCurrentLicenseFactors();
                factorSyncMessage = "因子数据同步完成：版本 " + StringUtils.blankToDefault(factorResult.getVersionCode(), "-")
                    + "，" + factorResult.getRecordCount() + " 条记录"
                    + (factorResult.isChanged() ? "，已有更新" : "，无新增变更");
                log.info("授权导入成功，{}", factorSyncMessage);
            } catch (Exception e) {
                factorSyncMessage = "因子数据同步失败，请稍后在因子管理中手动同步";
                log.warn("授权导入成功，但因子数据同步失败", e);
            }
            result = result.withSyncMessage("授权已导入。厂商端同步结果：" + dimensionSyncMessage + "；" + factorSyncMessage + "。");
        }
        return result;
    }

    private CeLicenseImportResult validateEnvelope(CeLicenseEnvelope envelope) {
        if (envelope == null) {
            return CeLicenseImportResult.invalid("MALFORMED_LICENSE", "授权文件内容为空");
        }
        if (!SCHEMA_VERSION.equals(envelope.getSchemaVersion())) {
            return CeLicenseImportResult.invalid("UNSUPPORTED_SCHEMA", "授权文件版本不受支持");
        }
        if (!ALGORITHM.equals(envelope.getAlgorithm())) {
            return CeLicenseImportResult.invalid("UNSUPPORTED_ALGORITHM", "授权文件签名算法不受支持");
        }
        if (StringUtils.isBlank(envelope.getKeyId()) || envelope.getPayload() == null || StringUtils.isBlank(envelope.getSignature())) {
            return CeLicenseImportResult.invalid("MALFORMED_LICENSE", "授权文件缺少必要字段");
        }
        return CeLicenseImportResult.valid(null);
    }

    private CeLicenseImportResult validatePayload(CeLicenseEnvelope envelope, CeLicensePayload payload) {
        if (payload == null
            || StringUtils.isAnyBlank(payload.getLicenseId(), payload.getCustomerId(), payload.getCustomerName(),
            payload.getEdition(), payload.getInstallId(), payload.getValidFrom(), payload.getValidTo(),
            payload.getIssuedAt(), payload.getIssuer(), payload.getKeyId())
            || payload.getFeatures() == null || payload.getFeatures().isEmpty()
            || payload.getTemplateEntitlements() == null || payload.getTemplateEntitlements().isEmpty()) {
            return CeLicenseImportResult.invalid("MALFORMED_LICENSE", "授权载荷缺少必要字段");
        }
        if (!Objects.equals(envelope.getKeyId(), payload.getKeyId())) {
            return CeLicenseImportResult.invalid("KEY_ID_MISMATCH", "授权文件签名密钥不一致");
        }
        return CeLicenseImportResult.valid(null);
    }

    private boolean verifySignature(String publicKeyPem, byte[] canonicalPayload, String signatureText) throws Exception {
        Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
        signature.initVerify(parsePublicKey(publicKeyPem));
        signature.update(canonicalPayload);
        return signature.verify(Base64.getDecoder().decode(signatureText));
    }

    private PublicKey parsePublicKey(String publicKeyPem) throws Exception {
        String normalized = publicKeyPem
            .replace(PEM_PUBLIC_KEY_BEGIN, "")
            .replace(PEM_PUBLIC_KEY_END, "")
            .replaceAll("\\s", "");
        byte[] encoded = Base64.getDecoder().decode(normalized);
        return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(encoded));
    }

    private Date parseInstant(String value, String fieldName) {
        try {
            return Date.from(Instant.parse(value));
        } catch (Exception e) {
            throw new IllegalArgumentException(fieldName + " 必须是 ISO-8601 UTC 时间", e);
        }
    }

    private String resolveEffectiveInstallId(String licenseInstallId, String expectedInstallId) {
        if (PENDING_INSTALL_ID.equals(licenseInstallId) && StringUtils.isNotBlank(expectedInstallId)) {
            return expectedInstallId;
        }
        return licenseInstallId;
    }

    private CeLicenseState buildLicenseState(CeLicenseEnvelope envelope, CeLicensePayload payload, String effectiveInstallId,
                                             byte[] canonicalPayload, Date validFrom, Date validTo, Date checkedAt,
                                             Date maxObservedTime) {
        CeLicenseState state = new CeLicenseState();
        state.setLicenseId(payload.getLicenseId());
        state.setCustomerId(payload.getCustomerId());
        state.setPackageId(payload.getPackageId());
        state.setPackageName(payload.getPackageName());
        state.setInstallId(effectiveInstallId);
        state.setKeyId(envelope.getKeyId());
        state.setAlgorithm(envelope.getAlgorithm());
        state.setSchemaVersion(envelope.getSchemaVersion());
        state.setValidFrom(validFrom);
        state.setValidTo(validTo);
        state.setLastVerifiedTime(checkedAt);
        state.setMaxObservedTime(laterOf(checkedAt, maxObservedTime));
        state.setFeatureCodes(String.join(",", payload.getFeatures()));
        state.setPayloadDigest(sha256Hex(canonicalPayload));
        state.setCurrentSummary(buildCurrentSummary(payload));
        state.setLicenseStatus("VALID");
        return state;
    }

    private String sha256Hex(byte[] canonicalPayload) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonicalPayload));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 digest is unavailable", e);
        }
    }

    private String buildCurrentSummary(CeLicensePayload payload) {
        return "客户=" + payload.getCustomerName()
            + "；授权套餐编号=" + (payload.getPackageId() == null ? "" : payload.getPackageId())
            + "；授权套餐=" + StringUtils.blankToDefault(payload.getPackageName(), "")
            + "；版本=" + payload.getEdition()
            + "；功能=" + String.join(",", payload.getFeatures())
            + "；模板授权数=" + payload.getTemplateEntitlements().size();
    }

    private Date laterOf(Date left, Date right) {
        if (right == null || left.after(right)) {
            return left;
        }
        return right;
    }

    private Date findMaxObservedTime() {
        List<CeLicenseState> states = licenseStateMapper.selectList(new LambdaQueryWrapper<CeLicenseState>()
            .isNotNull(CeLicenseState::getMaxObservedTime)
            .orderByDesc(CeLicenseState::getMaxObservedTime));
        return states.stream()
            .map(CeLicenseState::getMaxObservedTime)
            .filter(Objects::nonNull)
            .max(Comparator.naturalOrder())
            .orElse(null);
    }
}
