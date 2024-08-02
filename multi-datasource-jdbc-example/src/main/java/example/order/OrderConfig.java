package example.order;

import com.zaxxer.hikari.HikariDataSource;
import example.order.domain.Order;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.JdbcTransactionManager;

import javax.sql.DataSource;

@Configuration
@EnableJdbcRepositories(
        transactionManagerRef = "orderTransactionManager",
        jdbcOperationsRef = "orderJdbcOperations",
        basePackageClasses = Order.class
)
public class OrderConfig {


    @Bean
    @ConfigurationProperties("spring.datasource.orders")
    public DataSourceProperties ordersDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    public DataSource ordersDataSource() {
        return ordersDataSourceProperties()
                .initializeDataSourceBuilder()
                .build();
    }


    @Bean(name = "orderTransactionManager")
    JdbcTransactionManager orderTransactionManager(@Qualifier("ordersDataSource") DataSource dataSource) {
        return new JdbcTransactionManager(dataSource);
    }

    @Bean(name = "orderJdbcOperations")
    NamedParameterJdbcOperations orderJdbcOperations(@Qualifier("ordersDataSource") DataSource dataSource) {
        return new NamedParameterJdbcTemplate(dataSource);
    }

}
