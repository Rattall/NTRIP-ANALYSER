plugins {
    id("com.android.application") version "8.7.3" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
}

gradle.projectsEvaluated {
    allprojects {
        configurations
            .matching { it.name.endsWith("RuntimeClasspathCopy") }
            .configureEach {
                isCanBeConsumed = false
                isCanBeResolved = true
            }
    }
}
