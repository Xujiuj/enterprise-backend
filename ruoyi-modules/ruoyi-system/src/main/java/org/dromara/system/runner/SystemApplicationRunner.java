package org.dromara.system.runner;

import org.dromara.system.service.ISysOssConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * 初始化 system 模块对应业务数据
 *
 * @author Lion Li
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class SystemApplicationRunner implements ApplicationRunner {

    private final ISysOssConfigService ossConfigService;
    private final DataSource dataSource;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        syncEnterprisePortalMenu();
        ossConfigService.init();
        log.info("初始化OSS配置成功");
    }

    private void syncEnterprisePortalMenu() {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(
            new ClassPathResource("sql/enterprise_portal_menu_sync.sql")
        );
        populator.setSeparator(";");
        populator.setSqlScriptEncoding("UTF-8");
        populator.execute(dataSource);
        log.info("企业端门户目录已按意见反馈20260602.md同步");
    }

}
