# Napoli KMM DeepLink — DEPRECATED

**Este repositorio está deprecado.** Su código (auth-gate, persistencia de
URI pendiente, bus de eventos cold-start-safe, `HandleDeepLinkUseCase`,
`ConsumePendingDeepLinkUseCase`) se movió a
[`napoli-kmm-navigation`](https://github.com/elNapoli/kmm-navigation),
paquete `cl.baldomeronapoli.navigation.domain.deeplink` /
`cl.baldomeronapoli.navigation.deeplink`.

Motivo: manejar deep links es un caso de uso de navegación, no una
capacidad aparte — y ya no tenía sentido mantenerlo como librería separada
una vez que dejó de depender de `napoli-kmm-base` (todo lo que usa vive en
`navigation`).

No se publican versiones nuevas de `deeplink-kmp-domain` /
`deeplink-kmp-presentation` a partir de aquí. Los consumidores existentes
en `2.0.1` o anteriores pueden seguir usando esas versiones publicadas,
pero deben migrar a `napoli-kmm-navigation >= 2.2.0` para recibir
actualizaciones.
