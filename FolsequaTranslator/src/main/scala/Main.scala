import parser.FolseqParser
import eprover._
import sys.process._

object Main {
  def main(args: Array[String]): Unit = {
    println("Hello World")

    println(FolseqParser.parse(FolseqParser.fofsequa_document, """P("a") and P("b")"""))
    println(FolseqParser.parse(FolseqParser.quantifiedFormula, """![a from p_]: P(a, "b")"""))

    //val result = Eprover.execute("/home/jcmaas/Documents/coin-dedureas/example_db.tptp")

    //println(result)

    println(Eprover.evaluate_TPTP("""fof(goal, conjecture, livesIn("Marit", "Norway"))."""))
  }
}
