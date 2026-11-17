package vn.edu.sieuthi.sieuthi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.config.http.SessionCreationPolicy;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        CookieCsrfTokenRepository csrfRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfRepository.setCookiePath("/");
        http
            .csrf(csrf -> csrf
                .csrfTokenRepository(csrfRepository)
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
            )
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/login.html", "/css/**", "/js/**", "/favicon.ico", "/api/auth/csrf").permitAll()
                .requestMatchers("/", "/index.html").authenticated()
                .requestMatchers("/api/auth/accounts").hasRole("MANAGER")
                .requestMatchers(
                    "/api/tong-quan",
                    "/api/bao-cao/**",
                    "/api/mat-hang",
                    "/api/mat-hang/**",
                    "/api/cai-dat/vip"
                ).hasRole("MANAGER")
                .requestMatchers("/api/**").authenticated()
                .anyRequest().permitAll()
            )
            .formLogin(form -> form
                .loginPage("/login.html")
                .loginProcessingUrl("/login")
                .successHandler(phanHoiDangNhapThanhCong())
                .failureHandler(phanHoiDangNhapThatBai())
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessHandler((request, response, authentication) -> response.sendRedirect("/login.html?logout"))
            )
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(phanHoiChuaDangNhap())
                .accessDeniedHandler(phanHoiTuChoi())
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED));
        return http.build();
    }

    private static AuthenticationSuccessHandler phanHoiDangNhapThanhCong() {
        return (request, response, authentication) -> response.setStatus(HttpServletResponse.SC_OK);
    }

    private static AuthenticationFailureHandler phanHoiDangNhapThatBai() {
        return (request, response, exception) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"message\":\"Tên đăng nhập hoặc mật khẩu không đúng.\"}");
        };
    }

    private static AuthenticationEntryPoint phanHoiChuaDangNhap() {
        return (request, response, exception) -> {
            if (request.getRequestURI().startsWith("/api/")) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write("{\"message\":\"Vui lòng đăng nhập để tiếp tục.\"}");
            } else {
                response.sendRedirect("/login.html");
            }
        };
    }

    private static AccessDeniedHandler phanHoiTuChoi() {
        return (request, response, exception) -> {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"message\":\"Tài khoản không có quyền thực hiện thao tác này.\"}");
        };
    }
}
