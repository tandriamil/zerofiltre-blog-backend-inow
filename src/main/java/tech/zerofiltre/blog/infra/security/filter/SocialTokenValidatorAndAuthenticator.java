package tech.zerofiltre.blog.infra.security.filter;

import lombok.extern.slf4j.*;
import org.springframework.security.authentication.*;
import org.springframework.security.core.authority.*;
import org.springframework.security.core.context.*;
import tech.zerofiltre.blog.domain.*;
import tech.zerofiltre.blog.domain.error.*;
import tech.zerofiltre.blog.domain.metrics.*;
import tech.zerofiltre.blog.domain.metrics.model.*;
import tech.zerofiltre.blog.domain.user.*;
import tech.zerofiltre.blog.domain.user.model.*;
import tech.zerofiltre.blog.domain.user.use_cases.*;
import tech.zerofiltre.blog.infra.entrypoints.rest.*;

import java.util.*;
import java.util.stream.*;

@Slf4j
public class SocialTokenValidatorAndAuthenticator<L extends SocialLoginProvider> {

    private static final String SUCCESS = "success";
    final L socialLoginProvider;
    final UserProvider userProvider;
    private MetricsProvider metricsProvider;
    private final SecurityContextManager securityContextManager;

    public SocialTokenValidatorAndAuthenticator(L socialLoginProvider, UserProvider userProvider, MetricsProvider metricsProvider, SecurityContextManager securityContextManager) {
        this.socialLoginProvider = socialLoginProvider;
        this.userProvider = userProvider;
        this.metricsProvider = metricsProvider;
        this.securityContextManager = securityContextManager;
    }

    public void validateAndAuthenticate(String token) {
        try {    // exceptions might be thrown in validating the token: if for example the token is expired

            CounterSpecs counterSpecs = new CounterSpecs();

            // 4. Validate the token
            if (socialLoginProvider.isValid(token)) {
                //5. Get the user info from the token
                Optional<User> userOfToken = socialLoginProvider.userOfToken(token);
                if (userOfToken.isPresent()) {
                    User user = userOfToken.get();
                    //7. Check if user in DB, otherwise save him
                    Optional<User> foundUser = userProvider.userOfEmail(user.getEmail());
                    if (foundUser.isEmpty()) {
                        userProvider.save(user);
                        counterSpecs.setName(CounterSpecs.ZEROFILTRE_ACCOUNT_CREATIONS);
                        counterSpecs.setTags("from", user.getLoginFrom().toString(), SUCCESS, "true");
                        metricsProvider.incrementCounter(counterSpecs);

                    } else {
                        recordConnectionMetrics(counterSpecs, user.getLoginFrom().toString(), foundUser.get());
                    }
                    // 8. Create auth object
                    // UsernamePasswordAuthenticationToken: A built-in object, used by spring to represent the current authenticated / being authenticated user.
                    // It needs a list of authorities, which has type of GrantedAuthority interface, where SimpleGrantedAuthority is an implementation of that interface
                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                            user.getEmail(), null, user.getRoles().stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList()));

                    // 9. Authenticate the user
                    // Now, user is authenticated
                    SecurityContextHolder.getContext().setAuthentication(auth);

                }
            }

        } catch (Exception e) {
            log.error("Error when checking token ", e);
            // In case of failure. Make sure it's clear; so guarantee user won't be authenticated
            SecurityContextHolder.clearContext();
        }
    }

    private void recordConnectionMetrics(CounterSpecs counterSpecs, String loginFrom, User foundUser) throws UnAuthenticatedActionException {
        try {
            securityContextManager.getAuthenticatedUser();
        } catch (UserNotFoundException une) {
            counterSpecs.setName(CounterSpecs.ZEROFILTRE_ACCOUNT_CONNECTIONS);
            counterSpecs.setTags("from", loginFrom, SUCCESS, "true");
            metricsProvider.incrementCounter(counterSpecs);
        }

        if (foundUser.isExpired()) {

            counterSpecs.setName(CounterSpecs.ZEROFILTRE_ACCOUNT_CONNECTIONS);
            counterSpecs.setTags("from", loginFrom, SUCCESS, "false");
            metricsProvider.incrementCounter(counterSpecs);

            throw new UnAuthenticatedActionException(
                    String.format("The user %s has been deactivated, not allowing connection until activation", foundUser.getFullName()),
                    Domains.NONE.name());
        }
    }
}
