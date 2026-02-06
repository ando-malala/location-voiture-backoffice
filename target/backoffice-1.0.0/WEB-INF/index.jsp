<%@ page import="java.util.List, com.s5.framework.dev.models.Vehicle" %>
<!DOCTYPE html>
<html lang="fr">
<head>
  <meta charset="UTF-8" />
  <title>Liste des véhicules</title>
  <style>
    table { border-collapse: collapse; width: 100%; }
    th, td { border: 1px solid #ccc; padding: 8px; text-align: left; }
    th { background: #f5f5f5; }
  </style>
</head>
<body>
  <%
    String title = (String) request.getAttribute("title");
    @SuppressWarnings("unchecked")
    List<Vehicle> vehicles = (List<Vehicle>) request.getAttribute("vehicles");
  %>

  <h1><%= title != null ? title : "Liste des véhicules" %></h1>

  <% if (vehicles != null && !vehicles.isEmpty()) { %>
    <table>
      <thead>
        <tr>
          <th>ID</th>
          <th>Immat.</th>
          <th>VIN</th>
          <th>Modèle</th>
          <th>Type</th>
          <th>Couleur</th>
          <th>Année</th>
          <th>Statut</th>
        </tr>
      </thead>
      <tbody>
        <% for (Vehicle vehicle : vehicles) { %>
          <tr>
            <td><%= vehicle.getId() %></td>
            <td><%= vehicle.getLicensePlate() %></td>
            <td><%= vehicle.getVin() %></td>
            <td><%= vehicle.getModel() != null ? vehicle.getModel().getName() : "" %></td>
            <td><%= vehicle.getType() != null ? vehicle.getType().getCode() : "" %></td>
            <td><%= vehicle.getColor() != null ? vehicle.getColor() : "" %></td>
            <td><%= vehicle.getYear() != null ? vehicle.getYear() : "" %></td>
            <td><%= vehicle.getVehicleStatus() != null ? vehicle.getVehicleStatus().getCode() : "" %></td>
          </tr>
        <% } %>
      </tbody>
    </table>
  <% } else { %>
    <p>Aucun véhicule trouvé.</p>
  <% } %>
</body>
</html>