package com.example.test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import com.example.slackbot.service.SlackService;

public class SlackTest {
    public static void main(String[] args) throws Exception {
        ApplicationContext ctx = new ClassPathXmlApplicationContext("applicationContext.xml");
        SlackService slackService = ctx.getBean(SlackService.class);
        slackService.start();
    }
}
