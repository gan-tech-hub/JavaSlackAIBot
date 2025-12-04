package com.example.test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import com.example.service.RagService;

public class RagTest {
    public static void main(String[] args) {
        ApplicationContext ctx = new ClassPathXmlApplicationContext("applicationContext.xml");
        RagService ragService = ctx.getBean(RagService.class);

        String answer = ragService.answer("パスワードを忘れた場合どうすればいい？");
        System.out.println(answer);
    }
}
