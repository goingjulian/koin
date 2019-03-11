/*
 * Copyright 2017-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.koin.core.registry

import org.koin.core.Koin
import org.koin.core.error.NoScopeDefinitionFoundException
import org.koin.core.error.ScopeAlreadyCreatedException
import org.koin.core.error.ScopeNotCreatedException
import org.koin.core.module.Module
import org.koin.core.qualifier.Qualifier
import org.koin.core.scope.ScopeDefinition
import org.koin.core.scope.ScopeInstance
import java.util.concurrent.ConcurrentHashMap

/**
 * Scope Registry
 * create/find scopes for Koin
 *
 * @author Arnaud Giuliani
 */
class ScopeRegistry {

    private val definitions = ConcurrentHashMap<String, ScopeDefinition>()
    private val instances = ConcurrentHashMap<String, ScopeInstance>()

    internal fun loadScopes(modules: Iterable<Module>) {
        modules.forEach {
            declareScopes(it)
        }
    }

    fun loadDefaultScopes(koin: Koin) {
        ScopeInstance.GLOBAL.register(koin)
        saveInstance(ScopeInstance.GLOBAL)
    }

    private fun declareScopes(module: Module) {
        module.scopes.forEach {
            saveDefinition(it)
        }
    }

    private fun saveDefinition(scopeDefinition: ScopeDefinition) {
        val foundDefinition = definitions[scopeDefinition.scopeName.toString()]
        if (foundDefinition == null) {
            definitions[scopeDefinition.scopeName.toString()] = scopeDefinition
        } else {
            val currentDefinitions = foundDefinition.definitions
            currentDefinitions.addAll(scopeDefinition.definitions)
            foundDefinition.definitions = currentDefinitions
        }
    }

    fun getScopeDefinition(scopeName: String): ScopeDefinition? = definitions[scopeName]

    /**
     * Create a scope instance for given scope
     * @param id - scope instance id
     * @param scopeName - scope qualifier
     */
    fun createScopeInstance(id: String, scopeName: Qualifier? = null): ScopeInstance {
        val definition: ScopeDefinition? = scopeName?.let {
            definitions[scopeName.toString()]
                    ?: throw NoScopeDefinitionFoundException("No scope definition found for scopeName '$scopeName'")
        }
        val instance = ScopeInstance(id, definition)
        registerScopeInstance(instance)
        return instance
    }

    private fun registerScopeInstance(instance: ScopeInstance) {
        if (instances[instance.id] != null) {
            throw ScopeAlreadyCreatedException("A scope with id '${instance.id}' already exists. Reuse or close it.")
        }
        saveInstance(instance)
    }

    fun getScopeInstance(id: String): ScopeInstance {
        return instances[id]
                ?: throw ScopeNotCreatedException("ScopeInstance with id '$id' not found. Create a scope instance with id '$id'")
    }

    private fun saveInstance(instance: ScopeInstance) {
        instances[instance.id] = instance
    }

    fun getScopeInstanceOrNull(id: String): ScopeInstance? {
        return instances[id]
    }

    fun deleteScopeInstance(id: String) {
        instances.remove(id)
    }

    fun close() {
        definitions.clear()
        instances.values.forEach { it.close() }
        instances.clear()
    }
}