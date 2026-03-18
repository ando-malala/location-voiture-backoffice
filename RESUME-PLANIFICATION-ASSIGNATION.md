# Résumé — sprint 5

## 1) Construction des groupes de réservations
- Les réservations du jour sont triées par `dateHeure` croissante.
- Les groupes sont construits par fenêtre:
  - début = première réservation non traitée,
  - fin = début + `Temps d attente` (minutes) récupéré en base.

## 2) Récupération des véhicules disponibles pour un groupe
Pour chaque groupe, la liste des véhicules disponibles est calculée ainsi:
- véhicule jamais assigné => disponible,
- véhicule déjà assigné => disponible si `heureRetour <= finFenetreGroupe`.

## 3) Calcul de l'heure de départ
Après préparation des trajets du groupe:
- on calcule `derniereReservationAssigneeDuGroupe` = max(`dateHeure`) parmi les réservations réellement assignées,
- pour chaque trajet, on prend:
  - `departTrajet = max(derniereReservationAssigneeDuGroupe, heureRetourPrecedenteVehicule)`.


