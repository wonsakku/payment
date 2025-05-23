package com.example.paymentservice.payment.adapter.`in`.web.view

import com.example.paymentservice.common.WebAdapter
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import reactor.core.publisher.Mono


@Controller
@WebAdapter
//@RequestMapping("/v1/toss")
class PaymentController {

    @GetMapping("/success")
    fun successPage(): Mono<String>{
        return Mono.just("success");
    }

    @GetMapping("/fail")
    fun failPage(): Mono<String>{
        return Mono.just("fail");
    }


}