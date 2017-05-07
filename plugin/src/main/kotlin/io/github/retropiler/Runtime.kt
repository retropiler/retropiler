package io.github.retropiler

import javassist.ClassPool
import javassist.CtClass

class Runtime(val classPool: ClassPool) {
    val PACKAGE = "io.github.retropiler.runtime"

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
}
