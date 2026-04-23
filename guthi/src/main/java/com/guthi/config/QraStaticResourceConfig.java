package com.guthi.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Serves preset donation QR images from {@code ./qra} (relative to the process working directory,
 * typically the {@code guthi} module root), matching {@code D:\\Mukund\\guthi\\qra\\1.png}, etc.
 */
@Configuration
public class QraStaticResourceConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/qra/**")
                .addResourceLocations("file:./qra/", "classpath:/static/qra/");
    }
}
