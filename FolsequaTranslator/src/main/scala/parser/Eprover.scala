package eprover
import sys.process._
import java.io._


object Eprover {
  val PATH_TO_EPROVER: String = "./eprover-executable/PROVER/eprover"

  def evaluate_TPTP(tptp: String) : String = {
    val file_name = "/tmp/evaluation.tptp"

    val file = new File(file_name)
    val writer = new PrintWriter(file)
    writer.write(tptp)
    writer.close

    val result = execute(file_name)


    file.delete

    result
  }

  val eprover_options = " --auto -s --answers "

  def execute(file_name: String) : String = ((PATH_TO_EPROVER + eprover_options + file_name) lineStream_!).mkString("\n")
}
