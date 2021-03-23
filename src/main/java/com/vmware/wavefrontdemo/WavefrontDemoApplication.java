package com.vmware.wavefrontdemo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.annotation.Id;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import io.r2dbc.spi.ConnectionFactory;
import reactor.core.publisher.Flux;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

import org.springframework.beans.factory.annotation.Autowired;

@SpringBootApplication
public class WavefrontDemoApplication {

  public static void main(String[] args) {
    SpringApplication.run(WavefrontDemoApplication.class, args);
  }

  @Bean
  RouterFunction<ServerResponse> routes(ReservationRepository rr) {
      return route()
              .GET("/reservations", r -> ok().body(rr.findAll(), Reservation.class))
              .GET("/hello", r -> ok().bodyValue("Hi, Spring fans!"))
              .build();

  }

  @Configuration(proxyBeanMethods = false)
  static class DatabaseInitializationConfiguration {
    @Autowired
    void initializeDatabase(ConnectionFactory connectionFactory) {
        ResourceLoader resourceLoader = new DefaultResourceLoader();
        Resource[] scripts = new Resource[] { resourceLoader.getResource("classpath:schema.sql")};
        new ResourceDatabasePopulator(scripts).populate(connectionFactory).block();
    }
  }

  @Bean
  ApplicationRunner runner(ReservationRepository reservationRepository) {
      return args -> {

          var data = Flux
                  .just("A", "B", "C", "D")
                  .map(name -> new Reservation(null, name))
                  .flatMap(reservationRepository::save);

          reservationRepository
                  .deleteAll()
                  .thenMany(data)
                  .thenMany(reservationRepository.findAll())
                  .subscribe(System.out::println);
      };
  }
}

interface ReservationRepository extends ReactiveCrudRepository<Reservation, String> {
}

@Data
@AllArgsConstructor
@NoArgsConstructor
class Reservation {

  @Id
  private String id;
  private String name;
}
