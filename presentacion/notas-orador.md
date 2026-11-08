# Notas del orador: GenoClasifica 4.2.0 (TPI Verificación y Validación de Software)

**Duración total:** 15 minutos, 5 minutos por integrante.

| Integrante | Slides | Tiempo por slide | Total |
|---|---|---|---|
| Kiara Precopio | 1, 2, 3 y 10 | ~1 min 15 s | 5 min |
| Rocío Rusconi | 4, 5 y 6 | ~1 min 40 s | 5 min |
| Kalil Yasin | 7, 8 y 9 | ~1 min 40 s | 5 min |

> Kiara tiene 4 slides, así que las suyas van un poco más rápidas para que el reparto quede parejo. La slide 9 quedó asignada a Kalil para equilibrar los tiempos. Si prefieren que la presente Rocío, hay que cambiar el texto "Presenta: Kalil" en la slide 9.

> Los puntos marcados con **⚠ Revisar** son cosas que conviene confirmar antes de exponer.

---

## Slide 1: Portada y problema (Kiara, ~1:15)

- Presentarnos: somos un equipo de QA independiente. Nuestro trabajo es recomendar si la versión 4.2.0 de GenoClasifica se libera, se libera con condiciones o no se libera.
- Qué es GenoClasifica: un sistema de IA que usa un laboratorio de genómica clínica (LGCA). Para cada variante genética, la IA propone una de cinco categorías (benigna, probablemente benigna, significado incierto, probablemente patogénica o patogénica) junto con su nivel de confianza. Después un genetista revisa, firma, y recién entonces se emite el informe PDF.
- Qué trae la 4.2.0: un modelo nuevo para inserciones y deleciones, bases de datos actualizadas, informes automáticos y una API nueva para conectarse con el LIMS.
- Por qué importa tanto probarlo bien: el riesgo es clínico. Un falso negativo puede dejar a un paciente sin seguimiento. Un falso positivo puede llevar a estudios o incluso cirugías que no hacían falta.
- Pase: "Con esto en mente, les cuento qué decidimos probar y cómo."

## Slide 2: Alcance y estrategia (Kiara, ~1:15)

- Primero, una decisión clave: no tenemos el código ni el modelo real. Por eso armamos una implementación de referencia propia en Java 21 con Spring Boot, que cumple las reglas RF-01 a RF-10.
- La IA está simulada: nosotros elegimos la categoría y la confianza. Por eso nuestras conclusiones valen para las reglas y para nuestra implementación, no para el modelo real.
- Usamos solo datos sintéticos, por las leyes 25.326 (datos personales) y 26.529 (derechos del paciente).
- Dentro del alcance: las reglas, la API, la revisión y firma, el informe y un portal mínimo. Fuera: el modelo real, carga y disponibilidad, producción y datos reales.
- La tabla resume la estrategia: qué probamos, en qué nivel, con qué técnica y con qué herramienta. No hace falta leerla entera; alcanza con remarcar que combinamos pruebas unitarias, de integración, de aceptación y de sistema.
- **⚠ Revisar:** la columna "Dentro / Fuera" y la asignación de técnicas en la tabla son una síntesis armada a partir del contenido. Confirmen que coinciden con el plan de pruebas.

## Slide 3: Riesgos y criterios (Kiara, ~1:15)

- Priorizamos por riesgo clínico. Los cuatro principales:
  - **R-01:** la IA da un falso negativo. Es el más grave.
  - **R-02:** fallas de calidad o bloqueo de la muestra.
  - **R-03:** se emite un informe sin firma del genetista.
  - **R-05:** seguridad y trazabilidad.
- La decisión de liberar se toma con una regla basada en nuestro Definition of Done: liberar, liberar con condiciones o no liberar.
- Entornos: desarrollo, QA y staging, separados con perfiles de Spring Boot. Producción solo la describimos, no la probamos.
- **⚠ Revisar:** las condiciones de la regla de liberación quedaron como [COMPLETAR], porque el texto del DoD no estaba en el contenido. Hay que copiarlas del plan.
- Pase a Rocío: "Ahora Rocío les cuenta cómo pasamos de los requisitos a los casos de prueba."

## Slide 4: Análisis de requerimientos y valores límite (Rocío, ~1:40)

- Al leer los requisitos encontramos frases ambiguas. En lugar de adivinar, dejamos el supuesto por escrito:
  - "Menos de 80 %" lo tomamos como estricto: 80,0 % ya alcanza.
  - Un 3 % de contaminación no bloquea la muestra.
  - Un 70 % de confianza sí clasifica.
- Con los supuestos definidos aplicamos valores límite. Para el umbral de confianza probamos justo debajo, justo en el borde y justo encima: 69,9, 70,0 y 70,1.
- La regla más importante: nunca se puede clasificar como benigna con confianza menor a 70 %. Ahí es donde un error se convierte en un falso negativo.
- Idea para remarcar: los errores suelen estar en los bordes, y por eso probamos ahí.

## Slide 5: Transición de estados (Rocío, ~1:40)

