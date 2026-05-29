package com.memoassistant.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaForwardController {
    @GetMapping(value = {"/", "/{path:^(?!api|assets|favicon).*}/**"})
    public String forward() {
        return "forward:/index.html";
    }
}

