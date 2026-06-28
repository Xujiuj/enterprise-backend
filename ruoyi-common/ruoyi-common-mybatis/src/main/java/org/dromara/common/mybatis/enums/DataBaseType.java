package org.dromara.common.mybatis.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.dromara.common.core.utils.StringUtils;

/**
 * Database type for the current delivery.
 *
 * @author Lion Li
 */
@Getter
@AllArgsConstructor
public enum DataBaseType {

    /**
     * SQL Server.
     */
    SQL_SERVER("Microsoft SQL Server");

    /**
     * Database product name.
     */
    private final String type;

    /**
     * Resolve and validate the configured database product.
     *
     * @param databaseProductName database product name from JDBC metadata
     * @return current supported database type
     */
    public static DataBaseType find(String databaseProductName) {
        if (StringUtils.isBlank(databaseProductName)) {
            throw new IllegalArgumentException("Database product name is blank; SQL Server is required");
        }
        for (DataBaseType type : values()) {
            if (type.getType().equals(databaseProductName)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unsupported database product: " + databaseProductName + "; SQL Server is required");
    }

    /**
     * Whether the database is SQL Server.
     */
    public boolean isSqlServer() {
        return this == SQL_SERVER;
    }

}
