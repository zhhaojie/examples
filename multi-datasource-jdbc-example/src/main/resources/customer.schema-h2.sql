create table if not exists customer
(
    ID bigint auto_increment
    primary key,
    FIRST_NAME varchar(255) not null,
    LAST_NAME varchar(255) not null
    );

