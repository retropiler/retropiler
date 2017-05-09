/*
 * Copyright (c) 2017 FUJI Goro (gfx).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.retropiler

import javassist.*
import javassist.bytecode.Descriptor
import javassist.bytecode.LocalVariableAttribute

class Weaver(val classPool: ClassPool, config: RetropilerExtension) {

    val runtimePackage = config.runtimePackage
    val optionalType = classPool.get("java.util.Optional")!!
    val optionalDesc = Descriptor.of(optionalType)!!
    val retroOptionalType = classPool.get("$runtimePackage.java.util._Optional")!!
    val retroOptionalDesc = Descriptor.of(retroOptionalType)!!

    val lambdaClassPattern = Regex(".+\\\$\\\$Lambda\\\$\\d+") // for retrolambda
    val lambdaFactoryName = "lambdaFactory$" // for retrolambda

    fun getRetroClassOrNull(className: String): CtClass? {
        return getRetroClassOrNull(classPool.get(className))
    }

    fun getRetroClassOrNull(ctClass: CtClass): CtClass? {
        val packageName = ctClass.packageName
        val simpleName = ctClass.simpleName
        return classPool.getOrNull("$runtimePackage.$packageName._$simpleName")
    }

    fun getRetroClass(ctClass: CtClass): CtClass {
        return getRetroClassOrNull(ctClass)!!
    }

    fun getCompanionClassOrNull(ctClass: CtClass): CtClass? {
        val packageName = ctClass.packageName
        val simpleName = ctClass.simpleName
        return classPool.getOrNull("$packageName.$simpleName${'$'}")
    }

    fun preprocess(ctClass: CtClass) {
        if (lambdaClassPattern.matches(ctClass.simpleName)) {
            fixupLambdaClass(ctClass)
        }

        fixupLocalVariableTypes(ctClass)

        fixupDefaultMethods(ctClass)
    }

    // retrolambda generates `Consumer lambdaFactory$(...)` so it replaces the return type
    // to retropiler runtime classes
    private fun fixupLambdaClass(lambdaClass: CtClass) {
        val retroClass = getRetroClassOrNull(lambdaClass.interfaces[0]) ?: return

        lambdaClass.addInterface(retroClass)

        val lambdaFactory = lambdaClass.getDeclaredMethod(lambdaFactoryName)
        if (lambdaFactory.parameterTypes.isEmpty()) {
            val newLambdaFactory = CtMethod.make("""
                            public static ${lambdaClass.name} _${lambdaFactoryName}() {
                                return instance;
                            }
                        """, lambdaClass)
            lambdaClass.addMethod(newLambdaFactory)
        } else {
            val newLambdaFactory = CtMethod(lambdaClass, "_" + lambdaFactoryName, lambdaFactory.parameterTypes, lambdaClass)
            newLambdaFactory.modifiers = newLambdaFactory.modifiers.or(Modifier.STATIC)
            newLambdaFactory.setBody("""
                            { return new ${lambdaClass.name}($$); }
                        """)
            lambdaClass.addMethod(newLambdaFactory)
        }
    }

    private fun fixupLocalVariableTypes(ctClass: CtClass) {
        ctClass.declaredMethods.forEach { method ->
            val ca = method.methodInfo2.codeAttribute
            val lva = ca?.getAttribute(LocalVariableAttribute.tag) as LocalVariableAttribute?
            if (lva != null) {

                val cp = method.methodInfo2.constPool
                val newLva = LocalVariableAttribute(cp)
                var entryIndex = 0

                (0..lva.tableLength() - 1).forEach { i ->
                    if (lva.descriptor(i) == optionalDesc) {
                        newLva.addEntry(lva.startPc(i),
                                lva.codeLength(i),
                                lva.nameIndex(i),
                                cp.addUtf8Info(retroOptionalDesc),
                                entryIndex)
                        entryIndex += Descriptor.dataSize(retroOptionalDesc)
                    } else {
                        newLva.addEntry(lva.startPc(i),
                                lva.codeLength(i),
                                lva.nameIndex(i),
                                lva.descriptorIndex(i),
                                entryIndex)
                        entryIndex += Descriptor.dataSize(lva.descriptor(i))
                    }
                }

                lva.set(newLva.get())
                ca.maxLocals = entryIndex
            }
        }
    }

    fun fixupDefaultMethods(ctClass: CtClass) {
        // Default methods are transformed into two parts by retrolambda:
        // (1) abstract methods in interface
        // (2) concrete methods in companion class (i.e. _Function$)
        //
        // Here adds all the methods that just calls the default, static methods if needed
        ctClass.methods
                .filter { Modifier.isAbstract(it.modifiers) }
                .forEach { ctMethod ->
                    val companionClass = getCompanionClassOrNull(ctMethod.declaringClass)
                    if (companionClass != null && hasDefaultImpl(companionClass, ctMethod)) {
                        val newMethod = CtMethod(ctMethod.returnType, ctMethod.name, ctMethod.parameterTypes, ctClass)
                        if (ctMethod.returnType.name == "void") {
                            newMethod.setBody("${companionClass.name}.${ctMethod.name}($0, $$);")
                        } else {
                            newMethod.setBody("return ${companionClass.name}.${ctMethod.name}($0, $$);")
                        }
                        ctClass.addMethod(newMethod)
                    }
                }
    }

    fun hasDefaultImpl(companionClass: CtClass, ctMethod: CtMethod): Boolean {
        val params = arrayOf(ctMethod.declaringClass) + ctMethod.parameterTypes
        try {
            companionClass.getDeclaredMethod(ctMethod.name, params)
            return true
        } catch (e: NotFoundException) {
            return false
        }
    }

    fun postprocess(ctClass: CtClass) {
        if (!lambdaClassPattern.matches(ctClass.simpleName)) {
            return
        }

        val retroClass = getRetroClassOrNull(ctClass.interfaces[0]) ?: return

        ctClass.interfaces = ctClass.interfaces.filter {
            it == retroClass
        }.toTypedArray()

        ctClass.removeMethod(ctClass.getDeclaredMethod(lambdaFactoryName))
    }
}
