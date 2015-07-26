package com.heapwhisperer.jvm.monitoring.dropwizard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@Configuration
@ComponentScan // required to find other components in this package and subpackages
@EnableAutoConfiguration // Spring Boot depends on autoconfigs to start up properly
@EnableScheduling // Required to use scheduled task capability
public class AppSpringConfiguration {

    @Autowired
    ConfigurableApplicationContext ctx;

    // simple request simulator that generates steady stream of garbage for JVM to clean up
    @Scheduled(fixedRate = 5, initialDelay = 5000)
    public void simulateRequestVolume() {
        new RestTemplate().getForEntity("http://[::1]:8080/do/business", String.class);
    }

    /**
     * Entry point to demo. Creates Spring Boot application,
     * and adds an initializer to set a mock environment.
     */
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(AppSpringConfiguration.class);
        List<ApplicationContextInitializer<ConfigurableApplicationContext>> initializers =
                new ArrayList<ApplicationContextInitializer<ConfigurableApplicationContext>>();
        initializers.add(new PropertyMockingApplicationContextInitializer());
        app.setInitializers(initializers);
        ConfigurableApplicationContext ctx = app.run(args);

        // print out names of all beans in context
        String[] beanNames = ctx.getBeanDefinitionNames();
        Arrays.sort(beanNames);
        for (String beanName : beanNames) {
            System.out.println(beanName);
        }
    }

    /**
     * For demo purposes, use a mock environment so that environment variables are
     * deterministic i.e not dependent on a developer's machine. In a real deployment,
     * environmental config should be injected as environment variables by the deployment process.
     */
    public static class PropertyMockingApplicationContextInitializer
            implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            MockEnvironment mockEnvironment = new MockEnvironment();
            mockEnvironment.setProperty("server.port", "8080");
            /* Uncomment the following line to get the GraphiteReporter instead of the ConsoleReporter */
            // mockEnvironment.setProperty("graphite.host", "localhost");
            applicationContext.setEnvironment(mockEnvironment);
        }
    }

    /**
     * Basic controller where you would call various services for your business logic.
     */
    @RestController
    public static class BusinessLogicController {

        @RequestMapping(value = "/do/business", method = RequestMethod.GET)
        public String businessLogic() {
            return "Hi, I am your business logic. I added business value, and created lots of garbage.";
        }
    }
}