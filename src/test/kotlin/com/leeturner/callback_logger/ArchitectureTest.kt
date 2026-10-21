package com.leeturner.callback_logger

import com.tngtech.archunit.base.DescribedPredicate.alwaysTrue
import com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import com.tngtech.archunit.library.Architectures.onionArchitecture
import org.junit.jupiter.api.Test

class ArchitectureTest {
    private val classes =
        ClassFileImporter()
            .withImportOption(ImportOption.DoNotIncludeTests())
            // KSP writes Micronaut's bean definitions for test classes outside the paths DoNotIncludeTests knows about
            .withImportOption { location -> !location.contains("/build/generated/ksp/test/") }
            .importPackages("com.leeturner.callback_logger")

    @Test
    fun `layers follow ports and adapters`() {
        onionArchitecture()
            // `*` is one package segment, so the models are each aggregate's own package and not its ports
            .domainModels("com.leeturner.callback_logger.domain.*")
            .domainServices("com.leeturner.callback_logger.domain.*.ports..")
            .adapter("web", "com.leeturner.callback_logger.adapters.inbound.web..")
            .adapter("db", "com.leeturner.callback_logger.adapters.outbound.db..")
            // There is no application-services layer
            .withOptionalLayers(true)
            // The root package is the composition root: it wires the layers together, so it may depend on any of them
            .ignoreDependency(resideInAPackage("com.leeturner.callback_logger"), alwaysTrue())
            .check(classes)
    }

    @Test
    fun `the domain is free of frameworks`() {
        classes()
            .that()
            .resideInAPackage("com.leeturner.callback_logger.domain..")
            .should()
            .onlyDependOnClassesThat()
            .resideInAnyPackage(
                "com.leeturner.callback_logger.domain..",
                "kotlin..",
                "java..",
                "arrow..",
                "org.jetbrains.annotations..",
            ).check(classes)
    }

    @Test
    fun `nothing depends on the composition root`() {
        noClasses()
            .that()
            .resideOutsideOfPackage("com.leeturner.callback_logger")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("com.leeturner.callback_logger")
            .check(classes)
    }
}
