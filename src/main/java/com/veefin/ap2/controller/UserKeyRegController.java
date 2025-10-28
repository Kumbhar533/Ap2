package com.veefin.ap2.controller;

import com.veefin.ap2.dto.UserKeyDTO;
import com.veefin.ap2.service.UserKeyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user-key-reg")
@RequiredArgsConstructor
public class UserKeyRegController {


    private final UserKeyService userKeyService;
    @PostMapping
    public void registerUserPublicKey(@RequestBody UserKeyDTO userKeyDTO) {

        userKeyService.registerUserPublicKey(userKeyDTO.getUserId(), userKeyDTO.getPublicKey());

    }


}
