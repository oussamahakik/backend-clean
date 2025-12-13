package caisse.manager.caisse.config;

import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration Hibernate pour désactiver la création automatique des contraintes FK
 * afin d'éviter les erreurs "Foreign key constraint is incorrectly formed"
 */
@Configuration
public class HibernateConfig {

    @Bean
    public HibernatePropertiesCustomizer hibernatePropertiesCustomizer() {
        return (properties) -> {
            // Désactiver la création automatique des contraintes FK
            properties.put("hibernate.hbm2ddl.auto", "update");
            properties.put("hibernate.schema_update.unique_constraint_strategy", "skip");
            // Ne pas créer les contraintes FK automatiquement
            properties.put("hibernate.hbm2ddl.jdbc_metadata_extraction_strategy", "individually");
        };
    }
}

