package com.example.demo.controller;

import com.example.demo.service.CompteBancaireService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

@Controller
@RequestMapping("/")
public class DashboardController {

    private final CompteBancaireService compteBancaireService;

    @Autowired
    public DashboardController(CompteBancaireService compteBancaireService) {
        this.compteBancaireService = compteBancaireService;
    }

    @GetMapping("/")
    public String dashboard(Model model) {
        Map<String, Object> stats = compteBancaireService.obtenirStatistiques();
        model.addAttribute("stats", stats);
        return "dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboardAlternative(Model model) {
        return dashboard(model);
    }
}