package example.customer;

import com.zaxxer.hikari.HikariDataSource;
import example.customer.domain.Customer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.JdbcTransactionManager;

import javax.sql.DataSource;

@Configuration
@EnableJdbcRepositories(
        transactionManagerRef = "customerTransactionManager",
        jdbcOperationsRef = "customerJdbcOperations",
        basePackageClasses = Customer.class
)
public class CustomerConfig {


    @Bean
    @ConfigurationProperties("spring.datasource.customers")
    public DataSourceProperties customersDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    public DataSource customersDataSource() {
        return customersDataSourceProperties()
                .initializeDataSourceBuilder()
                .build();
    }


    @Bean(name = "customerTransactionManager")
    JdbcTransactionManager customerTransactionManager(@Qualifier("customersDataSource") DataSource dataSource) {
        return new JdbcTransactionManager(dataSource);
    }

    @Primary
    @Bean(name = "customerJdbcOperations")
    NamedParameterJdbcOperations customerJdbcOperations(@Qualifier("customersDataSource") DataSource dataSource) {
        return new NamedParameterJdbcTemplate(dataSource);
    }


}
