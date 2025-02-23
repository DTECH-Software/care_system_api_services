/**
 * User: Himal_J
 * Date: 2/23/2025
 * Time: 2:06 PM
 * <p>
 */

package com.dtech.auth.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "api/v1/profile")
@Log4j2
@RequiredArgsConstructor
public class ProfileController {

    @PostMapping(path = "/test")
    public String  get(){
        return "Hello World";
    }
}
