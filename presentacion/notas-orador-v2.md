# Notas del orador: GenoClasifica 4.2.0 (TPI Verificación y Validación de Software)

**Duración total:** 15 minutos, 5 minutos por integrante.

| Integrante | Slides | Tiempo por slide | Total |
|---|---|---|---|
| Kiara Precopio | 1, 2, 3, 4, 5, 12 y 13 | ~40 s (portada ~30 s, slide 3 ~50 s, cierre ~1 min) | 5 min |
| Rocío Rusconi | 6, 7 y 8 | ~1 min 40 s | 5 min |
| Kalil Yasin | 9, 10 y 11 | ~1 min 40 s | 5 min |

> Kiara tiene 7 slides, así que las suyas van más rápidas para que el reparto quede parejo. La slide 11 quedó asignada a Kalil para equilibrar los tiempos. Si prefieren que la presente Rocío, hay que cambiar el texto "Presenta: Kalil" en la slide 11.

> Los puntos marcados con **⚠ Revisar** son cosas que conviene confirmar antes de exponer.

---

## Slide 1: Portada (Kiara, ~0:30)

- Presentarnos: somos un equipo de QA independiente. Nuestro trabajo es recomendar si la versión 4.2.0 de GenoClasifica se libera, se libera con condiciones o no se libera.
- Qué es GenoClasifica: un sistema de IA que usa un laboratorio de genómica clínica (LGCA). Para cada variante genética, la IA propone una de cinco categorías (benigna, probablemente benigna, significado incierto, probablemente patogénica o patogénica) junto con su nivel de confianza.
- La IA propone, pero no decide sola: un genetista revisa, firma, y recién entonces se emite el informe PDF.

## Slide 2: Qué cambia y qué está en juego (Kiara, ~0:40)

- Qué trae la 4.2.0: un modelo nuevo para inserciones y deleciones, bases de datos actualizadas, informes automáticos y una API nueva para conectarse con el LIMS.
- Por qué importa tanto probarlo bien: el riesgo es clínico. Un falso negativo puede dejar a un paciente sin seguimiento. Un falso positivo puede llevar a estudios o incluso cirugías que no hacían falta.
- Pase: "Antes de contarles qué probamos, veamos cómo funciona el sistema."

## Slide 3: Cómo funciona GenoClasifica (Kiara, ~0:50)

- Recorrer el flujo de izquierda a derecha: el secuenciador genera los datos, se hace un control de calidad, se detectan las variantes, se anotan con bases públicas, el modelo de IA propone una categoría con su confianza, el genetista revisa y firma, y se emite el informe PDF.
- Además, el LIMS del laboratorio se conecta con GenoClasifica a través de una API REST, en ambos sentidos.
- Lo importante de esta slide es qué parte cubre nuestra implementación de referencia, marcada con la barra roja:
  - El **control de calidad**, con las reglas de Q30, cobertura y contaminación, aplicadas sobre valores sintéticos.
  - Todo lo que va **desde el modelo de IA hasta el informe PDF**.
  - La **API REST** con el LIMS.
- Las etapas en gris punteado (secuenciador, detección de variantes y anotación) quedan fuera de alcance: los valores y las variantes nos llegan ya calculados en un JSON.
- Pase: "Con esto claro, les cuento qué decidimos probar y cómo."

## Slide 4: Alcance y estrategia (Kiara, ~0:40)

- Primero, una decisión clave: no tenemos el código ni el modelo real. Por eso armamos una implementación de referencia propia en Java 21 con Spring Boot, que implementa las reglas del enunciado. (Como ya se vio en la slide 3 qué parte cubre, acá alcanza con decirlo rápido.)
- La IA está simulada: nosotros elegimos la categoría y la confianza. Por eso nuestras conclusiones valen para las reglas y para nuestra implementación, no para el modelo real.
- Usamos solo datos sintéticos, por las leyes 25.326 (datos personales) y 26.529 (derechos del paciente).
- Dentro del alcance: las reglas de calidad, contaminación y confianza, la API para el LIMS, la revisión y firma, la emisión del informe y un portal mínimo. También entran la auditoría inalterable y la reclasificación, que incorporamos al final para poder ejecutar sus casos P1. Fuera: el modelo real, carga y disponibilidad, producción y datos reales.
- La tabla resume la estrategia: qué probamos, en qué nivel, con qué técnica y con qué herramienta. No hace falta leerla entera; alcanza con remarcar que combinamos pruebas unitarias, de integración, de aceptación y de sistema.
- Las combinaciones de pairwise las diseñamos a mano y las ejecutamos con JUnit 5, sin una herramienta que las genere.
- **⚠ Revisar:** la columna "Dentro / Fuera" y la asignación de técnicas en la tabla son una síntesis armada a partir del contenido. Confirmen que coinciden con el plan de pruebas.

