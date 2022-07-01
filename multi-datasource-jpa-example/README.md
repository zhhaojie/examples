# demo-multi-datasource-jpa

演示如何在 spring data jpa 项目中连接多个数据库进行开发。

## 配置文件
```
customers.spring.datasource.hikari.jdbc-url=jdbc:h2:mem:customers;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
customers.spring.datasource.hikari.username=sa
customers.spring.datasource.hikari.password=
customers.spring.datasource.hikari.minimum-idle=2
customers.spring.datasource.hikari.maximum-pool-size=50
customers.spring.jpa.show-sql=true
customers.spring.jpa.generate-ddl=true
customers.spring.jpa.hibernate.ddl-auto=create-drop

orders.spring.datasource.hikari.jdbc-url=jdbc:h2:mem:orders;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
orders.spring.datasource.hikari.username=sa
orders.spring.datasource.hikari.password=
orders.spring.datasource.hikari.minimum-idle=2
orders.spring.datasource.hikari.maximum-pool-size=50
orders.spring.jpa.show-sql=true
orders.spring.jpa.generate-ddl=true
orders.spring.jpa.hibernate.ddl-auto=create-drop


logging.level.com.zaxxer.hikari.HikariConfig=debug
```

## CustomerConfig
```
@Configuration
@EnableJpaRepositories(entityManagerFactoryRef = "customerEntityManager",
		transactionManagerRef = "customerTransactionManager", basePackageClasses = Customer.class)
public class CustomerConfig {

	private final PersistenceUnitManager persistenceUnitManager;

	public CustomerConfig(ObjectProvider<PersistenceUnitManager> persistenceUnitManager) {
		this.persistenceUnitManager = persistenceUnitManager.getIfAvailable();
	}

	@Bean
	@ConfigurationProperties("customers.spring.jpa")
	public JpaProperties customerJpaProperties() {
		return new JpaProperties();
	}

	@Bean
	@ConfigurationProperties(prefix = "customers.spring.datasource.hikari")
	public HikariDataSource customerDataSource() {
		return DataSourceBuilder.create().type(HikariDataSource.class).build();
	}

	@Bean
	public LocalContainerEntityManagerFactoryBean customerEntityManager(JpaProperties customerJpaProperties) {
		EntityManagerFactoryBuilder builder = createEntityManagerFactoryBuilder(customerJpaProperties);
		return builder.dataSource(customerDataSource()).packages(Customer.class).persistenceUnit("customersDs").build();
	}

	@Bean
	@Primary
	public JpaTransactionManager customerTransactionManager(EntityManagerFactory customerEntityManager) {
		return new JpaTransactionManager(customerEntityManager);
	}

	private EntityManagerFactoryBuilder createEntityManagerFactoryBuilder(JpaProperties customerJpaProperties) {
		JpaVendorAdapter jpaVendorAdapter = createJpaVendorAdapter(customerJpaProperties);
		return new EntityManagerFactoryBuilder(jpaVendorAdapter, customerJpaProperties.getProperties(),
				this.persistenceUnitManager);
	}

	private JpaVendorAdapter createJpaVendorAdapter(JpaProperties jpaProperties) {
		AbstractJpaVendorAdapter adapter = new HibernateJpaVendorAdapter();
		adapter.setShowSql(jpaProperties.isShowSql());
		if (jpaProperties.getDatabase() != null) {
			adapter.setDatabase(jpaProperties.getDatabase());
		}
		if (jpaProperties.getDatabasePlatform() != null) {
			adapter.setDatabasePlatform(jpaProperties.getDatabasePlatform());
		}
		adapter.setGenerateDdl(jpaProperties.isGenerateDdl());
		return adapter;
	}

}
```

## OrderConfig
```
@Configuration
@EnableJpaRepositories(entityManagerFactoryRef = "orderEntityManager",
		transactionManagerRef = "orderTransactionManager", basePackageClasses = Order.class)
public class OrderConfig {

	private final PersistenceUnitManager persistenceUnitManager;

	public OrderConfig(ObjectProvider<PersistenceUnitManager> persistenceUnitManager) {
		this.persistenceUnitManager = persistenceUnitManager.getIfAvailable();
	}

	@Bean
	@ConfigurationProperties("orders.spring.jpa")
	public JpaProperties orderJpaProperties() {
		return new JpaProperties();
	}

	@Bean
	@ConfigurationProperties(prefix = "orders.spring.datasource.hikari")
	public HikariDataSource orderDataSource() {
		return DataSourceBuilder.create().type(HikariDataSource.class).build();
	}

	@Bean
	public LocalContainerEntityManagerFactoryBean orderEntityManager(JpaProperties orderJpaProperties) {
		EntityManagerFactoryBuilder builder = createEntityManagerFactoryBuilder(orderJpaProperties);
		return builder.dataSource(orderDataSource()).packages(Order.class).persistenceUnit("ordersDs").build();
	}

	@Bean
	public JpaTransactionManager orderTransactionManager(EntityManagerFactory orderEntityManager) {
		return new JpaTransactionManager(orderEntityManager);
	}

	private EntityManagerFactoryBuilder createEntityManagerFactoryBuilder(JpaProperties orderJpaProperties) {
		JpaVendorAdapter jpaVendorAdapter = createJpaVendorAdapter(orderJpaProperties);
		return new EntityManagerFactoryBuilder(jpaVendorAdapter, orderJpaProperties.getProperties(),
				this.persistenceUnitManager);
	}

	private JpaVendorAdapter createJpaVendorAdapter(JpaProperties jpaProperties) {
		AbstractJpaVendorAdapter adapter = new HibernateJpaVendorAdapter();
		adapter.setShowSql(jpaProperties.isShowSql());
		if (jpaProperties.getDatabase() != null) {
			adapter.setDatabase(jpaProperties.getDatabase());
		}
		if (jpaProperties.getDatabasePlatform() != null) {
			adapter.setDatabasePlatform(jpaProperties.getDatabasePlatform());
		}
		adapter.setGenerateDdl(jpaProperties.isGenerateDdl());
		return adapter;
	}

}
```

## 项目结构相互隔离，清晰明了

```
├── pom.xml
├── src
│   ├── main
│   │   ├── java
│   │   │   └── demo
│   │   │       ├── DemoApplication.java
│   │   │       ├── customer // 客户数据库
│   │   │       │   ├── CustomerConfig.java
│   │   │       │   └── domain
│   │   │       └── order // 订单数据库
│   │   │           ├── OrderConfig.java
│   │   │           └── domain
│   │   └── resources
│   │       └── application.properties


```

## 其它知识点

- spring.datasource 用于配置 Datasource 属性
- spring.jpa  用于配置 JPA 属性
- 借助于 spring boot 自动配置 @EnableAutoConfiguration 功能，它可以很好地处理 OrderConfig 与 CustomerConfig 的EnableJpaRepositories。
- 如果想使用spring-data-jdbc 实现类似的功能，可以参看这个案例：[demo-multi-datasource-jdbc](https://github.com/zhhaojie/demo-multi-datasource-jdbc)

