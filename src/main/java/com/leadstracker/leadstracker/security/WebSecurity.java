package com.leadstracker.leadstracker.security;

import com.leadstracker.leadstracker.DTO.Utils;
import com.leadstracker.leadstracker.repositories.UserRepository;
import com.leadstracker.leadstracker.services.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class WebSecurity {

    private final UserService userService;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final UserRepository userRepository;

    public WebSecurity(UserService userService, BCryptPasswordEncoder bCryptPasswordEncoder, UserRepository userRepository) {
        this.userService = userService;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
        this.userRepository = userRepository;
    }


    @Bean
    SecurityFilterChain configure(HttpSecurity http) throws Exception {

        AuthenticationManagerBuilder authenticationManagerBuilder = http.getSharedObject(AuthenticationManagerBuilder.class);
        authenticationManagerBuilder.userDetailsService(userService).passwordEncoder(bCryptPasswordEncoder);

        AuthenticationManager authenticationManager = authenticationManagerBuilder.build();

        //Customize Login URL path
        AuthenticationFilter authenticationFilter = new AuthenticationFilter(authenticationManager, userRepository);
        authenticationFilter.setFilterProcessesUrl("/leads/login");

        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests((authz) -> authz

                        .requestMatchers(HttpMethod.POST, SecurityConstants.Create_User).hasAnyAuthority("ROLE_ADMIN")
                        .requestMatchers(HttpMethod.POST, SecurityConstants.Login).permitAll()
                        .requestMatchers(HttpMethod.POST, SecurityConstants.Forgot_Password_Request).permitAll()
                        .requestMatchers(HttpMethod.POST, SecurityConstants.Reset_Password).permitAll()
                        .requestMatchers(HttpMethod.GET, SecurityConstants.view_Users,
                                SecurityConstants.All_Team_Members).hasAnyAuthority("ROLE_ADMIN")
                        .requestMatchers(HttpMethod.GET, SecurityConstants.View_Team_Lead,
                                SecurityConstants.Members_Under_Lead).hasAnyAuthority("ROLE_ADMIN", "ROLE_TEAM_LEAD")
                        .requestMatchers(HttpMethod.POST, SecurityConstants.Verify_Email).permitAll()
                        .requestMatchers(HttpMethod.POST, SecurityConstants.Verify_OTP).permitAll()
                        .requestMatchers(HttpMethod.POST, SecurityConstants.Resend_OTP).permitAll()
                        .requestMatchers(HttpMethod.DELETE, SecurityConstants.Delete_User).hasAnyAuthority("ROLE_ADMIN")
                        .requestMatchers(HttpMethod.PUT, SecurityConstants.Edit_Users).hasAnyAuthority("ROLE_ADMIN")
                        .requestMatchers(HttpMethod.GET, SecurityConstants.Member_Under_Lead).permitAll()

                        .anyRequest().authenticated())

                .authenticationManager(authenticationManager)
                .addFilter(authenticationFilter)
                .addFilter(new AuthorizationFilter(authenticationManager, userRepository))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return http.build();

    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        final CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("*"));
        configuration.setAllowedMethods(List.of("*"));
        configuration.setAllowedHeaders(List.of("*"));


        final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
     }

    }
