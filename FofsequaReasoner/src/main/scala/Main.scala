package org.nanquanu.fofsequa_reasoner
import eprover._
import org.nanquanu.fofsequa._

import scala.io.{Source, StdIn}

object Main {
  def main(args: Array[String]): Unit = {
    test.Test.test()
    // console_interface(args)
  }

  def console_interface(args: Array[String]): Unit = {
    for(arg <- args) {
      println(arg)
    }

    // evaluate_file("./test_fofsequa_kb.txt", read_input_query)
    test.Test.test()
  }

  def read_input_query: String = {
    var query = StringBuilder.newBuilder
    do {
      query.append(StdIn.readLine())
    }
    while(query.charAt(query.length - 1) != ';')

    query.deleteCharAt(query.length - 1)

    query.toString
  }

  def evaluate_file(file_path: String, query: String): Boolean = {
    val file = Source.fromFile(file_path)
    val lines = try file.getLines() mkString "\n" finally file.close()

    evaluate_fofsequa_to_string(lines, query) match {
      case Some(answer) => {
        println(answer)
        true
      }
      case None => false
    }
  }

  def evaluate_fofsequa(knowledge_base: String, goal: String): Option[Statement] = {
    val parsed_knowledge_base = FolseqParser.parseAll(FolseqParser.fofsequa_document, knowledge_base) match {
      case FolseqParser.Success(result, next) => result
      case FolseqParser.Error(message, next) => {
        println(message)
        return None
      }
      case FolseqParser.Failure(message, next) => {
        println(message)
        return None
      }
      case FolseqParser.NoSuccess(message, next) => {
        println(message)
        return None
      }
      case _ => {
        println("parsing knowledge base not succesfull")
        return None
      }
    }

    val parsed_goal = FolseqParser.parseAll(FolseqParser.statement, goal) match {
      case FolseqParser.Success(result, next) => result
      case FolseqParser.Error(message, next) => {
        println(message)
        return None
      }
      case FolseqParser.Failure(message, next) => {
        println(message)
        return None
      }
      case FolseqParser.NoSuccess(message, next) => {
        println(message)
        return None
      }
      case _ => {
        println("parsing goal not succesful")
        return None
      }
    }

    val eprover_answer = Eprover.evaluate_TPTP(FofsequaToFof.to_tptp(parsed_knowledge_base, parsed_goal))
    println(eprover_answer)
    // TODO: support constant tuples
    val answer_constants = Eprover.get_answer_tuples(eprover_answer).map(answers => {
      val answers_as_constants = answers.map(
        constant_name => Constant(LowercaseID(constant_name.toLowerCase()))
      )
      ConstantTuple(answers_as_constants)
    })
    println("parsed goal:")
    println(parsed_goal)
    println("-------")
    // turn eprover's answer into a statement into the answer lang
    val substituted = parsed_goal match {
      case QuantifiedStatement(quantifier, arguments, statement) => arguments match {
        case ConstantSetQuantifierArguments(variables, constant_set) => constant_set match {
          case PatternVar(name) => {
            println("variables_length = " + variables.length)
            QuantifiedStatement(quantifier, ConstantSetQuantifierArguments(variables, BasicConstantSet(answer_constants)), statement)
          }
          case _ => return None
        }
        case _ => return None
      }
      case _ => return None
    }

    Some(substituted)
  }

  def evaluate_fofsequa_to_string(knowledge_base: String, goal: String): Option[String] = evaluate_fofsequa(knowledge_base, goal).map(_.toString())
}
