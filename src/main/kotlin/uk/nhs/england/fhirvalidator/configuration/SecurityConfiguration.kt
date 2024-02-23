package uk.nhs.england.fhirvalidator.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.SecurityFilterChain
import org.springframework.http.HttpHeaders
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import java.util.*


@Configuration
@EnableWebSecurity
open class SecurityConfiguration  {
    @Bean
    public fun configure(http: HttpSecurity): SecurityFilterChain {
        http
            .authorizeHttpRequests { authorize ->
                authorize
                    .anyRequest().permitAll()
            }
            .csrf { csrf ->
                csrf
                    .disable()
            }
            .cors{ cors -> corsConfigurationSource()}
        return http.build()
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()
        configuration.addAllowedMethod("GET")
        configuration.addAllowedMethod("PUT")
        configuration.addAllowedMethod("PUT")
        configuration.addAllowedMethod("POST")
        configuration.addAllowedMethod("OPTIONS")
        configuration.allowedOrigins = Arrays.asList("*")
        //configuration.allowedMethods = Arrays.asList("*")
        configuration.allowedHeaders = Arrays.asList("*")
        configuration.maxAge = 3600
        configuration.exposedHeaders = Arrays.asList(
            HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
            HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS,
            HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS,
            HttpHeaders.ACCESS_CONTROL_MAX_AGE,
            HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS)
        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }
}
