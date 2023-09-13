package com.github.xingshuangs.rtsp.starter.controller;


import com.github.xingshuangs.rtsp.starter.model.RtspAddress;
import com.github.xingshuangs.rtsp.starter.properties.RtspProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author xingshuang
 */
@Slf4j
@RestController
public class RtspController {

    private final RtspProperties rtspProperties;

    public RtspController(RtspProperties rtspProperties) {
        this.rtspProperties = rtspProperties;
    }

    @GetMapping("/channel/number")
    public ResponseEntity<List<Integer>> getChannelNumber() {
        List<Integer> numbers = this.rtspProperties.getAddresses().stream().map(RtspAddress::getNumber).collect(Collectors.toList());
        return ResponseEntity.ok(numbers);
    }
}
