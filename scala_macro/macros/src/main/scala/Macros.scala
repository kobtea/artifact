import scala.language.experimental.macros
import scala.collection.mutable.{ListBuffer, Stack}
import scala.reflect.macros.blackbox.Context


object Macros {

  def printf(format: String, params: Any*): Unit = macro printf_impl

  def printf_impl(c: Context)(format: c.Expr[String], params: c.Expr[Any]*): c.Expr[Unit] = {
    import c.universe._
    val Literal(Constant(s_format: String)) = format.tree

    val evals = ListBuffer[ValDef]()
    def precompute(value: Tree, tpe: Type): Ident = {
      val freshName = newTermName(c.fresh("eval$"))
      evals += ValDef(Modifiers(), freshName, TypeTree(tpe), value)
      Ident(freshName)
    }

    val paramStack = Stack[Tree](params map (_.tree): _*)
    val refs = s_format.split("(?<=%[\\w%])|(?=%[\\w%])") map {
      case "%d" => precompute(paramStack.pop, typeOf[Int])
      case "%s" => precompute(paramStack.pop, typeOf[String])
      case "%%" => Literal(Constant("%"))
      case part => Literal(Constant(part))
    }

    val stats = evals ++ refs.map(ref => reify(print(c.Expr[Any] (ref).splice)).tree)
    c.Expr[Unit](Block(stats.toList, Literal(Constant())))
  }

}
