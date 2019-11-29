package site.zido.coffee.security.authentication.phone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.cache.Cache;
import org.springframework.cache.support.NoOpCache;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.SpringSecurityMessageSource;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.core.authority.mapping.NullAuthoritiesMapper;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.util.Assert;

/**
 * 手机号验证码用户认证
 *
 * @author zido
 */
public class PhoneAuthUserAuthenticationProvider implements AuthenticationProvider, InitializingBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(PhoneAuthUserAuthenticationProvider.class);
    private PhoneAuthUserService userService;
    protected MessageSourceAccessor messages = SpringSecurityMessageSource.getAccessor();
    private GrantedAuthoritiesMapper authoritiesMapper = new NullAuthoritiesMapper();
    private Cache userCache = new NoOpCache("phone");
    private CodeValidator codeValidator = new CustomCodeValidator();

    public PhoneAuthUserAuthenticationProvider(PhoneAuthUserService userService) {
        this.userService = userService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {

        if (authentication.getCredentials() == null) {
            LOGGER.debug("Authentication failed: no credentials provided");

            throw new BadCredentialsException(messages.getMessage(
                    "AbstractUserDetailsAuthenticationProvider.badCredentials",
                    "Bad credentials"));
        }
        String principal = (authentication.getPrincipal() == null) ? "NONE_PROVIDED"
                : authentication.getName();
        if (!codeValidator.validate(principal, (String) authentication.getCredentials())) {
            throw new BadCredentialsException(messages.getMessage(
                    "AbstractUserDetailsAuthenticationProvider.badCredentials",
                    "Bad credentials"));
        }
        Cache.ValueWrapper wrapper = this.userCache.get(principal);
        UserDetails user = null;
        if (wrapper != null) {
            user = (UserDetails) wrapper.get();
        }
        if (user == null) {
            try {
                user = userService.loadByPhone(principal);
                userCache.put(principal, user);
            } catch (UsernameNotFoundException | InternalAuthenticationServiceException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new InternalAuthenticationServiceException(ex.getMessage(), ex);
            }
            Assert.notNull(user,
                    "retrieveUser returned null - a violation of the interface contract");
        }
        UsernamePasswordAuthenticationToken result = new UsernamePasswordAuthenticationToken(user, authentication.getCredentials(),
                authoritiesMapper.mapAuthorities(user.getAuthorities()));
        result.setDetails(authentication.getDetails());
        return result;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return (PhoneCodeAuthenticationToken.class
                .isAssignableFrom(authentication));
    }

    public void setUserCache(Cache userCache) {
        this.userCache = userCache;
    }

    public void setAuthoritiesMapper(GrantedAuthoritiesMapper authoritiesMapper) {
        this.authoritiesMapper = authoritiesMapper;
    }

    public void setCodeValidator(CodeValidator codeValidator) {
        this.codeValidator = codeValidator;
    }

    @Override
    public final void afterPropertiesSet() throws Exception {
        Assert.notNull(this.userCache, "A user cache must be set");
    }
}