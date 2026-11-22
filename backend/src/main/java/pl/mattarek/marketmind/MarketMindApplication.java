package pl.mattarek.marketmind;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class MarketMindApplication {

    public static void main(final String[] args) {
        SpringApplication.run(MarketMindApplication.class, args);
    }
}
