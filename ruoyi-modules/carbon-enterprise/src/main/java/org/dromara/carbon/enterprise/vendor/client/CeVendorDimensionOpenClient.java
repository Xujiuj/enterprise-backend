package org.dromara.carbon.enterprise.vendor.client;

import org.dromara.carbon.enterprise.dimension.domain.bo.CeDimensionRecordBo;
import org.dromara.carbon.enterprise.vendor.domain.CeVendorDimensionListResponse;

/**
 * Client for vendor open dimension API.
 */
public interface CeVendorDimensionOpenClient {

    /**
     * Fetch license-scoped dimension records from vendor backend.
     *
     * @param licenseId enterprise license id
     * @param installId enterprise install id
     * @param query dimension query
     * @param pageNum current page number
     * @param pageSize page size
     * @return vendor dimension list response
     */
    CeVendorDimensionListResponse listDimensions(
        String licenseId,
        String installId,
        CeDimensionRecordBo query,
        Integer pageNum,
        Integer pageSize
    );
}
