# language: es
@RF-06 @RF-07
Característica: Revisión y firma del genetista antes de emitir el informe
  Como genetista
  Quiero firmar los resultados patogénicos antes de que salga el informe
  Para que ningún hallazgo grave llegue al paciente sin revisión médica

  @critico
  Escenario: No se emite el informe de una muestra con patogénicas sin firmar
    Dada una muestra aceptada con una variante "patogénica"
    Cuando se pide el informe de la muestra
    Entonces el informe no se emite
    Y la muestra sigue en estado "Pendiente de firma"

  Escenario: Con la firma del genetista se emite el informe
    Dada una muestra aceptada con una variante "patogénica"
    Y el genetista firma la muestra
    Cuando se pide el informe de la muestra
    Entonces el informe se emite
    Y la muestra queda en estado "Informe emitido"

  @critico
  Esquema del escenario: Solo el genetista puede firmar
    Dada una muestra aceptada con una variante "patogénica"
    Cuando un usuario con rol "<rol>" intenta firmar la muestra
    Entonces la firma se rechaza por falta de permisos
    Y la muestra sigue en estado "Pendiente de firma"

    Ejemplos:
      | rol            |
      | técnico        |
      | bioinformático |

  Escenario: Una muestra sin variantes patogénicas no necesita firma
    Dada una muestra aceptada con una variante "benigna"
    Cuando se pide el informe de la muestra
    Entonces el informe se emite
    Y la muestra queda en estado "Informe emitido"

  Escenario: No se emite el informe de una muestra bloqueada
    Dada una muestra bloqueada por contaminación
    Cuando se pide el informe de la muestra
    Entonces el informe no se emite
    Y la muestra sigue en estado "Bloqueada"
