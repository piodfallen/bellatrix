import dev.kordex.gradle.plugins.kordex.DataCollection
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

plugins {
	application
	distribution

	alias(libs.plugins.kotlin.jvm)
	alias(libs.plugins.kotlin.serialization)

	alias(libs.plugins.detekt)
	alias(libs.plugins.flyway)

	alias(libs.plugins.kordex.i18n)
	alias(libs.plugins.kordex.plugin)
	alias(libs.plugins.ksp.plugin)
}

group = "bellatrix"
version = "1.0-SNAPSHOT"

application {
	mainClass = "bellatrix.AppKt"
}

dependencies {
	detektPlugins(libs.detekt)

	implementation(libs.kotlin.stdlib)
	implementation(libs.kx.ser)
	implementation(libs.exposed.core)
	implementation(libs.exposed.jdbc)
	implementation(libs.flyway.core)
	implementation(libs.flyway.database.postgresql)
	implementation(libs.postgresql)

	testImplementation(libs.kotlin.test)

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

flyway {
	locations = arrayOf("filesystem:src/main/resources/db/migration")
}

tasks.register("flywayAdd") {
	group = "flyway"
	description = "Creates the next Flyway SQL migration file."

	doLast {
		val migrationDirectory = layout.projectDirectory
			.dir("src/main/resources/db/migration")
			.asFile
			.also(File::mkdirs)

		val migrationDescription = providers
			.gradleProperty("flyway.add.description")
			.orNull
			?.trim()
			?.lowercase()
			?.replace(Regex("[^a-z0-9]+"), "_")
			?.trim('_')
			?.takeIf(String::isNotBlank)
			?: error("Missing migration description. Use -Pflyway.add.description=modmail")

		val formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
		var migrationVersion = LocalDateTime.now()
		var migrationFile: File

		do {
			val formattedVersion = migrationVersion.format(formatter)
			migrationFile = migrationDirectory.resolve("V${formattedVersion}__${migrationDescription}.sql")
			migrationVersion = migrationVersion.plusSeconds(1)
		} while (migrationFile.exists())

		check(migrationFile.createNewFile()) {
			"Migration file already exists: ${migrationFile.relativeTo(projectDir)}"
		}

		logger.lifecycle("Created ${migrationFile.relativeTo(projectDir)}")
	}
}
