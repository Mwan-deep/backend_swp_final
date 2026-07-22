package AI_Study_Hub.config;

import AI_Study_Hub.repository.InvalidationRespository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.spec.SecretKeySpec;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SecurityConfig {
    static String[] PUBLIC_ENDPOINT = {
            "/api/account",
            "/api/authen",
            "/api/authen/introspec",
            "/api/auth/**",
            "/api/authen/verify-otp",
            "/api/v1/share/download/**",
            "/api/v1/master-data/**",
            "/api/v1/rankings/**"
    };
    static String[] MUST_BE_AUTHENTICATE = {"/api/account/change-password", "/api/authen/logout", "/infor/{id}"};

    @Autowired
    @NonFinal
    CustomAccessDeniedHandler customAccessDeniedHandler;

    @Autowired
    @NonFinal
    GoogleSingInHandler googleSingInHandler;

    @Autowired
    @NonFinal
    InvalidationRespository invalidationRespository;

    @Value("${jwt.signerToken}")
    @NonFinal
    String SIGNER_TOKEN;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(requestMatcherRegistry ->
                        requestMatcherRegistry
                                .requestMatchers(PUBLIC_ENDPOINT).permitAll()

                                .requestMatchers("/api/v1/question-sets/**").authenticated()
                                .requestMatchers("/api/v1/quizzes/").authenticated()
                                .requestMatchers("/api/v1/quizzes/**").authenticated()
                                .requestMatchers("/api/v1/documents/").authenticated()

                                // BỔ SUNG QUAN TRỌNG: Mở khóa Hồ sơ cá nhân bất kể Frontend dùng URL cũ hay mới
                                // Khai báo này nằm TRƯỚC luồng ADMIN để đánh chặn lỗi 403
                                .requestMatchers("/api/my-profile", "/api/my-profile/**").authenticated()
                                .requestMatchers("/api/account/my-profile", "/api/account/my-profile/**").authenticated()

                                .requestMatchers(HttpMethod.POST, "/api/reports").authenticated()
                                .requestMatchers(HttpMethod.GET, "/api/reports/**").hasAnyRole("ADMIN", "MANAGER")
                                .requestMatchers(HttpMethod.PUT, "/api/reports/**").hasAnyRole("ADMIN", "MANAGER")

                                // KHU VỰC CỦA ADMIN: Đã an toàn vì /my-profile đã được bóc tách ở trên
                                .requestMatchers(HttpMethod.PUT, "/api/account/**").hasRole("ADMIN")
                                .requestMatchers(HttpMethod.GET, "/api/account/**").hasRole("ADMIN")
                                .requestMatchers(HttpMethod.DELETE, "/api/account/**").hasRole("ADMIN")
                                .requestMatchers(HttpMethod.POST, "/api/account/createAccountByAdmin").hasRole("ADMIN")

                                .requestMatchers(HttpMethod.POST, MUST_BE_AUTHENTICATE).authenticated()
                                .anyRequest()
                                .authenticated())
                .exceptionHandling(exception ->
                        exception.accessDeniedHandler(customAccessDeniedHandler));

        httpSecurity.oauth2Login(oauth2 -> oauth2.successHandler(googleSingInHandler));

        httpSecurity.oauth2ResourceServer(oAuth2 ->
                oAuth2.jwt(jwtConfigurer -> jwtConfigurer.decoder(jwtDecoder())
                        .jwtAuthenticationConverter(jwtAuthenticationConverter())));

        return httpSecurity.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter(){
        JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        jwtGrantedAuthoritiesConverter.setAuthorityPrefix("");

        JwtAuthenticationConverter authenticationConverter = new JwtAuthenticationConverter();
        authenticationConverter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter);

        return authenticationConverter;
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        SecretKeySpec secretKeySpec = new SecretKeySpec(SIGNER_TOKEN.getBytes(), "HS256");
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withSecretKey(secretKeySpec).build();

        OAuth2TokenValidator<Jwt> defaultValidator = JwtValidators.createDefaultWithIssuer("devteria");

        OAuth2TokenValidator<Jwt> checkLogoutValidator = jwt -> {
            String jit = jwt.getId();
            if (invalidationRespository.existsById(jit)) {
                return OAuth2TokenValidatorResult.failure(
                        new OAuth2Error("invalid_token", "Token này đã bị Đăng xuất!", null)
                );
            }
            return OAuth2TokenValidatorResult.success();
        };

        jwtDecoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(defaultValidator, checkLogoutValidator));

        return jwtDecoder;
    }
}