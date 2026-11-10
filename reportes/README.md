# Reportes de la corrida final (GCL-36)

Corrida del 10/11/2026, con la API, el portal, Selenium y la auditoría, el informe guardado y la reclasificación.

| Herramienta | Resultado | Reporte |
|---|---|---|
| JUnit 5 + Spring Boot Test + Cucumber + Selenium | 118 tests, 0 fallas, 0 errores | `junit/` (resumen por clase) |
| Cucumber (BDD, 5.2) | 14 escenarios, 14 aprobados | `cucumber/cucumber.html` |
| JaCoCo (cobertura) | 95,8 % instrucciones · 87,9 % ramas (124/141) · 94,0 % líneas (474/504) | `jacoco/index.html` |
| PIT (mutación, reglas de umbral) | 41 mutantes, 41 eliminados (100 %) | `pit/index.html` |
| Postman / Newman (API, 6.3) | 37 pedidos, 76 aserciones, 0 fallas | `postman/newman.html` |
| Selenium (portal, 7.5) | 5 casos aprobados | `selenium/` (captura final de cada caso) |

## Cómo regenerarlos

Desde la raíz del proyecto, con Java 21 y Chrome instalados:

```
.\mvnw clean test
.\mvnw test-compile org.pitest:pitest-maven:mutationCoverage
```

Quedan en `target/site/jacoco`, `target/pit-reports`, `target/cucumber-reports`, `target/surefire-reports` y `target/selenium-capturas`.

Para Postman, con la app levantada (`.\mvnw spring-boot:run`):

```
npx -p newman -p newman-reporter-htmlextra newman run postman/GenoClasifica.postman_collection.json -e postman/GenoClasifica-dev.postman_environment.json -r cli,htmlextra --reporter-htmlextra-export reportes/postman/newman.html
```
