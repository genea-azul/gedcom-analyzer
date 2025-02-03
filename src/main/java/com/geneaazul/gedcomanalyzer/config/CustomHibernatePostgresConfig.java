package com.geneaazul.gedcomanalyzer.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
@ConditionalOnProperty(prefix = "spring.jpa", name = "database-platform", havingValue = "org.hibernate.dialect.PostgreSQLDialect")
public class CustomHibernatePostgresConfig implements HibernatePropertiesCustomizer {

    @Override
    public void customize(Map<String, Object> hibernateProperties) {
        hibernateProperties.put("hibernate.dialect", "com.geneaazul.gedcomanalyzer.repository.extend.CustomPostgresDialect");
    }

}
