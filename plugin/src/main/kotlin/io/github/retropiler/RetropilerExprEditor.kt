package io.github.retropiler

import io.github.retropiler.annotation.RetroMixin
import javassist.*
import javassist.expr.ExprEditor
import javassist.expr.MethodCall
import javassist.expr.NewExpr


class RetropilerExprEditor(val classPool: ClassPool) : ExprEditor() {

    val retropilerRuntimePackage = "io.github.retropiler.runtime"

    val signaturePattern = Regex("\\bL([a-zA-Z0-9_/\\${'$'}]+);")

    override fun edit(newExpr: NewExpr) {
        val ctr = newExpr.constructor
        val declaringClass = ctr.declaringClass
        val retroClass = getRetroClassOrNull(declaringClass) ?: return
        val signature = makeRetroSignature(ctr.signature)
        val params = makeCastedParams(signature)
        newExpr.replace("""
        ${'$'}_ = new ${retroClass.name}(${params});
        """)
    }

    override fun edit(m: MethodCall) {
        val method = m.method;

        trace(method)

        if (m.methodName == "lambdaFactory$") {
            m.replace("""
                ${'$'}_ = ${m.className}._lambdaFactory${'$'}(${'$'}${'$'});
            """)
            return
        }

        val declaringClass = method.declaringClass
        val retroClass = getRetroClassOrNull(declaringClass) ?: return

        val signature = makeRetroSignature(m.signature)

        if (retroClass.hasAnnotation(RetroMixin::class.java)) { // mixin
            System.out.println("RetroMixin: " + retroClass.name)
            val staticMethodSignature = makeStaticMethodSignature(signature, declaringClass)
            try {
                val retroMethod = retroClass.getMethod(m.methodName, staticMethodSignature)
                val params = makeCastedParams(signature)
                m.replace("""
                        ${retroClass.name}.${retroMethod.name}((${declaringClass.name})$0, ${params});
                    """)
            } catch (e: NotFoundException) {
                System.out.println("NotFoundException: ${m.methodName} ${staticMethodSignature}")
            }
        } else { // replacement
            System.out.println("RetroReplacement: " + retroClass.name)

            if (isStatic(method)) {
                // e.g. Optional.of(x) -> $Optional.of(x)
                val params = makeCastedParams(signature)
                m.replace("""
                        ${'$'}_ = ${retroClass.name}.${m.methodName}(${params});
                    """)
            } else {
                // e.g. invoke Optional#get() -> invoke $Optional#get()
                val params = makeCastedParams(signature)
                m.replace("""
                        ${'$'}_ = ((${retroClass.name})$0).${m.methodName}(${params});
                    """)
            }
        }
    }

    fun makeCastedParams(signature: String): String {
        val paramDescriptorsToken = Regex("\\((.*)\\)").find(signature)!!.groupValues[1]
        return signaturePattern.findAll(paramDescriptorsToken).mapIndexed { i, matched ->
            "(${matched.groupValues[1].replace("/", ".")})${'$'}${i + 1}"
        }.joinToString(",")
    }

    fun getRetroClassOrNull(ctClass: CtClass): CtClass? {
        val packageName = ctClass.packageName
        val simpleName = ctClass.simpleName
        return classPool.getOrNull("$retropilerRuntimePackage.$packageName._$simpleName")
    }

    // replace "Ljava/lang/iterable;" to "Lio/github/retropiler/runtime/java/lang/$Iterable;"
    fun makeRetroSignature(signature: String): String {
        return signaturePattern.replace(signature, { matched ->
            val className = matched.groupValues[1].replace("/", ".")
            val retroClass = getRetroClassOrNull(classPool.get(className))
            if (retroClass != null) {
                "L${retroClass.name.replace(".", "/")};"
            } else {
                matched.value
            }
        })
    }

    fun makeStaticMethodSignature(signature: String, receiverType: CtClass): String {
        return signature.replaceFirst("(", "(L${receiverType.name.replace(".", "/")};")
    }

    fun isStatic(method: CtMethod): Boolean {
        return method.modifiers.and(Modifier.STATIC) == Modifier.STATIC
    }

    fun trace(method: CtMethod) {
        System.out.print(method.declaringClass.name)
        if (method.modifiers.and(Modifier.STATIC) != 0) {
            System.out.print(".")
        } else {
            System.out.print("#")
        }
        System.out.println("${method.name} ${method.signature}");
    }
}
