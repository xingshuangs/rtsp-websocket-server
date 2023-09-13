package com.github.xingshuangs.rtsp.starter.controller;


import com.github.xingshuangs.rtsp.starter.model.RtspAddress;
import com.github.xingshuangs.rtsp.starter.properties.RtspProperties;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author xingshuang
 */
@Controller
public class PageController {

    private final RtspProperties rtspProperties;

    public PageController(RtspProperties rtspProperties) {
        this.rtspProperties = rtspProperties;
    }

    @RequestMapping("")
    public String client(Model map) {
        List<Integer> channelNumbers = this.rtspProperties.getAddresses().stream().map(RtspAddress::getNumber).collect(Collectors.toList());
        map.addAttribute("channelNumbers", channelNumbers);
        return "client";
    }
}
