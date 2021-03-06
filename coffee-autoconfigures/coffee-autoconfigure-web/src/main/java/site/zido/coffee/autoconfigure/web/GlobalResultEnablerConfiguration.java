package site.zido.coffee.autoconfigure.web;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import site.zido.coffee.mvc.rest.EnableGlobalResult;

@EnableGlobalResult
@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(value = "spring.coffee.web.global-result", matchIfMissing = true, havingValue = "true")
@Import(JavaxExceptionEnablerConfiguration.class)
public class GlobalResultEnablerConfiguration {
}
