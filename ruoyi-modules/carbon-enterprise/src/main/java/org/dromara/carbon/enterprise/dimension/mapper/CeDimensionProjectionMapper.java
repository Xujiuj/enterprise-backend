package org.dromara.carbon.enterprise.dimension.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.apache.ibatis.annotations.DeleteProvider;
import org.apache.ibatis.annotations.InsertProvider;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.SelectProvider;
import org.apache.ibatis.annotations.UpdateProvider;
import org.dromara.carbon.enterprise.dimension.domain.bo.CeDimensionRecordBo;
import org.dromara.carbon.enterprise.dimension.domain.vo.CeDimensionRecordVo;

import java.util.List;

/**
 * Read-only projections from concrete sample-aligned tables to legacy dimension page DTOs.
 */
@InterceptorIgnore(dataPermission = "true", tenantLine = "true")
public interface CeDimensionProjectionMapper {

    @SelectProvider(type = CeDimensionProjectionSqlProvider.class, method = "selectByDimensionCode")
    List<CeDimensionRecordVo> selectByDimensionCode(@Param("dimensionCode") String dimensionCode);

    @SelectProvider(type = CeDimensionProjectionSqlProvider.class, method = "selectByDimensionCodeAndId")
    CeDimensionRecordVo selectByDimensionCodeAndId(@Param("dimensionCode") String dimensionCode, @Param("id") Long id);

    @InsertProvider(type = CeDimensionProjectionSqlProvider.class, method = "insertByDimensionCode")
    int insertByDimensionCode(@Param("record") CeDimensionRecordBo record);

    @UpdateProvider(type = CeDimensionProjectionSqlProvider.class, method = "updateByDimensionCode")
    int updateByDimensionCode(@Param("record") CeDimensionRecordBo record);

    @DeleteProvider(type = CeDimensionProjectionSqlProvider.class, method = "deleteByDimensionCodeAndId")
    int deleteByDimensionCodeAndId(@Param("dimensionCode") String dimensionCode, @Param("id") Long id);

    @Select("select dimension_code from ce_dimension_edit_key where edit_key = #{editKey}")
    String selectDimensionCodeByEditKey(@Param("editKey") String editKey);
}