## Slide 5: Riesgos y criterios (Kiara, ~0:40)

- Priorizamos por riesgo clínico. La matriz tiene 7 riesgos, ordenados de mayor a menor prioridad.
- Los marcados en rojo son P1 (R-01 a R-05). El más grave es **R-01**: que la IA clasifique una patogénica como benigna, es decir, un falso negativo.
- No hace falta leer los 7: alcanza con nombrar R-01 y decir que los P1 son los que probamos primero y con más profundidad.
- Regla de liberación, basada en el Definition of Done:
  - **Liberar:** se cumple todo el DoD.
  - **Liberar con condiciones:** solo quedan defectos de severidad media o baja, con plan de corrección y fecha.
  - **No liberar:** hay un defecto crítico o alto abierto, falla una métrica del modelo o quedan casos P1 sin ejecutar.
- Probamos en tres entornos (desarrollo, QA y staging) con perfiles de Spring Boot; producción solo se describe.
- Pase a Rocío: "Ahora Rocío les cuenta cómo pasamos de los requisitos a los casos de prueba."

## Slide 6: Análisis de requerimientos y valores límite (Rocío, ~1:40)

- Al leer los requisitos encontramos frases ambiguas. En lugar de adivinar, dejamos el supuesto por escrito:
  - "Menos de 80 %" lo tomamos como estricto: 80,0 % ya alcanza.
  - Un 3 % de contaminación no bloquea la muestra.
  - Un 70 % de confianza sí clasifica.
- Con los supuestos definidos aplicamos valores límite. Para el umbral de confianza probamos justo debajo, justo en el borde y justo encima: 69,9, 70,0 y 70,1.
- La regla más importante: nunca se puede clasificar como benigna con confianza menor a 70 %. Ahí es donde un error se convierte en un falso negativo.
- Idea para remarcar: los errores suelen estar en los bordes, y por eso probamos ahí.

## Slide 7: Cómo diseñamos los casos (Rocío, ~1:40)

- No usamos una sola técnica para todo: elegimos la técnica según el tipo de regla que queríamos probar.
  - Si la regla tiene **umbrales numéricos** (como Q30), usamos partición de equivalencia y valores límite: 41 casos.
  - Si combina **varias condiciones** (como la emisión del informe), usamos tablas de decisión.
  - Si depende del **estado en que está la muestra**, usamos transición de estados.
  - Si hay **muchos parámetros combinados**, usamos pairwise para no probar todas las combinaciones: de 54 bajamos a 9.
  - Con caja gris usamos el sistema por su servicio y verificamos el resultado consultando la base con SQL. Ejemplo: un UPDATE directo sobre la tabla de auditoría se rechaza. Diseñamos 4 casos y se aprobaron los 4.
  - Con caja blanca revisamos las 4 reglas de umbral hasta llegar al 100 % de ramas. Así encontramos 2 casos que faltaban: contaminación nula (CP-CON-08) y confianza nula (CP-CONF-16).
- Las filas en rojo (transición de estados y caja blanca) son las que vemos en detalle a continuación: transición de estados en la próxima slide, y caja blanca en la de Kalil, con la regla de Q30.
- No hace falta leer la tabla entera: el detalle de cada caso está en el informe.
- Pase: "Les muestro una de estas técnicas en detalle: la transición de estados."

## Slide 8: Transición de estados (Rocío, ~1:40)

