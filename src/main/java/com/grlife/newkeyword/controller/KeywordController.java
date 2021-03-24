package com.grlife.newkeyword.controller;

import com.grlife.newkeyword.service.KeywordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class KeywordController {

    private final KeywordService service;

    @Autowired
    public KeywordController(KeywordService service) {
        this.service = service;
    }

    @Scheduled(fixedDelay=100)
    public void insertScheduler() {
        System.out.println("Keyword System Start");
        service.startKeyword();

    }

    @RequestMapping("/")
    public String home(Model model) {

        //service.startKeyword();

        return "test";
    }

    @RequestMapping("getKeywordData")
    public String getKeywordData(Model model) {

        //service.startKeyword();

        return "test";
    }

}
