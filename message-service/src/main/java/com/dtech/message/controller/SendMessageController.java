/**
 * User: Himal_J
 * Date: 2/11/2025
 * Time: 2:30 PM
 * <p>
 */

package com.dtech.message.controller;

import com.dtech.message.dto.request.MessageRequestDTO;
import com.dtech.message.dto.request.validator.MessageRequestValidatorDTO;
import com.dtech.message.dto.response.ApiResponse;
import com.dtech.message.service.SendMessageService;
import com.google.gson.Gson;
import io.swagger.annotations.ApiOperation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;

@RestController
@RequestMapping(path = "api/v1/text")
@Log4j2
@RequiredArgsConstructor
public class SendMessageController {

    @Autowired
    public final SendMessageService sendMessageService;

    @Autowired
    public final Gson gson;

    @PostMapping(path = "/send")
    @ApiOperation(value = "Handle send message request ",notes = "Send message request success or failed")
    public ResponseEntity<ApiResponse<Object>> sendMessage(@RequestBody @Valid  MessageRequestValidatorDTO messageRequestValidatorDTO, Locale locale) {
        log.info("OTP send via txt request  controller {} ", messageRequestValidatorDTO);
        return sendMessageService.sendMessage(gson.fromJson(gson.toJson(messageRequestValidatorDTO), MessageRequestDTO.class), locale);
    }

}
