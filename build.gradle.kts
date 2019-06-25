plugins {
    `kotlin-dsl`
}

repositories {
    jcenter()
}

version = "0.1.0"

gradlePlugin {
    plugins {
        register("witness") {
            id = "witness"
            implementationClass = "org.whispersystems.witness.WitnessPlugin"
        }
    }
}

kotlinDslPluginOptions {
    experimentalWarning.set(false)
}
