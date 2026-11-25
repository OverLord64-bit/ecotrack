package com.ecotrack.report.soap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.webservices.client.WebServiceTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.ws.client.core.WebServiceTemplate;

import java.time.Duration;

@Configuration
public class MunicipalClientConfig {

    @Bean
    Jaxb2Marshaller municipalMarshaller() {
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setContextPath("com.ecotrack.contracts.municipal"); // package generated from the XSD
        return marshaller;
    }

    /** Explicit connect and read timeouts: a hung municipal system can never block a thread forever. */
    @Bean
    WebServiceTemplate municipalTemplate(WebServiceTemplateBuilder builder, Jaxb2Marshaller marshaller,
                                         @Value("${ecotrack.municipal.soap-url}") String url,
                                         @Value("${ecotrack.municipal.connect-timeout-ms}") long connectTimeoutMs,
                                         @Value("${ecotrack.municipal.read-timeout-ms}") long readTimeoutMs) {
        return builder
                .setMarshaller(marshaller)
                .setUnmarshaller(marshaller)
                .setDefaultUri(url)
                .setConnectTimeout(Duration.ofMillis(connectTimeoutMs))
                .setReadTimeout(Duration.ofMillis(readTimeoutMs))
                .interceptors(new SoapLoggingInterceptor())
                .build();
    }
}
