package io.ussop.ExchangeRateProject.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@Data
@PropertySource("application.properties")
public class BotConfig {
    @Value("E1d0s_bot")
    String botName;

    @Value("6675086619:AAG9MIu4Fq8VynRAfWEjxxKf1kkfwX5D5Mw")
    String token;

}
