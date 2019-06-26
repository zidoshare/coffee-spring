package site.zido.coffee.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactoryBean;
import org.springframework.web.util.UrlPathHelper;
import site.zido.coffee.CommonAutoConfiguration;
import site.zido.coffee.auth.annotations.PermissionInterceptor;
import site.zido.coffee.auth.entity.IUser;
import site.zido.coffee.auth.entity.annotations.AuthEntity;
import site.zido.coffee.auth.handlers.*;
import site.zido.coffee.auth.handlers.jpa.JpaAuthHandler;
import site.zido.coffee.common.rest.DefaultHttpResponseBodyFactory;
import site.zido.coffee.common.rest.HttpResponseBodyFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static site.zido.coffee.auth.Constants.DEFAULT_LOGIN_URL;

/**
 * @author zido
 */
@Configuration
@AutoConfigureAfter({JpaRepositoriesAutoConfiguration.class,
        JacksonAutoConfiguration.class,
        CommonAutoConfiguration.JsonAutoConfiguration.class
})
public class AuthAutoConfiguration implements BeanFactoryAware {
    private static final String ERROR_WHEN_MULTI = String.format("多用户实体时需要使用%s标记，" +
            "并提供不同的url以帮助识别登录用户", AuthEntity.class.getName());
    private BeanFactory beanFactory;
    private AuthenticatorFactory authenticatorFactory;
    private AuthenticationFilter filter;

    /**
     * 注册拦截器，用于权限校验
     *
     * @param disabledUserHandler  禁用用户处理
     * @param loginExpectedHandler 需要登陆处理
     * @return interceptor
     */
    @Bean
    @ConditionalOnMissingBean(PermissionInterceptor.class)
    public PermissionInterceptor interceptor(DisabledUserHandler disabledUserHandler,
                                             LoginExpectedHandler loginExpectedHandler) {
        PermissionInterceptor interceptor = new PermissionInterceptor();
        interceptor.setDisabledUserHandler(disabledUserHandler);
        interceptor.setLoginExpectedHandler(loginExpectedHandler);
        return interceptor;
    }

    /**
     * 注册过滤器,用于认证/授权
     *
     * @param loginSuccessHandler 登录成功处理
     * @param loginFailureHandler 登录失败处理
     * @return filter
     */
    @Bean
    @ConditionalOnMissingBean(AuthenticationFilter.class)
    public AuthenticationFilter getFilter(LoginSuccessHandler loginSuccessHandler,
                                          LoginFailureHandler loginFailureHandler) {
        this.filter = new AuthenticationFilter();
        Map<String, JpaRepositoryFactoryBean> jpaRepositoryFactoryBeanMap = BeanFactoryUtils.beansOfTypeIncludingAncestors((ListableBeanFactory) beanFactory, JpaRepositoryFactoryBean.class);
        Map<String, AuthHandler<? extends IUser>> map = new HashMap<>();
        for (JpaRepositoryFactoryBean factoryBean : jpaRepositoryFactoryBeanMap.values()) {
            Class<?> javaType = factoryBean.getEntityInformation().getJavaType();
            if (javaType.isAssignableFrom(IUser.class)) {
                JpaRepository repository = (JpaRepository) factoryBean.getObject();
                AuthEntity annotation = AnnotationUtils.getAnnotation(javaType, AuthEntity.class);
                String url;
                if (annotation != null) {
                    url = annotation.url().trim();
                    if (!url.startsWith("/")) {
                        url = "/" + url;
                    }
                } else {
                    url = DEFAULT_LOGIN_URL;
                }
                if (map.get(url) != null) {
                    throw new IllegalArgumentException(ERROR_WHEN_MULTI);
                }
                map.put(url, new JpaAuthHandler<>(javaType, authenticatorFactory.newChains(javaType)));
            }
        }
        if (map.isEmpty()) {
            //TODO don't register filter
            return null;
        }
        map = Collections.unmodifiableMap(map);
        filter.setHandlerMap(map);
        filter.setAuthenticationSuccessHandler(loginSuccessHandler);
        filter.setAuthenticationFailureHandler(loginFailureHandler);
        return filter;
    }

    @Bean
    @ConditionalOnMissingBean(HttpResponseBodyFactory.class)
    public HttpResponseBodyFactory responseBodyFactory() {
        return new DefaultHttpResponseBodyFactory();
    }

    @Bean
    @ConditionalOnMissingBean(DisabledUserHandler.class)
    @Autowired(required = false)
    public DisabledUserHandler disabledUserHandler(ObjectMapper mapper) {
        return new RestDisabledUserHandler(responseBodyFactory(), mapper);
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Bean
    @ConditionalOnMissingBean(LoginSuccessHandler.class)
    @Autowired(required = false)
    public LoginSuccessHandler loginSuccessHandler(ObjectMapper mapper) {
        return new RestLoginSuccessHandler(responseBodyFactory(), mapper);
    }


    @Bean
    @ConditionalOnMissingBean(LoginFailureHandler.class)
    @Autowired(required = false)
    public LoginFailureHandler loginFailureHandler(ObjectMapper mapper) {
        return new RestLoginFailureHandler(responseBodyFactory(), mapper);
    }

    @Bean
    @ConditionalOnMissingBean(LoginExpectedHandler.class)
    @Autowired(required = false)
    public LoginExpectedHandler loginExpectedHandler(ObjectMapper mapper) {
        return new RestLoginExceptedHandler(responseBodyFactory(), mapper);
    }

    @Autowired(required = false)
    public void setPathUrlHelper(UrlPathHelper helper) {
        filter.setUrlPathHelper(helper);
    }

}
