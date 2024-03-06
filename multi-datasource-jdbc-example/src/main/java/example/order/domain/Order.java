package example.order.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.io.Serializable;
import java.util.Date;


@Table("CUSTOMER_ORDER")
public class Order implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column("ID")
    private Long id;

    @Column("CUSTOMER_ID")
    private Long customerId;

    @Column("ORDER_DATE")
    private Date orderDate;

    public Order() {
    }

    public Order(Long customerId, Date orderDate) {
        this.customerId = customerId;
        this.orderDate = orderDate;
    }

    public Long getId() {
        return id;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public Date getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(Date orderDate) {
        this.orderDate = orderDate;
    }

}
