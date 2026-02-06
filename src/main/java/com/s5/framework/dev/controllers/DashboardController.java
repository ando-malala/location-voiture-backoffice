package com.s5.framework.dev.controllers;

import com.maharavo.flame.models.ModelView;
import com.maharavo.flame.stereotype.Controller;
import com.maharavo.flame.web.bind.GetMapping;
import com.s5.framework.dev.models.Vehicle;
import com.s5.framework.dev.services.VehicleService;
import com.s5.framework.dev.config.ApplicationContextProvider;

import java.util.List;

@Controller
public class DashboardController {

    private VehicleService vehicleService;

    public DashboardController() {
    }

    private VehicleService getVehicleService() {
        if (this.vehicleService == null)
            this.vehicleService = ApplicationContextProvider.getBean(VehicleService.class);
        return this.vehicleService;
    }

    @GetMapping("/test")
    public ModelView index() {
        List<Vehicle> vehicles = getVehicleService().findAllWithAssociations();
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
