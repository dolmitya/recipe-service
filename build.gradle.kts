plugins {
	java
	id("org.springframework.boot") version "3.5.0"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "com.recipemaster"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion.set(JavaLanguageVersion.of(21))
	}
}

dependencyManagement {
	imports {
		mavenBom("org.springframework.boot:spring-boot-dependencies:3.5.0")
	}
}

allprojects {
	repositories {
		mavenCentral()
	}
}

subprojects {
	apply(plugin = "java")
	apply(plugin = "io.spring.dependency-management")

	configurations {
		named("compileOnly") {
			extendsFrom(configurations.named("annotationProcessor").get())
		}
	}

	dependencies {
		implementation("org.springframework.boot:spring-boot-starter-data-elasticsearch")
		compileOnly("org.projectlombok:lombok")
		annotationProcessor("org.projectlombok:lombok")
	}
}

dependencies {
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
	useJUnitPlatform()
}
