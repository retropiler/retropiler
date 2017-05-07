package io.github.retropiler

import io.github.retropiler.annotation.RetroMixin
import javassist.*
import javassist.bytecode.Descriptor
import javassist.expr.ExprEditor
import javassist.expr.MethodCall
import javassist.expr.NewExpr
import org.slf4j.LoggerFactory


class RetropilerExprEditor(val classPool: ClassPool) : ExprEditor() {

    val logger = LoggerFactory.getLogger(RetropilerPlugin::class.java)!!

    val signaturePattern = Regex("\\bL([a-zA-Z0-9_/\\${'$'}]+);")

    val runtime = Runtime(classPool)

    override fun edit(newExpr: NewExpr) {
        val ctr = newExpr.constructor
        val declaringClass = ctr.declaringClass
        val retroClass = runtime.getRetroClassOrNull(declaringClass) ?: return
        val signature = makeRetroSignature(ctr.signature)
        val params = makeCastedParams(signature)
        newExpr.replace("""
        ${'$'}_ = new ${retroClass.name}(${params});
        """)
    }

    override fun edit(m: MethodCall) {
        val method = m.method

        trace(m)

        // s/lambdaFactory()/_lambdaFactory()/
        if (m.methodName == "lambdaFactory$") {
            m.replace("""
                ${'$'}_ = ${m.className}._lambdaFactory${'$'}(${'$'}${'$'});
            """)
            return
        }

        val declaringClass = method.declaringClass
        val retroClass = runtime.getRetroClassOrNull(declaringClass) ?: return

        val signature = makeRetroSignature(m.signature)

        if (retroClass.hasAnnotation(RetroMixin::class.java)) { // mixin (like _Iterable)
            logger.info("RetroMixin: " + retroClass.name)
            val staticMethodSignature = makeStaticMethodSignature(signature, declaringClass)
            try {
                val retroMethod = retroClass.getMethod(m.methodName, staticMethodSignature)
                val params = makeCastedParams(signature)
                m.replace("""
                        ${retroClass.name}#${retroMethod.name}((${declaringClass.name})$0, ${params});
                    """)
            } catch (e: NotFoundException) {
                System.err.println("${e.javaClass}: ${m.methodName} ${staticMethodSignature}")
            }
        } else {
            val params = makeCastedParams(signature)
            val lhs = if (method.returnType.name != "void") {
                "${'$'}_ ="
            } else {
                ""
            }

            if (isStaticMethod(method)) {
                // e.g. invoke Optional.of(x) -> invoke _Optional.of(x)
                logger.info("replace static method: ${retroClass.name}.${m.methodName}")

                val className = StringBuilder(retroClass.name)
                if (retroClass.isInterface) {
                    className.append("$") // retrolambda companion class
                }
                m.replace("""
                        ${lhs} ${className}#${m.methodName}(${params});
                    """)
            } else {
                // e.g. invoke Optional#get() -> invoke _Optional#get()
                logger.info("replace instance method: ${retroClass.name}#${m.methodName}")

                m.replace("""
                        ${lhs} ((${retroClass.name})${'$'}0).${m.methodName}(${params});
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

    // replace "Ljava/lang/iterable;" to "Lio/github/retropiler/runtime/java/lang/$Iterable;"
    fun makeRetroSignature(signature: String): String {
        return signaturePattern.replace(signature, { matched ->
            val className = matched.groupValues[1].replace("/", ".")
            val retroClass = runtime.getRetroClassOrNull(classPool.get(className))
            if (retroClass != null) {
                Descriptor.of(retroClass.name)
            } else {
                matched.value
            }
        })
    }

    fun makeStaticMethodSignature(signature: String, receiverType: CtClass): String {
        return signature.replaceFirst("(", "(${Descriptor.of(receiverType.name)}")
    }

    fun isStaticMethod(method: CtMethod): Boolean {
        return method.modifiers.and(Modifier.STATIC) == Modifier.STATIC
    }

    fun trace(methodCall: MethodCall) {
        val method = methodCall.method

        val s = StringBuilder("> ")
        s.append(method.declaringClass.name)
        if (isStaticMethod(method)) {
            s.append(".")
        } else {
            s.append("#")
        }
        s.append(method.name)
        s.append(" ")
        s.append(method.signature)
        s.append(" ")
        s.append(methodCall.fileName)
        s.append(":")
        s.append(methodCall.lineNumber)
        logger.info(s.toString())
    }
}
