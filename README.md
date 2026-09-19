# Napoli KMM DeepLink

**Versión 2.0.0**

Motor Kotlin Multiplatform (KMM) para manejo de deep links: auth-gate,
persistencia de URI pendiente, y bus de eventos cold-start-safe. El módulo
no conoce el shape de las URIs ni las rutas de negocio — el proyecto
consumidor define su propio parser (`String -> Route?`), su propio mapper
(`Route -> Destination?`), y provee `DeepLinkAuthProvider` +
`PendingDeepLinkRepository`.

```kotlin
// En el proyecto consumidor
class MyAppDeepLinkParser {
    fun parse(uri: String): MyAppRoute? { /* ... */ }
}

class MyAppDeepLinkMapper {
    fun toDestination(route: MyAppRoute): Destination? { /* ... */ }
}

val handleDeepLink = HandleDeepLinkUseCase(
    parse = parser::parse,
    mapToDestination = mapper::toDestination,
    authProvider = myAuthProviderImpl,
    pendingRepository = myPendingRepositoryImpl,
    navigationCoordinator = coordinator,
    authGraphDestination = AuthDestination.Graph,
)
```

---

## ✨ Características

- **🧩 Agnóstico de rutas**: `parse`/`mapToDestination` son funciones inyectadas — el módulo no define ningún `Route` concreto
- **🔐 Auth-gate**: usuario sin sesión → persiste URI + navega al graph de auth; usuario logueado → navega directo
- **📦 Cold-start safe**: `DeepLinkBus` bufferea URIs que llegan antes de que el NavHost monte
- **♻️ Retry en NavController no listo**: si `navigate()` falla (coordinator no attached), persiste para drenar después
- **📱 Multiplataforma**: Android + iOS

---

## 📋 Requisitos

- **Kotlin**: 2.3+
- **Koin**: 4.0+ (el proyecto consumidor arma su propio módulo DI, este módulo no expone uno)
- **napoli-kmm-base** (`NavigationCoordinator`, `Destination`)
- **napoli-kmm-navigation** (`NavigateToRoute`)

---

## 📦 Instalación

### 1. Agregar el repositorio

```kotlin
// settings.gradle.kts
maven {
    url = uri("https://maven.pkg.github.com/elNapoli/kmm-deeplink")
    credentials {
        username = System.getenv("GITHUB_ACTOR") ?: providers.gradleProperty("gpr.user").orNull
        password = System.getenv("GITHUB_TOKEN") ?: providers.gradleProperty("gpr.token").orNull
    }
}
```

### 2. Agregar la dependencia

Dos artefactos: `deeplink-kmp-domain` (contratos + use cases puros, sin
Compose) y `deeplink-kmp-presentation` (`HandleDeepLinkUseCase`, que necesita
`NavigationCoordinator` de `base-kmp-presentation`).

```toml
# gradle/libs.versions.toml
[versions]
napoli-deeplink = "2.0.0"

[libraries]
napoli-kmm-deeplink-domain = { module = "cl.baldomeronapoli:deeplink-kmp-domain", version.ref = "napoli-deeplink" }
napoli-kmm-deeplink-presentation = { module = "cl.baldomeronapoli:deeplink-kmp-presentation", version.ref = "napoli-deeplink" }
```

`ConsumePendingDeepLinkUseCase` (no necesita NavController, solo se usa post-login
para saber a dónde navegar) vive en `deeplink-kmp-domain` — cualquier módulo que
solo necesite drenar el link pendiente (p.ej. un `domain` de feature) puede
depender únicamente de `deeplink-kmp-domain`, sin arrastrar `base-kmp-presentation`.

### 3. Implementar los contratos

```kotlin
import cl.baldomeronapoli.deeplink.domain.auth.DeepLinkAuthProvider
import cl.baldomeronapoli.deeplink.domain.repository.PendingDeepLinkRepository

class MyAuthProvider(private val session: SessionRepository) : DeepLinkAuthProvider {
    override fun currentUserId(): String? = session.currentUserId()
}

class MyPendingDeepLinkRepository(
    private val prefs: AppPreferencesRepository,
) : PendingDeepLinkRepository {
    override suspend fun save(uri: String) { prefs.pendingDeepLink = uri }
    override suspend fun consume(): String? {
        val uri = prefs.pendingDeepLink
        prefs.pendingDeepLink = null
        return uri
    }
    override suspend fun clear() { prefs.pendingDeepLink = null }
}
```

### 4. Definir tu propio Route + Parser + Mapper (viven en tu proyecto)

```kotlin
sealed class MyAppRoute {
    data class ProductDetail(val id: String) : MyAppRoute()
    data object PaymentBridgeReturn : MyAppRoute() // pass-through, no navega
}

class MyAppDeepLinkParser {
    fun parse(uri: String): MyAppRoute? {
        if (!uri.startsWith("myapp://")) return null
        // ...
    }
}

class MyAppDeepLinkMapper {
    fun toDestination(route: MyAppRoute): Destination? = when (route) {
        is MyAppRoute.ProductDetail -> ProductDestination.Detail(route.id)
        is MyAppRoute.PaymentBridgeReturn -> null // handled elsewhere, not a nav target
    }
}
```

### 5. Wire con Koin

```kotlin
factory {
    HandleDeepLinkUseCase(
        parse = get<MyAppDeepLinkParser>()::parse,
        mapToDestination = get<MyAppDeepLinkMapper>()::toDestination,
        authProvider = get(),
        pendingRepository = get(),
        navigationCoordinator = get(),
        authGraphDestination = AuthDestination.Graph,
    )
}
```

---

## 🏗️ Arquitectura

```
deeplink-kmp-domain/commonMain/          -- sin Compose, api(base-kmp-domain) + api(navigation-kmp-domain)
├── domain/
│   ├── bus/DeepLinkBus.kt                       # buffer cold-start-safe
│   ├── auth/DeepLinkAuthProvider.kt             # interface: currentUserId()
│   ├── repository/PendingDeepLinkRepository.kt  # interface: save/consume/clear
│   └── usecase/
│       └── ConsumePendingDeepLinkUseCase.kt     # drena URI pendiente post-login

deeplink-kmp-presentation/commonMain/    -- api(deeplink-kmp-domain) + api(base-kmp-presentation)
└── usecase/
    └── HandleDeepLinkUseCase.kt                 # auth-gate + persist-or-navigate
                                                  # (necesita NavigationCoordinator)
```

`HandleDeepLinkUseCase` vive en `presentation`, no en `domain`, porque su
constructor recibe `NavigationCoordinator` — un tipo de `base-kmp-presentation`,
no de `base-kmp-domain`. El resto del módulo (bus, contratos, el use case de
consumo post-login) es Kotlin puro y no necesita saber nada de navegación real.

## 🚀 Publicar una nueva versión

```bash
git tag v2.0.1
git push origin v2.0.1
```

El workflow `.github/workflows/publish.yml` publica automáticamente a GitHub
Packages al pushear un tag `v*`.
