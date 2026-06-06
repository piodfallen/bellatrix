import dev.kordex.gradle.plugins.kordex.DataCollection

plugins {
	distribution

	alias(libs.plugins.kotlin.jvm)
	alias(libs.plugins.kotlin.serialization)

	alias(libs.plugins.detekt)

	alias(libs.plugins.kordex.i18n)
	alias(libs.plugins.kordex.plugin)
	alias(libs.plugins.ksp.plugin)
}

group = "bellatrix"
version = "1.0-SNAPSHOT"

dependencies {
	detektPlugins(libs.detekt)

	implementation(libs.kotlin.stdlib)
	implementation(libs.kx.ser)
	implementation(libs.exposed.core)
	implementation(libs.exposed.jdbc)
	implementation(libs.postgresql)

	// Logging dependencies
	implementation(libs.groovy)
	implementation(libs.jansi)
	implementation(libs.logback)
	implementation(libs.logback.groovy)
	implementation(libs.logging)
}

// Configure distributions plugin
distributions {
	main {
		distributionBaseName = project.name

		contents {
			// Copy the LICENSE file into the distribution
			from("LICENSE")

			// Exclude src/main/dist/README.md
			exclude("README.md")
		}
	}
}

kordEx {
	// https://github.com/gradle/gradle/issues/31383
	kordExVersion = libs.versions.kordex.asProvider()

	bot {
		// See https://docs.kordex.dev/data-collection.html
		dataCollection(DataCollection.None)

		mainClass = "bellatrix.AppKt"
	}
}

i18n {
	bundle("bellatrix.strings", "bellatrix.i18n")
}

detekt {
	buildUponDefaultConfig = true

	config.from(rootProject.files("detekt.yml"))
}