- Modelamos el ciclo de vida de la muestra como una máquina de estados: Recibida, Clasificada, Pendiente de firma, Firmada, Informe emitido y, si cambia la clasificación, Reclasificación notificada.
- También hay salidas: Bloqueada (por contaminación), Calidad insuficiente (por Q30 bajo) y Rechazada.
- No solo probamos las transiciones válidas, también las inválidas. Ejemplo: pedir el PDF sin firma. El sistema tiene que rechazar el pedido, y la muestra tiene que seguir en "Pendiente de firma". Esto cubre directamente el riesgo R-03.
- **⚠ Revisar:** en el diagrama, "Rechazada" sale de "Pendiente de firma". Confirmen que ese es el estado de origen correcto en su modelo; si no, hay que mover la flecha.

## Slide 6: Tablas de decisión, caja gris y pairwise (Rocío, ~1:40)

- **Tabla de decisión:** para emitir el informe miramos tres condiciones (si la muestra fue aceptada, si hay variantes patogénicas y si hay firma). Sin muestra aceptada o sin firma, el informe no se emite.
- **Caja gris:** como conocemos parte de la implementación, verificamos que el hash del PDF original no cambie después de una reclasificación. El informe que ya se emitió no se modifica.
- **Pairwise:** de 54 combinaciones posibles de parámetros bajamos a 9 casos, un 83 % menos, y cada par de valores sigue cubierto.
- **⚠ Revisar:** las filas de la tabla de decisión las armé suponiendo que la firma es obligatoria siempre y que las variantes patogénicas no cambian la emisión. Compárenlas con su tabla real y corrijan si su tabla distingue ese caso.
- Pase a Kalil: "Kalil les muestra la parte técnica: caja blanca y automatización."

## Slide 7: Caja blanca (Kalil, ~1:40)

- Tomamos el método `esCalidadSuficiente` de la clase `ControlCalidad`, que decide si la calidad de la secuenciación (Q30) alcanza.
- Tiene 3 caminos independientes:
  - **C1:** Q30 inválido (nulo, negativo o mayor a 100) → lanza excepción.
  - **C2:** Q30 menor a 80 → calidad insuficiente.
  - **C3:** Q30 mayor o igual a 80 → calidad suficiente.
- Escribimos tests para cubrir cada camino y los bordes. Están todos en verde y JaCoCo marca 100 % de líneas y de ramas.
- La clase se hizo con TDD: primero el test en rojo, después el código mínimo para pasarlo y después el refactor.
- **⚠ Revisar:** en el repo, `ControlCalidadTest.java` tiene **8** métodos `@Test` (CP-Q30-01 a 08), no 9. Confirmen el número antes de exponer. También conviene volver a correr `mvnw test jacoco:report` para tener a mano el reporte del 100 %.

## Slide 8: Automatización, BDD y mutación (Kalil, ~1:40) · EN CURSO

- **Cucumber (BDD):** escenarios Given-When-Then para la revisión y firma y para el umbral de confianza. Sirven como documentación que entiende alguien que no programa.
- **Postman:** colección para la API (login, permisos por rol y datos inválidos o repetidos).
- **JUnit 5:** suite de regresión, con evidencia de red-green-refactor en los commits.
- **PIT (mutación):** cambia el código a propósito para ver si los tests lo detectan. Mide si los tests realmente verifican algo o solo pasan por el código.
- Resultados: cobertura total [COMPLETAR] contra un objetivo de 80 %, y puntaje de mutación [COMPLETAR] contra un objetivo de 75 %.
- Si todavía no hay datos el día de la exposición, decirlo tal cual: "esto está en curso".

## Slide 9: Pruebas manuales, portal y defectos (Kalil, ~1:40) · EN CURSO

- **Exploratorias:** sesiones sobre el portal mínimo, buscando lo que no está escrito en los casos.
- **Selenium:** automatizamos el flujo principal del portal de punta a punta.
- **Jira:** proyecto GCL con un tablero Kanban y 5 épicas. La severidad de cada defecto se define por su impacto clínico, no solo técnico.
- Resultados: casos aprobados, fallidos y bloqueados [COMPLETAR], y defectos por severidad [COMPLETAR].
- Pase a Kiara: "Kiara cierra con las métricas y nuestra recomendación."

## Slide 10: Métricas, conclusiones y recomendación (Kiara, ~1:15) · EN CURSO

- Comparamos los KPIs con los criterios de salida: sensibilidad ≥ 98 %, patogénicas clasificadas como benignas ≤ 1 % y concordancia ≥ 95 %. Aclarar que se midieron sobre el modelo simulado.
- Limitaciones, dichas con honestidad:
  - La independencia es parcial, porque la implementación la hicimos nosotros.
  - No hicimos pruebas de carga ni de disponibilidad.
  - No probamos el modelo real.
- Recomendación: [COMPLETAR: liberar, liberar con condiciones o no liberar, y por qué].
- Cerrar mencionando que el uso de IA en el trabajo está declarado y documentado.
- Cierre: "Gracias. ¿Preguntas?"
