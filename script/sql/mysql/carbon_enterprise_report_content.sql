-- Enterprise report Content catalog from customer sample workbook.
-- Database boundary: enterprise only.

CREATE TABLE IF NOT EXISTS ce_report_content (
    id BIGINT NOT NULL AUTO_INCREMENT,
    directory_no INT DEFAULT NULL,
    directory_name VARCHAR(255) DEFAULT NULL,
    subdirectory_no INT DEFAULT NULL,
    subdirectory_name VARCHAR(255) DEFAULT NULL,
    chart_names TEXT DEFAULT NULL,
    display_order INT NOT NULL DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    remark VARCHAR(500) DEFAULT NULL,
    PRIMARY KEY (id),
    KEY idx_ce_report_content_order (display_order),
    KEY idx_ce_report_content_directory (directory_no, subdirectory_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Enterprise report Content catalog';
