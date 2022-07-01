create table if not exists customer_order
(
    ID bigint auto_increment
    primary key,
    CUSTOMER_ID bigint not null,
    ORDER_DATE date not null
);