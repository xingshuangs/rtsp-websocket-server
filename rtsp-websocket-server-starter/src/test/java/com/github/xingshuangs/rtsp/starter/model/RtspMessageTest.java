package com.github.xingshuangs.rtsp.starter.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.xingshuangs.rtsp.starter.enums.ERtspMessageType;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

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
}