package com.github.xingshuangs.rtsp.starter.config;


import com.github.xingshuangs.rtsp.starter.properties.RtspProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author xingshuang
 */
@EnableConfigurationProperties
@Configuration
public class PropertiesConfig {

    @Bean
    public RtspProperties rtspProperties(){
        return new RtspProperties();
    }
}
