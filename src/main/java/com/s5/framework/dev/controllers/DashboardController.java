package com.s5.framework.dev.controllers;

import com.maharavo.flame.models.ModelView;
import com.maharavo.flame.stereotype.Controller;
import com.maharavo.flame.web.bind.GetMapping;
import com.s5.framework.dev.models.Vehicle;
import com.s5.framework.dev.services.VehicleService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Controller
public class DashboardController {

    private final VehicleService vehicleService;

    @Autowired
    public DashboardController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @GetMapping("/test")
    public ModelView index() {
        List<Vehicle> vehicles = vehicleService.findAll();
        ModelView modelView = new ModelView("index");
        modelView.add("vehicles", vehicles);
        modelView.add("title", "Liste des véhicules");
        return modelView;
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        return "dashboard";
    }

}
