package caisse.manager.caisse.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.hibernate.proxy.HibernateProxy;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private interface HibernateProxyMixin {
    }

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer hibernateProxyMixinCustomizer() {
        return builder -> builder.mixIn(HibernateProxy.class, HibernateProxyMixin.class);
    }
}