- Modelamos el ciclo de vida de la muestra como una máquina de estados: Recibida, Clasificada, Pendiente de firma, Firmada, Informe emitido y, si cambia la clasificación, Reclasificación notificada.
- También hay salidas: Bloqueada (por contaminación), Calidad insuficiente (por Q30 bajo) y Rechazada.
- No solo probamos las transiciones válidas, también las inválidas. Ejemplo: pedir el PDF sin firma. El sistema tiene que rechazar el pedido, y la muestra tiene que seguir en "Pendiente de firma". Esto cubre directamente el riesgo R-03.
- Debajo del diagrama está el resultado de la ejecución: automatizamos las transiciones con Spring Boot Test (`MuestraServiceTest` y `ReclasificacionTest`) y se aprobaron las 23. Las de reclasificación (T12 y TI8) se pudieron ejecutar al incorporar la reclasificación: cuando cambia la categoría de una variante se notifica al genetista, y el informe ya emitido no cambia.
- **⚠ Revisar:** en el diagrama, "Rechazada" sale de "Pendiente de firma". Confirmen que ese es el estado de origen correcto en su modelo; si no, hay que mover la flecha.
- Pase a Kalil: "Kalil les muestra la parte técnica: caja blanca y automatización."

## Slide 9: Caja blanca (Kalil, ~1:40)

- Retomamos la regla de Q30 de la slide del mapa. Tomamos el método `esCalidadSuficiente` de la clase `ControlCalidad`, que decide si la calidad de la secuenciación (Q30) alcanza.
- Tiene 3 caminos independientes:
  - **C1:** Q30 inválido (nulo, negativo o mayor a 100) → lanza excepción.
  - **C2:** Q30 menor a 80 → calidad insuficiente.
  - **C3:** Q30 mayor o igual a 80 → calidad suficiente.
- La conclusión principal está abajo: los casos de caja negra de Q30 (CP-Q30-01 a 08) ya recorrían el 100 % del método (JaCoCo marcaba 100 % de líneas y de ramas), pero PIT encontró un mutante vivo, así que agregamos CP-Q30-09 (Q30 = 0).
- La clase se hizo con TDD: primero el test en rojo, después el código mínimo para pasarlo y después el refactor.

## Slide 10: Pruebas automatizadas (Kalil, ~1:40)

- **Qué automatizamos:**
  - **JUnit 5 + Spring Boot Test:** pruebas unitarias, de integración y de regresión.
  - **Postman:** la API (login, permisos por rol y datos inválidos o repetidos).
  - **Cucumber:** escenarios Given-When-Then de revisión y firma, y del umbral de confianza. Sirven como documentación que entiende alguien que no programa.
  - **Selenium:** el portal: flujo de firma, permisos, muestra bloqueada y filtros.
- **Corrida final: 122 tests, 0 fallas.** Todas las pruebas automatizadas pasan.
- **Idea central:** no alcanza con que los tests pasen; con JaCoCo y PIT medimos si realmente detectan errores.
  - **JaCoCo** mide cuánto código recorren los tests (objetivo 80 %). Resultado: **94,0 % de líneas y 87,9 % de ramas** (95,8 % de instrucciones).
  - **PIT** cambia el código a propósito (mutantes) y mira si algún test falla. Resultado: **100 %, 41 de 41 mutantes eliminados**. Ojo: PIT corre solo sobre las reglas de umbral (calidad, contaminación, cobertura, confianza y categorías), no sobre todo el proyecto. En la primera corrida JaCoCo daba 100 % en `ControlCalidad`, pero PIT encontró un mutante vivo (`q30 < 0` cambiado por `<= 0`): faltaba el caso Q30 = 0, y lo agregamos como CP-Q30-09.
- Qué no automatizamos y por qué: exploración del portal y legibilidad del informe (requieren juicio humano), pruebas que se hacen una sola vez y el modelo de IA real (no tenemos acceso).

## Slide 11: Ejecución manual y defectos (Kalil, ~1:40)

