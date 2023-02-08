# Les requêtes HTTP sur le port 80 sont redirigées vers le port 5000 du conteneur
docker container run -p 80:5000 server-flask
