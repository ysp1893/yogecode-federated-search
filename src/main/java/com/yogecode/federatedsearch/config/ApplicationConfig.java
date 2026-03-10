package com.yogecode.federatedsearch.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({SearchProperties.class, CacheProperties.class})
public class ApplicationConfig {
}
