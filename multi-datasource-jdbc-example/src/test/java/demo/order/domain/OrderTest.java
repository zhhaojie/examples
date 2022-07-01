package demo.order.domain;

import java.util.Date;

import demo.customer.domain.CustomerRepository;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJdbcTest // Slice test does not use OrderConfig
class OrderTest {

	@Autowired
	private OrderRepository orderRepository;

	@Test
	void save() {
		Order order = new Order(123L, new Date());
		assertThat(order.getId()).isNull();
		this.orderRepository.save(order);
		assertThat(order.getId()).isNotNull();
	}

}
