# language: fr
Fonctionnalité: Parcours API de base caisse

  Scénario: Authentification super admin
    Etant donné les credentials du super admin sont valides
    Quand le super admin se connecte
    Alors la connexion super admin réussit

  Scénario: Création snack et login manager
    Etant donné un super admin authentifié
    Quand le super admin crée un snack avec manager
    Alors le snack et le manager sont utilisables

  Scénario: Historique des commandes accessible au manager
    Etant donné un manager authentifié
    Quand le manager consulte l'historique des commandes du jour
    Alors l'historique des commandes est retourné
