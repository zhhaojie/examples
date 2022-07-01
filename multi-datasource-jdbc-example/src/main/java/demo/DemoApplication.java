package demo;

import demo.customer.domain.CustomerRepository;
import demo.order.domain.OrderRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.Resource;

@SpringBootApplication
public class DemoApplication implements CommandLineRunner {

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		orderRepository.findAll();
		customerRepository.findAll();
	}

	@Resource
	OrderRepository orderRepository;

	@Resource
	CustomerRepository customerRepository;
}
