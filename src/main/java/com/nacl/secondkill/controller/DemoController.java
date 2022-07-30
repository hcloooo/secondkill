package com.nacl.secondkill.controller;

import com.nacl.secondkill.entity.User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/demo")
public class DemoController {

    @RequestMapping("/hello")
    public String hello(Model model, User user) {
        model.addAttribute("name", "nacl");
        System.out.println(user);
        return "hello";
    }

}
