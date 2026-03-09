package com.al3000.cloudbase.config;

import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

@Configuration
public class SpaWebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new SpaPageResourceResolver());
    }

    static class SpaPageResourceResolver extends PathResourceResolver {

        private final Resource index = new ClassPathResource("/static/index.html");

        @Override
        protected Resource getResource(@NotNull String resourcePath, Resource location) throws IOException {
            Resource requested = location.createRelative(resourcePath);

            // 1. Если файл реально существует — отдать его
            if (requested.exists() && requested.isReadable()) {
                return requested;
            }

            // 2. API никогда не форвардим в SPA
            if (resourcePath.startsWith("api/")) {
                return null;
            }

            // 3. assets — это только реальные статические файлы
            if (resourcePath.startsWith("assets/")) {
                return null;
            }

            // 4. Всё остальное считаем отсутствующим ресурсом
            return index;
        }
    }
}