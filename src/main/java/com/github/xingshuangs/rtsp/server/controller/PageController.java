package com.github.xingshuangs.rtsp.server.controller;


import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * @author xingshuang
 */
@Controller
public class PageController {

    @RequestMapping("/client")
    public String client() {
        return "client";
    }
}
