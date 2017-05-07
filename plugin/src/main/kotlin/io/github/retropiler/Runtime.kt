package io.github.retropiler

import javassist.ClassPool
import javassist.CtClass
import javassist.CtMethod
import javassist.Modifier
import javassist.bytecode.Descriptor
import javassist.bytecode.LocalVariableAttribute

class Runtime(val classPool: ClassPool) {
    val PACKAGE = "io.github.retropiler.runtime"
    val lambdaClassPattern = Regex(".+\\\$\\\$Lambda\\\$\\d+")
    val optionalType = classPool.get("java.util.Optional")!!
    val optionalDesc = Descriptor.of(optionalType)!!
    val retroOptionalType = classPool.get("io.github.retropiler.runtime.java.util._Optional")!!
    val retroOptionalDesc = Descriptor.of(retroOptionalType)!!
    val lambdaFactoryName = "lambdaFactory$"

    fun getRetroClassOrNull(ctClass: CtClass): CtClass? {
        val packageName = ctClass.packageName
        val simpleName = ctClass.simpleName
        return classPool.getOrNull("$PACKAGE.$packageName._$simpleName")
    }

    fun getRetroClass(ctClass: CtClass): CtClass {
        val packageName = ctClass.packageName
        val simpleName = ctClass.simpleName
        return classPool.getOrNull("$PACKAGE.$packageName._$simpleName")
    }

    fun fixup(ctClass: CtClass) {
        if (lambdaClassPattern.matches(ctClass.simpleName)) {
            fixupLambdaClass(ctClass)
        }

        fixupLocalVariableTypes(ctClass)
    }

    // retrolambda generates `Consumer lambdaFactory$(...)` so it replaces the return type
    // to retropiler runtime classes
    private fun fixupLambdaClass(lambdaClass: CtClass) {
        val retroClass = getRetroClass(lambdaClass.interfaces[0])

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

     fun cleanup(ctClass: CtClass) {
        if (!lambdaClassPattern.matches(ctClass.simpleName)) {
            return
        }

        // assume that lambdaClass implements just the single functional interface
        val retroClass = getRetroClass(ctClass.interfaces[0])

        ctClass.interfaces = ctClass.interfaces.filter {
            it == retroClass
        }.toTypedArray()

        ctClass.removeMethod(ctClass.getDeclaredMethod(lambdaFactoryName))
    }
}
