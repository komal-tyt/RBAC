package com.taxi.user;

import com.taxi.user.model.Tariff;
import com.taxi.user.repository.TariffRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@Slf4j
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }

    @Bean
    public CommandLineRunner initTariffs(TariffRepository tariffRepository) {
        return args -> {
            if (tariffRepository.count() == 0) {
                // STANDARD tariff
                Tariff standard = new Tariff();
                standard.setName("STANDARD");
                standard.setBasePrice(49.0);
                standard.setPricePerKm(20.0);
                standard.setPricePerMinute(5.0);
                standard.setActive(true);
                tariffRepository.save(standard);
                log.info("Created STANDARD tariff");

                // COMFORT tariff
                Tariff comfort = new Tariff();
                comfort.setName("COMFORT");
                comfort.setBasePrice(99.0);
                comfort.setPricePerKm(35.0);
                comfort.setPricePerMinute(8.0);
                comfort.setActive(true);
                tariffRepository.save(comfort);
                log.info("Created COMFORT tariff");

                // BUSINESS tariff
                Tariff business = new Tariff();
                business.setName("BUSINESS");
                business.setBasePrice(199.0);
                business.setPricePerKm(50.0);
                business.setPricePerMinute(12.0);
                business.setActive(true);
                tariffRepository.save(business);
                log.info("Created BUSINESS tariff");

                log.info("All default tariffs initialized successfully");
            }
        };
    }
}