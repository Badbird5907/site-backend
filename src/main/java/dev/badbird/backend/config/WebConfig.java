package dev.badbird.backend.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@ComponentScan
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        if (!registry.hasMappingForPattern("/static/**")) {
            registry.addResourceHandler("/static/**")
                    .addResourceLocations("/static/");
        }
        /* // Deployed on cloudflare pages
        if (!registry.hasMappingForPattern("/react/**")) {
            registry.addResourceHandler("/react/**")
                    .addResourceLocations("/react/");
        }
         */
    }
}
