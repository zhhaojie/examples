package demo.customer.domain;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.jdbc.Sql;

import static org.assertj.core.api.Assertions.assertThat;

@DataJdbcTest // Slice test does not use CustomerConfig
class CustomerTest {

	@Autowired
	private CustomerRepository customerRepository;

	@Test
	void save() {
		Customer customer = new Customer("John", "Smith");
		assertThat(customer.getId()).isNull();
		this.customerRepository.save(customer);
		assertThat(customer.getId()).isNotNull();
	}

}
