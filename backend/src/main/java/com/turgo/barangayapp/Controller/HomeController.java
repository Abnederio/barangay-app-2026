package com.turgo.barangayapp.Controller;

import com.turgo.barangayapp.Model.User;
import com.turgo.barangayapp.Service.UserServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;


@Controller
public class HomeController {

    @GetMapping("/")
    public String home(Model model, @AuthenticationPrincipal OidcUser user){
        model.addAttribute("userName", user.getAttribute("name"));
        return "index";
    }
}
