package dev.badbird.backend.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import javax.annotation.security.PermitAll;

@Controller
@PermitAll
public class ReactAppController {
    @GetMapping(value = {"/*"})
    public String index() {
        return "forward:/index.html";
    }
}
