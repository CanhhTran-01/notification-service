package com.project.notificationservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.StringTemplateResolver;

@Configuration
public class TemplateEngineConfig {

    @Bean(name = "stringTemplateEngine")
    public SpringTemplateEngine stringTemplateEngine() {

        SpringTemplateEngine engine = new SpringTemplateEngine();
        StringTemplateResolver resolver = new StringTemplateResolver();

        resolver.setTemplateMode(TemplateMode.HTML);
        engine.setTemplateResolver(resolver);

        return engine;
    }
}
