package demo.customer;

import com.zaxxer.hikari.HikariDataSource;
import demo.customer.domain.Customer;
import org.springframework.beans.factory.annotation.Qualifier;
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
    @ConfigurationProperties(prefix = "customers.spring.datasource.hikari")
    public HikariDataSource customerDataSource() {
        return DataSourceBuilder.create().type(HikariDataSource.class).build();
    }

    @Bean(name = "customerTransactionManager")
    JdbcTransactionManager customerTransactionManager(@Qualifier("customerDataSource") DataSource dataSource) {
        return new JdbcTransactionManager(dataSource);
    }

    @Primary
    @Bean(name = "customerJdbcOperations")
    NamedParameterJdbcOperations customerJdbcOperations(@Qualifier("customerDataSource") DataSource dataSource) {
        return new NamedParameterJdbcTemplate(dataSource);
    }


//    @Bean
//    DataSourceInitializer customerDataSourceInitializer(@Qualifier("customerDataSource") DataSource dataSource) {
//        ResourceDatabasePopulator resourceDatabasePopulator = new ResourceDatabasePopulator();
//        resourceDatabasePopulator.addScript(new ClassPathResource("customer.schema-h2.sql"));
//
//        DataSourceInitializer dataSourceInitializer = new DataSourceInitializer();
//        dataSourceInitializer.setDataSource(dataSource);
//        dataSourceInitializer.setDatabasePopulator(resourceDatabasePopulator);
//        dataSourceInitializer.afterPropertiesSet();
//        return dataSourceInitializer;
//    }

}
