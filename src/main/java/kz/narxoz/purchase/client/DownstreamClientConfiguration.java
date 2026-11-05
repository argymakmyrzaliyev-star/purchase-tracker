package kz.narxoz.purchase.client;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(DownstreamProperties.class)
class DownstreamClientConfiguration {
    @Bean
    RestClient downstreamClient(RestClient.Builder builder, DownstreamProperties properties) {
        var requests = new SimpleClientHttpRequestFactory();
        requests.setConnectTimeout(properties.timeout());
        requests.setReadTimeout(properties.timeout());
        return builder.baseUrl(properties.baseUrl().toString()).requestFactory(requests).build();
    }
}
