package com.swp391_be.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestAreaController {
    @GetMapping("/manager/dashboard")
    public String manager(@AuthenticationPrincipal UserDetails user) { return "Hello Manager: " + user.getUsername(); }
    @GetMapping("/coach/dashboard")
    public String coach(@AuthenticationPrincipal UserDetails user) { return "Hello Coach: " + user.getUsername(); }
    @GetMapping("/member/dashboard")
    public String member(@AuthenticationPrincipal UserDetails user) { return "Hello Member: " + user.getUsername(); }
    @GetMapping("/receptionist/dashboard")
    public String receptionist(@AuthenticationPrincipal UserDetails user) { return "Hello Receptionist: " + user.getUsername(); }
}
