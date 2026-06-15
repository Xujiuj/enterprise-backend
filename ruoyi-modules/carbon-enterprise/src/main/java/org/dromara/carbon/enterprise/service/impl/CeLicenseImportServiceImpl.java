package org.dromara.carbon.enterprise.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.dromara.carbon.enterprise.domain.CeLicenseState;
import org.dromara.carbon.enterprise.domain.license.CeLicenseEnvelope;
import org.dromara.carbon.enterprise.domain.license.CeLicenseImportResult;
import org.dromara.carbon.enterprise.domain.license.CeLicensePayload;
import org.dromara.carbon.enterprise.mapper.CeLicenseStateMapper;
import org.dromara.carbon.enterprise.service.ICeLicenseImportService;
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
@RequiredArgsConstructor
@Service
public class CeLicenseImportServiceImpl implements ICeLicenseImportService {

    private static final String SCHEMA_VERSION = "license.v1";
    private static final String ALGORITHM = "RS256";
    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";
    private static final String PEM_PUBLIC_KEY_BEGIN = "-----BEGIN PUBLIC KEY-----";
    private static final String PEM_PUBLIC_KEY_END = "-----END PUBLIC KEY-----";

    private final CeLicenseStateMapper licenseStateMapper;
    private final ObjectMapper objectMapper;

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
                return CeLicenseImportResult.invalid("SIGNATURE_INVALID", "license signature is invalid");
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
                return CeLicenseImportResult.invalid("NOT_YET_VALID", "license is not valid yet");
            }
            if (checkedAt.after(validTo)) {
                return CeLicenseImportResult.invalid("EXPIRED", "license has expired");
            }
            if (!Objects.equals(expectedInstallId, payload.getInstallId())) {
                return CeLicenseImportResult.invalid("INSTALL_ID_MISMATCH", "license installId does not match local installId");
            }
            if (maxObservedTime != null && checkedAt.before(maxObservedTime)) {
                return CeLicenseImportResult.invalid("CLOCK_ROLLBACK", "system time is earlier than max observed time");
            }

            return CeLicenseImportResult.valid(buildLicenseState(envelope, payload, canonicalPayload, validFrom, validTo, checkedAt, maxObservedTime));
        } catch (Exception e) {
            return CeLicenseImportResult.invalid("MALFORMED_LICENSE", e.getMessage());
        }
    }

    @Override
    public CeLicenseImportResult importLicense(String licenseContent, String publicKeyPem, String expectedInstallId,
                                               Date verificationTime) {
        CeLicenseImportResult result = verifyLicense(licenseContent, publicKeyPem, expectedInstallId,
            verificationTime, findMaxObservedTime());
        if (result.isValid()) {
            licenseStateMapper.insert(result.getLicenseState());
        }
        return result;
    }

    private CeLicenseImportResult validateEnvelope(CeLicenseEnvelope envelope) {
        if (envelope == null) {
            return CeLicenseImportResult.invalid("MALFORMED_LICENSE", "license envelope is empty");
        }
        if (!SCHEMA_VERSION.equals(envelope.getSchemaVersion())) {
            return CeLicenseImportResult.invalid("UNSUPPORTED_SCHEMA", "unsupported license schemaVersion");
        }
        if (!ALGORITHM.equals(envelope.getAlgorithm())) {
            return CeLicenseImportResult.invalid("UNSUPPORTED_ALGORITHM", "unsupported license algorithm");
        }
        if (StringUtils.isBlank(envelope.getKeyId()) || envelope.getPayload() == null || StringUtils.isBlank(envelope.getSignature())) {
            return CeLicenseImportResult.invalid("MALFORMED_LICENSE", "license envelope misses required fields");
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
            return CeLicenseImportResult.invalid("MALFORMED_LICENSE", "license payload misses required fields");
        }
        if (!Objects.equals(envelope.getKeyId(), payload.getKeyId())) {
            return CeLicenseImportResult.invalid("KEY_ID_MISMATCH", "envelope keyId does not match payload keyId");
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
            throw new IllegalArgumentException(fieldName + " must be ISO-8601 UTC", e);
        }
    }

    private CeLicenseState buildLicenseState(CeLicenseEnvelope envelope, CeLicensePayload payload, byte[] canonicalPayload,
                                             Date validFrom, Date validTo, Date checkedAt, Date maxObservedTime) {
        CeLicenseState state = new CeLicenseState();
        state.setLicenseId(payload.getLicenseId());
        state.setCustomerId(payload.getCustomerId());
        state.setPackageId(payload.getPackageId());
        state.setPackageName(payload.getPackageName());
        state.setInstallId(payload.getInstallId());
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
        return "customerName=" + payload.getCustomerName()
            + ";packageId=" + (payload.getPackageId() == null ? "" : payload.getPackageId())
            + ";packageName=" + StringUtils.blankToDefault(payload.getPackageName(), "")
            + ";edition=" + payload.getEdition()
            + ";features=" + String.join(",", payload.getFeatures())
            + ";templateEntitlements=" + payload.getTemplateEntitlements().size();
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
