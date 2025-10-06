package com.grow.notification_service.test;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AppController {

    @GetMapping("/")
    public String test() {
        return "알림 서버 메인 페이지 연결 성공";
    }
}
