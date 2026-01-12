package com.xinyue.atelier.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import static com.xinyue.atelier.service.LocalFileStorageService.UPLOAD_DIR;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry
                .addResourceHandler("/data/**")
                .addResourceLocations("file:" + UPLOAD_DIR + "/");
    }
}