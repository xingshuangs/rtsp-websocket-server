package com.github.xingshuangs.rtsp.starter.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.xingshuangs.rtsp.starter.enums.ERtspMessageType;
import com.sun.deploy.util.StringUtils;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;


class RtspMessageTest {

    @Test
    void getType() throws JsonProcessingException {

        ObjectMapper objectMapper = new ObjectMapper();
        RtspMessage<String> message = new RtspMessage<>();
        message.setType(ERtspMessageType.QUERY);
        message.setContent("channel");
        String res = objectMapper.writeValueAsString(message);
        RtspMessage<String> readValue = objectMapper.readValue(res, new TypeReference<RtspMessage<String>>() {
        });
        assertEquals(readValue.getType(), message.getType());
        assertEquals(readValue.getContent(), message.getContent());
    }

    @Test
    void test1() {
        Set<Integer> list = new HashSet<>();
        list.add(1);
        list.add(2);
        String join = StringUtils.join(list.stream().map(String::valueOf).collect(Collectors.toList()), ",");
        System.out.println(join);
    }
}