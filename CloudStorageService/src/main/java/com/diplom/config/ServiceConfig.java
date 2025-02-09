package com.diplom.config;

import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Configuration
@EnableWebMvc
@EnableTransactionManagement
public class ServiceConfig implements WebMvcConfigurer {

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();
        // Настройка маппинга для сущностей (опционально)
        modelMapper.getConfiguration().setSkipNullEnabled(true); // Игнорировать null поля
        return modelMapper;
    }

    public static <R, E> List<R> convertList(List<E> list, Function<E, R> converter) {
        return list.stream().map(e -> converter.apply(e)).collect(Collectors.toList());
    }
    public static <D, T> List<D> convertList(List<T> entityList, Class<D> outClass, ModelMapper mapper) {
        return entityList.stream()
                .map(entity -> mapper.map(entity, outClass))
                .collect(Collectors.toList());
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowCredentials(true)
                .allowedOrigins("http://localhost:8081")
                .allowedMethods("*");
    }

    @Bean
    public Logger logger() {
        return LoggerFactory.getLogger("Logger");
    }
}