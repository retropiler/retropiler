package io.github.retropiler

import javassist.CannotCompileException
import javassist.ClassPool
import javassist.CtMethod
import javassist.Modifier
import javassist.expr.ExprEditor
import javassist.expr.MethodCall


class RetropilerExprEditor(val classPool: ClassPool) : ExprEditor() {

    val iterable = "io.github.retropiler.runtime.JavaUtilIterable"
    val consumer = "io.github.retropiler.runtime.JavaUtilFunctionConsumer"

    @Throws(CannotCompileException::class)
    override fun edit(m: MethodCall) {
        trace(m.method)

        if (m.method.name == "lambdaFactory$") {
            m.replace("""
                ${'$'}_ = ${'$'}0.lambdaFactory${'$'}(${'$'}${'$'});
            """)
        } else if (m.className == "java.util.List" && m.methodName == "forEach") {
            m.replace("""
                $iterable.forEach($0, ($consumer)$1);
            """)
        }
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
