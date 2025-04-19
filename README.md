# ChuteAlerte📱

ChuteAlerte est une application Android développée en Kotlin qui détecte les chutes des utilisateurs à l'aide de capteurs embarqués (accéléromètre, etc.). L'application envoie automatiquement une alerte et permet de prévenir un contact d'urgence.
Elle est particulièrement destinée aux **personnes âgées**, afin d'améliorer leur sécurité et leur autonomie au quotidien.

## Fonctionnalités principales

- Détection de chute en temps réel à l’aide de seuils dynamiques
- Envoi des données de capteurs vers **Firebase Realtime Database**
- Interface utilisateur intuitive : Urgence, Signaux, Paramètres
- Choix du contact d’urgence via l'application
- Intégration avec MQTT (optionnel)

## Technologies utilisées

- **Langage** : Kotlin
- **Framework** : Android SDK
- **Backend** : Firebase Realtime Database
- **MQTT** : Communication optionnelle via Mosquitto //juste pour le test
- **Gradle** : Pour la gestion de build
