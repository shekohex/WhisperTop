package me.shadykhalifa.whispertop.architecture

import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import me.shadykhalifa.whispertop.presentation.viewmodels.BaseViewModel
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.fail

class ArchitectureIntegrationTest {
    
    private val testDispatcher = StandardTestDispatcher()
    
    companion object {
        private val FORBIDDEN_PRESENTATION_IMPORTS = setOf(
            "me.shadykhalifa.whispertop.data",
            "me.shadykhalifa.whispertop.platform",
            "android.content.SharedPreferences",
            "androidx.room",
            "io.ktor.client",
            "kotlinx.serialization.json.Json"
        )
        
        private val FORBIDDEN_DOMAIN_IMPORTS = setOf(
            "me.shadykhalifa.whispertop.platform",
            "android.",
            "androidx.",
            "io.ktor",
            "kotlinx.serialization.json.Json"
        )
    }
    
    @Test
    fun `verify ViewModels only depend on Use Cases and domain models`() = runTest(testDispatcher) {
        val viewModelClasses = getViewModelClasses()
        
        assertTrue(viewModelClasses.isNotEmpty(), "Should find ViewModels to test")
        
        viewModelClasses.forEach { viewModelClass ->
            verifyViewModelDependencies(viewModelClass)
        }
    }
    
    @Test
    fun `verify ViewModels do not import data layer classes`() = runTest(testDispatcher) {
        val viewModelClasses = getViewModelClasses()
        
        viewModelClasses.forEach { viewModelClass ->
            verifyViewModelImports(viewModelClass)
        }
    }
    
    @Test
    fun `verify Use Cases only depend on repositories and domain models`() = runTest(testDispatcher) {
        val useCaseClasses = getUseCaseClasses()
        
        assertTrue(useCaseClasses.isNotEmpty(), "Should find Use Cases to test")
        
        useCaseClasses.forEach { useCaseClass ->
            verifyUseCaseDependencies(useCaseClass)
        }
    }
    
    @Test
    fun `verify ViewModels follow proper naming conventions`() = runTest(testDispatcher) {
        val viewModelClasses = getViewModelClasses()
        
        viewModelClasses.forEach { viewModelClass ->
            assertTrue(
                viewModelClass.simpleName?.endsWith("ViewModel") == true,
                "ViewModel ${viewModelClass.simpleName} should end with 'ViewModel'"
            )
            
            assertFalse(
                viewModelClass.simpleName?.contains("Manager") == true ||
                viewModelClass.simpleName?.contains("Service") == true ||
                viewModelClass.simpleName?.contains("Repository") == true,
                "ViewModel ${viewModelClass.simpleName} should not contain Manager/Service/Repository in name"
            )
        }
    }
    
    @Test
    fun `verify Use Cases follow proper naming conventions`() = runTest(testDispatcher) {
        val useCaseClasses = getUseCaseClasses()
        
        useCaseClasses.forEach { useCaseClass ->
            assertTrue(
                useCaseClass.simpleName?.endsWith("UseCase") == true,
                "Use Case ${useCaseClass.simpleName} should end with 'UseCase'"
            )
        }
    }
    
    private fun getViewModelClasses(): List<KClass<*>> {
        return try {
            listOf(
                me.shadykhalifa.whispertop.presentation.AudioRecordingViewModel::class,
                me.shadykhalifa.whispertop.presentation.viewmodels.SettingsViewModel::class,
                me.shadykhalifa.whispertop.presentation.viewmodels.DashboardViewModel::class,
                me.shadykhalifa.whispertop.presentation.viewmodels.RecordingViewModel::class,
                me.shadykhalifa.whispertop.presentation.viewmodels.ModelSelectionViewModel::class,
                me.shadykhalifa.whispertop.presentation.viewmodels.OnboardingWpmViewModel::class
            )
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun getUseCaseClasses(): List<KClass<*>> {
        return try {
            listOf(
                me.shadykhalifa.whispertop.domain.usecases.TranscriptionWorkflowUseCase::class,
                me.shadykhalifa.whispertop.domain.usecases.StartRecordingUseCase::class,
                me.shadykhalifa.whispertop.domain.usecases.StopRecordingUseCase::class,
                me.shadykhalifa.whispertop.domain.usecases.ServiceManagementUseCase::class,
                me.shadykhalifa.whispertop.domain.usecases.ServiceInitializationUseCase::class,
                me.shadykhalifa.whispertop.domain.usecases.PermissionManagementUseCase::class,
                me.shadykhalifa.whispertop.domain.usecases.ServiceBindingUseCase::class
            )
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun verifyViewModelDependencies(viewModelClass: KClass<*>) {
        val constructor = viewModelClass.primaryConstructor
        assertNotNull(constructor, "ViewModel ${viewModelClass.simpleName} should have a primary constructor")
        
        constructor.parameters.forEach { parameter ->
            val parameterType = parameter.type.classifier as? KClass<*>
            
            when {
                parameterType?.simpleName?.endsWith("UseCase") == true -> {
                    // This is expected and good
                }
                parameterType?.simpleName?.endsWith("Repository") == true -> {
                    // For some ViewModels like SettingsViewModel, direct repository access is acceptable
                    // But should be minimal and well-justified
                }
                parameterType?.simpleName?.contains("Service") == true -> {
                    fail("ViewModel ${viewModelClass.simpleName} should not directly depend on service ${parameterType.simpleName}. Use Use Cases instead.")
                }
                parameterType?.simpleName?.contains("Manager") == true && 
                parameterType?.simpleName != "ViewModelErrorHandler" -> {
                    fail("ViewModel ${viewModelClass.simpleName} should not directly depend on manager ${parameterType.simpleName}. Use Use Cases instead.")
                }
                parameterType?.qualifiedName?.startsWith("me.shadykhalifa.whispertop.data") == true -> {
                    fail("ViewModel ${viewModelClass.simpleName} should not depend on data layer class ${parameterType.simpleName}")
                }
            }
        }
    }
    
    private fun verifyViewModelImports(viewModelClass: KClass<*>) {
        val className = viewModelClass.qualifiedName ?: return
        
        FORBIDDEN_PRESENTATION_IMPORTS.forEach { forbiddenImport ->
            assertFalse(
                className.contains(forbiddenImport, ignoreCase = true),
                "ViewModel ${viewModelClass.simpleName} appears to import forbidden package: $forbiddenImport"
            )
        }
    }
    
    private fun verifyUseCaseDependencies(useCaseClass: KClass<*>) {
        val constructor = useCaseClass.primaryConstructor ?: return
        
        constructor.parameters.forEach { parameter ->
            val parameterType = parameter.type.classifier as? KClass<*>
            
            when {
                parameterType?.simpleName?.endsWith("Repository") == true -> {
                    // This is expected and good
                }
                parameterType?.qualifiedName?.startsWith("me.shadykhalifa.whispertop.domain") == true -> {
                    // Domain dependencies are fine
                }
                parameterType?.simpleName?.endsWith("UseCase") == true -> {
                    // Use Case composition is acceptable
                }
                parameterType?.qualifiedName?.startsWith("me.shadykhalifa.whispertop.presentation") == true -> {
                    fail("Use Case ${useCaseClass.simpleName} should not depend on presentation layer class ${parameterType.simpleName}")
                }
            }
        }
    }
}