- **Ejecución:**
  - Ejecutamos a mano los casos de caja negra y guardamos evidencia con capturas.
  - Hicimos sesiones exploratorias del portal, buscando lo que no está escrito en los casos.
  - Selenium automatiza el flujo principal del portal: búsqueda, filtros y revisión y firma.
  - Resultado: **manual por API, 4 de 6 aprobados; portal, 5 de 6 aprobados; la exploratoria encontró 1 defecto crítico; Selenium, 5 de 5.**
- **Defectos en Jira:** la severidad de cada defecto se define por su impacto clínico, no solo técnico. Cada defecto tiene además prioridad, evidencia y un ciclo de vida.
  - En total, **5 defectos: 1 crítico, 2 altos y 2 bajos. 4 siguen abiertos.**
- Defecto de ejemplo, **GCL-56: se emite el informe con una variante "requiere revisión" sin revisión del genetista**.
  - Lo encontramos en una sesión exploratoria del portal; la evidencia es el PDF de PORT-C.
  - Severidad **crítica**: un informe puede llegar al paciente con una variante dudosa que ningún genetista revisó. Es justo el tipo de error que el sistema existe para evitar.
  - Prioridad **alta**: hay que corregirlo antes de liberar.
  - Estado **Abierto**.
- Si da el tiempo, mostrar GCL-56 en Jira con la evidencia.
- Pase a Kiara: "Kiara cierra con las métricas y nuestra recomendación."

## Slide 12: Resultados frente a los criterios de salida (Kiara, ~0:40)
- Modelo: sensibilidad, patogénicas clasificadas como benignas y concordancia quedaron sin medir, porque no tenemos el modelo real.

- No cumple:

  Casos P1 y P2 aprobados: los casos P1 automatizados se aprobaron con éxito (91/91), pero hubo fallos en la ejecución manual (MAN-05 y MAN-06). Ya no quedan casos bloqueados.

  Defectos críticos o altos abiertos: el objetivo es 0 y hay 3.

- Cumple:

  Cobertura JaCoCo: 94,0 % de líneas y 87,9 % de ramas (objetivo 80 %).

  Mutación PIT: 100 % (41 de 41 mutantes).

  Búsqueda en el portal: responde en milisegundos (medido con 4 muestras), muy por debajo de 3 s.

  "Procesamiento por muestra < 2 h" no es medible, porque nuestra implementación no procesa archivos del secuenciador.

## Slide 13: ¿Liberamos la 4.2.0? (Kiara, ~1:00)
- Recomendación: no liberar.

- Conectarlo con la regla de liberación de la slide de riesgos (slide 5): no se libera si hay un defecto crítico o alto abierto, si falla una métrica del modelo o si quedan casos P1 sin ejecutar. Con una sola condición alcanza:

  Hay un defecto crítico y dos altos abiertos: un informe puede salir con una variante dudosa sin que la revise un genetista, y el genetista firma sin ver la evidencia. Esta condición se cumple.

  Las métricas del modelo no se pudieron medir, así que esa condición no se pudo verificar.

  Todos los casos P1 de diseño automatizado se aprobaron, pero dos fallaron en la ejecución manual (MAN-05 y MAN-06). La reclasificación y la auditoría ya se probaron y funcionan.

  Frase de cierre: "Las pruebas automatizadas de nuestra implementación de referencia finalizaron correctamente. Sin embargo, las pruebas manuales y exploratorias permitieron identificar defectos críticos y altos que impiden recomendar la liberación. Además, no pudimos validar el desempeño del modelo de IA real."

  Conectar con la introducción: verificar es comprobar que cumple la especificación; validar, que le sirve a quien lo usa. GCL-56 cumple la especificación y aun así no es seguro.

  Riesgos residuales y limitaciones, dichos con honestidad:

  La independencia es parcial, porque la implementación de referencia la hicimos nosotros.

  Recibimos la calidad, la cobertura y la contaminación ya calculadas: si el sistema real las calculara mal, no lo detectaríamos. Tampoco probamos el modelo de IA real ni hicimos pruebas de carga o disponibilidad.

  Cerrar mencionando que el uso de IA en el trabajo está declarado y documentado.

  Cierre: "Gracias. ¿Preguntas?"