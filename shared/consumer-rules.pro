# Consumer ProGuard rules for shared module
# These rules are automatically applied to consumers of this library

# ==============================
# Shared Module API Rules
# ==============================

# Keep all public domain models that may be used by consumers
-keep class me.shadykhalifa.whispertop.domain.models.** { *; }

# Keep all repository interfaces
-keep interface me.shadykhalifa.whispertop.domain.repositories.** { *; }

# Keep all use case interfaces and classes
-keep class me.shadykhalifa.whispertop.domain.usecases.** { *; }

# Keep all service interfaces
-keep interface me.shadykhalifa.whispertop.domain.services.** { *; }

# ==============================
# Serialization Rules for Shared Models
# ==============================

# Keep all data transfer objects that will be serialized
-keep @kotlinx.serialization.Serializable class me.shadykhalifa.whispertop.data.models.** { *; }

# ==============================
# Dependency Injection Module Rules
# ==============================

# Keep Koin modules defined in shared
-keep class me.shadykhalifa.whispertop.di.** { *; }

# Keep module functions
-keep class **.*ModuleKt { *; }

# ==============================
# Platform-specific Rules
# ==============================

# Keep expect/actual classes
-keep class me.shadykhalifa.whispertop.data.local.** { *; }
-keep class me.shadykhalifa.whispertop.data.remote.** { *; }

# ==============================
# Error Model Rules
# ==============================

# Keep error classes for proper exception handling
-keep class me.shadykhalifa.whispertop.domain.models.*Error { *; }
-keep class me.shadykhalifa.whispertop.domain.models.*Exception { *; }