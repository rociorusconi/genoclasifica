# Colección de Postman (sección 6.3)

1. Levantar la API: `.\mvnw spring-boot:run` (perfil `dev`, puerto 8080).
2. En Postman: Import → los dos `.json` de esta carpeta. Elegir el entorno "GenoClasifica dev (local)".
3. Ejecutar la colección completa con el Runner, en orden (cada carpeta usa los tokens y muestras de las anteriores).

Por consola, sin abrir Postman:

```
npx newman run postman/GenoClasifica.postman_collection.json -e postman/GenoClasifica-dev.postman_environment.json
```
