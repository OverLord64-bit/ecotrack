package com.ecotrack.municipal.ws;

import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.ws.config.annotation.EnableWs;
import org.springframework.ws.config.annotation.WsConfigurer;
import org.springframework.ws.server.EndpointInterceptor;
import org.springframework.ws.server.endpoint.adapter.method.MethodArgumentResolver;
import org.springframework.ws.server.endpoint.adapter.method.MethodReturnValueHandler;
import org.springframework.ws.soap.server.endpoint.interceptor.PayloadValidatingInterceptor;
import org.springframework.ws.transport.http.MessageDispatcherServlet;
import org.springframework.ws.wsdl.wsdl11.DefaultWsdl11Definition;
import org.springframework.xml.xsd.SimpleXsdSchema;
import org.springframework.xml.xsd.XsdSchema;

import java.util.List;

/** SOAP server configuration. The XSD (from the contracts module) is the single source of truth. */
@EnableWs
@Configuration
public class WsConfig implements WsConfigurer {

    public static final String NAMESPACE = "http://ecotrack.example/municipal";

    @Bean
    ServletRegistrationBean<MessageDispatcherServlet> messageDispatcherServlet(ApplicationContext ctx) {
        MessageDispatcherServlet servlet = new MessageDispatcherServlet();
        servlet.setApplicationContext(ctx);
        servlet.setTransformWsdlLocations(true);
        return new ServletRegistrationBean<>(servlet, "/ws/*");
    }

    /** The WSDL is GENERATED from the XSD and served at /ws/municipal.wsdl (bean name = file name). */
    @Bean(name = "municipal")
    DefaultWsdl11Definition municipalWsdl(XsdSchema municipalSchema) {
        DefaultWsdl11Definition definition = new DefaultWsdl11Definition();
        definition.setPortTypeName("MunicipalPort");
        definition.setLocationUri("/ws");
        definition.setTargetNamespace(NAMESPACE);
        definition.setSchema(municipalSchema);
        return definition;
    }

    @Bean
    XsdSchema municipalSchema() {
        return new SimpleXsdSchema(new ClassPathResource("municipal.xsd"));
    }

    @Override
    public void addArgumentResolvers(List<MethodArgumentResolver> argumentResolvers) {
        // none needed
    }

    @Override
    public void addReturnValueHandlers(List<MethodReturnValueHandler> returnValueHandlers) {
        // none needed
    }

    /** Rejects any request that violates the XSD before endpoint code runs; the client gets a SOAP Fault. */
    @Override
    public void addInterceptors(List<EndpointInterceptor> interceptors) {
        PayloadValidatingInterceptor validator = new PayloadValidatingInterceptor();
        validator.setXsdSchema(municipalSchema());
        validator.setValidateRequest(true);
        validator.setValidateResponse(false);
        validator.setAddValidationErrorDetail(true);
        interceptors.add(validator);
    }
}
