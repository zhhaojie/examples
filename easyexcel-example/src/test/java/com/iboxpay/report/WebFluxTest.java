package com.iboxpay.report;

import reactor.core.publisher.Flux;

public class WebFluxTest {

    public static void main(String[] args) {

        Flux.just("1", "2").subscribe(s -> System.out.println(s));

    }
}
