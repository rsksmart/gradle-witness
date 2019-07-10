plugins {
    `kotlin-dsl`
}

repositories {
    jcenter()
}

dependencies {
    testCompileOnly(gradleTestKit())
    testImplementation("junit:junit:4.12")
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
