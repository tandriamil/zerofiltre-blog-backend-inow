package tech.zerofiltre.blog.infra.entrypoints.rest.user;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
import org.springframework.boot.test.mock.mockito.*;
import org.springframework.context.*;
import org.springframework.security.crypto.password.*;
import org.springframework.test.context.junit.jupiter.*;
import tech.zerofiltre.blog.domain.*;
import tech.zerofiltre.blog.domain.user.*;
import tech.zerofiltre.blog.domain.user.model.*;
import tech.zerofiltre.blog.domain.user.use_cases.*;
import tech.zerofiltre.blog.infra.entrypoints.rest.*;
import tech.zerofiltre.blog.infra.entrypoints.rest.user.model.*;

import javax.servlet.http.*;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class UserControllerTest {

    public static final String TOKEN = "token";
    public static final String EMAIL = "email";
    @MockBean
    MessageSource sources;

    @MockBean
    PasswordEncoder passwordEncoder;

    @MockBean
    UserProvider userProvider;

    @MockBean
    PasswordVerifierProvider passwordVerifierProvider;

    @MockBean
    UserNotificationProvider userNotificationProvider;

    @MockBean
    VerificationTokenProvider verificationTokenProvider;

    @MockBean
    HttpServletRequest request;

    UserController userController;

    RegisterUserVM userVM = new RegisterUserVM();
    UpdatePasswordVM updatePasswordVM = new UpdatePasswordVM();


    @MockBean
    private SecurityContextManager securityContextManager;


    @BeforeEach
    void setUp() {
        userController = new UserController(
                userProvider, userNotificationProvider, verificationTokenProvider, sources, passwordEncoder, securityContextManager, passwordVerifierProvider);
    }

    @Test
    void registerUserAccount_MustRegisterUser_ThenNotifyRegistrationComplete() throws UserAlreadyExistException {

        //ACT
        userController.registerUserAccount(userVM, request);

        //ASSERT
        verify(userProvider, times(1)).userOfEmail(any());
        verify(userProvider, times(1)).save(any());
        verify(userNotificationProvider, times(1)).notify(any());
    }

    @Test
    void resendRegistrationConfirm_mustNotify() {
        //ARRANGE
        when(userProvider.userOfEmail(any())).thenReturn(Optional.of(new User()));

        //ACT
        userController.resendRegistrationConfirm("email", request);

        //ASSERT
        verify(userProvider, times(1)).userOfEmail(any());
        verify(userNotificationProvider, times(1)).notify(any());

    }

    @Test
    void confirmRegistration_mustCheckToken_ThenSaveUser() throws InvalidTokenException {
        //ARRANGE
        when(verificationTokenProvider.ofToken(any())).thenReturn(Optional.of(new VerificationToken(new User(), "")));

        //ACT
        userController.registrationConfirm("token", request);

        //ASSERT
        verify(verificationTokenProvider, times(1)).ofToken(any());
        verify(userProvider, times(1)).save(any());

    }

    @Test
    void resetPassword_mustCheckUser_ThenNotify() {
        //ARRANGE
        when(userProvider.userOfEmail(any())).thenReturn(Optional.of(new User()));

        //ACT
        userController.initPasswordReset("email", request);

        //ASSERT
        verify(userProvider, times(1)).userOfEmail(EMAIL);
        verify(userNotificationProvider, times(1)).notify(any());

    }

    @Test
    void verifyToken_mustCheckToken() throws InvalidTokenException {
        //ARRANGE
        when(verificationTokenProvider.ofToken(any())).thenReturn(Optional.of(new VerificationToken(new User(), "")));


        //ACT
        userController.verifyTokenForPasswordReset(TOKEN);

        //ASSERT
        verify(verificationTokenProvider, times(1)).ofToken(any());

    }

    @Test
    void updatePassword_mustCheckUserAndPassword_thenSave() throws BlogException {
        //ARRANGE
        User user = new User();
        user.setEmail(EMAIL);
        when(securityContextManager.getAuthenticatedUser()).thenReturn(user);
        when(userProvider.userOfEmail(any())).thenReturn(Optional.of(user));
        when(passwordVerifierProvider.isValid(any(), any())).thenReturn(true);
        when(userProvider.save(any())).thenReturn(user);

        //ACT
        userController.updatePassword(updatePasswordVM, request);

        //ASSERT
        verify(userProvider, times(1)).userOfEmail(EMAIL);
        verify(passwordVerifierProvider, times(1)).isValid(user, updatePasswordVM.getOldPassword());
        verify(userProvider, times(1)).save(any());

    }
}