/**
 * User: Himal_J
 * Date: 2/18/2025
 * Time: 9:17 AM
 * <p>
 */

package com.dtech.auth.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "api/v1/sign-up")
@Log4j2
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SignUpController {
}
