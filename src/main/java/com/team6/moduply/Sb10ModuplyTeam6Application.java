package com.team6.moduply;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class Sb10ModuplyTeam6Application {

  public static void main(String[] args) {
    SpringApplication.run(Sb10ModuplyTeam6Application.class, args);
  }

}
