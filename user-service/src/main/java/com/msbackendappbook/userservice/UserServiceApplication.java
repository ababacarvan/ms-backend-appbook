package com.msbackendappbook.userservice;

import com.msbackendappbook.userservice.model.entity.User;
import com.msbackendappbook.userservice.model.enums.Role;
import com.msbackendappbook.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

@SpringBootApplication
@EnableFeignClients
@EnableScheduling
@EnableAsync
@RequiredArgsConstructor
public class UserServiceApplication implements CommandLineRunner {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    @Value("${spring.kafka.bootstrap-servers}")
    private String kafkaBootstrapServers;
    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
    @Override
    public void run(String... args) {
        System.out.println("=========================================");
        System.out.println("📌 KAFKA BOOTSTRAP SERVERS: " + kafkaBootstrapServers);
        System.out.println("=========================================");
    }
    @Bean
    public CommandLineRunner commandLineRunner(){
        return args -> {
            User user = User.builder()
                    .email("ababacarvan@gmail.com")
                    .firstName("Ababacar")
                    .lastName("GADIAGA")
                    .phoneNumber("+211771828282")
                    .passwordHash(passwordEncoder.encode("saliou182"))
                    .createdAt(LocalDateTime.now())
                    .isActive(true)
                    .emailVerified(true)
                    .role(Role.ADMIN)
                    .build();
            //userRepository.save(user);
            System.out.println("Admin creer ");
        };
    }
}
