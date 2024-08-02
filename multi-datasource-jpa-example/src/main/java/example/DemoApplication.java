package example;

import example.customer.domain.CustomerRepository;
import example.order.domain.OrderRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;

import javax.annotation.Resource;

@SpringBootApplication
public class DemoApplication implements CommandLineRunner {
    @Override
    public void run(String... args) throws Exception {
        customerRepository.count();
        orderRepository.count();
    }

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }


    @Resource
    CustomerRepository customerRepository;

    @Resource
    OrderRepository orderRepository;
}
