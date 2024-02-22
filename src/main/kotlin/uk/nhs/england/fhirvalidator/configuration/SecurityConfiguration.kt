package uk.nhs.england.fhirvalidator.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity

import org.springframework.security.web.SecurityFilterChain


@Configuration
@EnableWebSecurity
open class SecurityConfiguration  {
    @Bean
    public fun configure(http: HttpSecurity): SecurityFilterChain {
        http.authorizeRequests().anyRequest().permitAll().and().csrf().disable()
        return http.build()
    }
}
