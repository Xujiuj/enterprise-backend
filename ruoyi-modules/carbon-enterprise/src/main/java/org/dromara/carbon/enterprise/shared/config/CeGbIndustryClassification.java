package org.dromara.carbon.enterprise.shared.config;

import java.util.List;
import java.util.Map;

/**
 * GB/T 4754-2017 factory industry classification used by enterprise-side forms.
 */
public final class CeGbIndustryClassification {

    public static final String REMARK = "GB/T 4754-2017 工厂行业划分";
    public static final String LEGACY_REMARK = "Source(A) 102公司表参考数据";

    public static final IndustryRecord HEADQUARTERS = new IndustryRecord(
        1, "L", "租赁和商务服务业", "72", "商务服务业", "721", "组织管理服务", "7211", "企业总部管理");
    public static final IndustryRecord POLYSILICON = new IndustryRecord(
        2, "C", "制造业", "39", "计算机、通信和其他电子设备制造业", "398", "电子元件及电子专用材料制造", "3985", "电子专用材料制造");
    public static final IndustryRecord THERMAL_POWER = new IndustryRecord(
        3, "D", "电力、热力、燃气及水生产和供应业", "44", "电力、热力生产和供应业", "441", "电力生产", "4411", "火力发电");
    public static final IndustryRecord CEMENT = new IndustryRecord(
        4, "C", "制造业", "30", "非金属矿物制品业", "301", "水泥、石灰和石膏制造", "3011", "水泥制造");

    private static final List<IndustryRecord> RECORDS = List.of(HEADQUARTERS, POLYSILICON, THERMAL_POWER, CEMENT);
    private static final Map<String, IndustryRecord> BY_FACTORY_TYPE = Map.of(
        "集团总部", HEADQUARTERS,
        "多晶硅生产", POLYSILICON,
        "电力生产", THERMAL_POWER,
        "水泥生产", CEMENT
    );

    private CeGbIndustryClassification() {
    }

    public static List<IndustryRecord> records() {
        return RECORDS;
    }

    public static IndustryRecord byFactoryType(String factoryType) {
        return BY_FACTORY_TYPE.get(factoryType);
    }

    public record IndustryRecord(
        int sortOrder,
        String sectionCode,
        String sectionName,
        String divisionCode,
        String divisionName,
        String groupCode,
        String groupName,
        String classCode,
        String className
    ) {
    }
}
