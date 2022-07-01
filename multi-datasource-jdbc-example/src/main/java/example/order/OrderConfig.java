package demo.order;

import com.zaxxer.hikari.HikariDataSource;
import demo.order.domain.Order;
import org.springframework.beans.factory.annotation.Qualifier;
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
    @ConfigurationProperties(prefix = "orders.spring.datasource.hikari")
    HikariDataSource orderDataSource() {
        return DataSourceBuilder.create().type(HikariDataSource.class).build();
    }

    @Bean(name = "orderTransactionManager")
    JdbcTransactionManager orderTransactionManager(@Qualifier("orderDataSource") DataSource dataSource) {
        return new JdbcTransactionManager(dataSource);
    }

    @Bean(name = "orderJdbcOperations")
    NamedParameterJdbcOperations orderJdbcOperations(@Qualifier("orderDataSource") DataSource dataSource) {
        return new NamedParameterJdbcTemplate(dataSource);
    }

//    @Bean
//    DataSourceInitializer orderDataSourceInitializer(@Qualifier("orderDataSource") DataSource dataSource) {
//        ResourceDatabasePopulator resourceDatabasePopulator = new ResourceDatabasePopulator();
//        resourceDatabasePopulator.addScript(new ClassPathResource("order.schema-h2.sql"));
//
//        DataSourceInitializer dataSourceInitializer = new DataSourceInitializer();
//        dataSourceInitializer.setDataSource(dataSource);
//        dataSourceInitializer.setDatabasePopulator(resourceDatabasePopulator);
//        dataSourceInitializer.afterPropertiesSet();
//        return dataSourceInitializer;
//    }

}
