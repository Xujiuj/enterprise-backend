package org.dromara.common.mybatis.helper;

import cn.hutool.core.convert.Convert;
import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.dromara.common.core.utils.SpringUtils;
import org.dromara.common.mybatis.enums.DataBaseType;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Database helper for the SQL Server-only delivery.
 *
 * @author Lion Li
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DataBaseHelper {

    private static final DynamicRoutingDataSource DS = SpringUtils.getBean(DynamicRoutingDataSource.class);

    public static DataBaseType getDataBaseType() {
        DataSource dataSource = DS.determineDataSource();
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            return DataBaseType.find(metaData.getDatabaseProductName());
        } catch (SQLException e) {
            throw new RuntimeException("Failed to resolve database type", e);
        }
    }

    public static DataBaseType getDataBaseType(String dsName) {
        DataSource dataSource = DS.getDataSource(dsName);
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            return DataBaseType.find(metaData.getDatabaseProductName());
        } catch (SQLException e) {
            throw new RuntimeException("Failed to resolve database type", e);
        }
    }

    public static String findInSet(Object var1, String var2) {
        String var = Convert.toStr(var1);
        return "charindex(',%s,' , ','+%s+',') <> 0".formatted(var, var2);
    }

    public static List<String> getDataSourceNameList() {
        return new ArrayList<>(DS.getDataSources().keySet());
    }
}
