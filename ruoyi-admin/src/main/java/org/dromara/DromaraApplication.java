package org.dromara;

import org.dromara.enterprise.config.EnterpriseSecureConfigSetup;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup;

import java.util.Arrays;

/**
 * 启动程序
 *
 * @author Lion Li
 */

@SpringBootApplication
public class DromaraApplication {

    public static void main(String[] args) {
        if (Arrays.asList(args).contains("--enterprise-setup")) {
            try {
                EnterpriseSecureConfigSetup.run(args);
                return;
            } catch (Exception e) {
                System.err.println("Enterprise secure config setup failed: " + e.getMessage());
                System.exit(1);
            }
        }
        SpringApplication application = new SpringApplication(DromaraApplication.class);
        application.setApplicationStartup(new BufferingApplicationStartup(2048));
        application.run(args);
        System.out.println("Enterprise Carbon Data Management Platform started successfully.");
    }

}
