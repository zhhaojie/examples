package com.iboxpay.report;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.openjdk.jol.info.GraphLayout;

import java.util.*;

public class MemoryTest {

    public static void main(String[] args) {
        Map<Long, Order> orderMap = new HashMap<>();
        Random random = new Random();
        // 10*1w objects
        for (long i = 0; i < 10 * 10000; i++) {
            Order o = new Order(i, UUID.randomUUID().toString(), random.nextDouble(), new Date());
            orderMap.put(i, o);
        }
        // 26mb
        System.out.println((GraphLayout.parseInstance(orderMap).totalSize() / 1024 / 1024 + "MB"));
        System.out.println((GraphLayout.parseInstance(orderMap).getClassCounts().count(Order.class)));

        SimpleOrder simpleOrder = new SimpleOrder();
        // 24byte
        System.out.println((GraphLayout.parseInstance(simpleOrder).totalSize()) +"byte");
    }

}

@Data
@AllArgsConstructor
@NoArgsConstructor
class Order {

    private Long id;

    private String OrderNo;

    private Double orderAmt;

    private Date orderTime;

}

@Data
@AllArgsConstructor
@NoArgsConstructor
class SimpleOrder {

    private Long id;

    private String OrderNo;

    private Double orderAmt;


}