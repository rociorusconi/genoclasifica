# language: es
@RF-05
Característica: Umbral de confianza del modelo
  Como genetista
  Quiero que las variantes con confianza baja queden para revisión
  Para no informar una categoría que el modelo no puede sostener

  Esquema del escenario: La confianza decide si la variante se clasifica
    Dado una variante que el modelo propone como "<categoria>" con <confianza> % de confianza
    Cuando el sistema clasifica la variante
    Entonces la variante queda en estado "<resultado>"

    Ejemplos: Límite del umbral
      | categoria            | confianza | resultado         |
      | patogénica           | 69.9      | requiere revisión |
      | patogénica           | 70.0      | clasificada       |
      | patogénica           | 70.1      | clasificada       |
      | significado incierto | 69.9      | requiere revisión |
      | significado incierto | 70.0      | clasificada       |

  @critico
  Esquema del escenario: Una variante benigna con confianza baja nunca se informa como benigna
    Dado una variante que el modelo propone como "<categoria>" con 69.9 % de confianza
    Cuando el sistema clasifica la variante
    Entonces la variante queda en estado "requiere revisión"
    Y la variante no tiene categoría asignada

    Ejemplos:
      | categoria             |
      | benigna               |
      | probablemente benigna |

  Escenario: Una confianza fuera de rango se rechaza
    Dado una variante que el modelo propone como "patogénica" con 100.1 % de confianza
    Cuando el sistema clasifica la variante
    Entonces la clasificación se rechaza por confianza inválida
