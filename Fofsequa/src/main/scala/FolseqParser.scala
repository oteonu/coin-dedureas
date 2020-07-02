package org.nanquanu.fofsequa

import scala.util.parsing.combinator._

/*
 Parser
*/
object FolseqParser extends RegexParsers {
  // TODO: Numbers aren't supported yet

  // <fofsequa_document> ::= <fofsequa_statement>+
  def fofsequa_document: Parser[List[Statement]] = statement ~ ((";" ~ statement) ^^ {case _ ~ stmt => stmt}).* ^^ {
    case first_statement ~ statement_list => first_statement :: statement_list
  }

  // TODO: Currently, each statement with a binaryConnective has to be surrounded by brackets, this shouldn't be needed and should change in the future
  //<fofsequa_statement> ::= <unary_connective> <fofsequa_statement> | (<fofsequa_statement> <binary connective> <fofsequa_statement) | <quantified_formula> | <fol_atom>
  def statement : Parser[Statement] =
    unary_connective ~ statement ^^ { case connective ~ stmt => UnaryConnectiveStatement(connective, stmt) } |
    "(" ~ statement ~ binary_connective ~ statement ~ ")" ^^ { case _ ~ statement1 ~ connective ~ statement2 ~ _ => BinaryConnectiveStatement(statement1, connective, statement2) } |
    quantified_formula |
    fol_atom

  // <unary_connective> ::= not
  def unary_connective = "not" ^^ { (x) => Not() }

  // <binary_connective> ::= and | or | => | <=>
  def binary_connective : Parser[BinaryConnective] = "and"  ^^ { (x) => And() } |
    "or" ^^ { (x) => Or() } |
    "=>"  ^^ { (x) => IfThen()} |
    "<=>" ^^ { (x) => Iff() }

  // <fol_atom> ::= <fol_predicate>(<fol_term_list>) | <fol_term> = <fol_term>
  def fol_atom : Parser[AtomStatement] = fol_predicate ~ "(" ~ fol_term_list ~ ")" ^^ { case predicate ~ _ ~ termList ~ _ => AtomStatement(predicate, termList) }

  // <fol_predicate> ::= <uppercase_id> | <predifined_predicate>
  def fol_predicate = uppercase_ID ^^ { FolPredicate(_) }

  // sequence of alphabetical characters, starting with upper case
  def uppercase_ID = "[A-Z]([a-zA-Z]|_)*".r ^^ { UppercaseID(_) }

  // <fol_term_list> ::= <fol_term> | <fol_term_list>, <fol_term_list>
  def fol_term_list : Parser[List[FolTerm]] = fol_term ~ ", *".r ~ fol_term_list ^^ { case term ~ _ ~ termList => List(term) ++ termList } | fol_term ^^ { List(_) }

  // <fol_term> ::= <var> | <constant> | <fol_function>(<fol_term_list>)
  def fol_term =
    variable ^^ { VariableTerm(_) } |
    constant ^^ { ConstantTerm(_) } |
    fol_function ~ "(" ~ fol_term_list ~ ")" ^^ { case function ~ _ ~ termList ~ _  => FunctionApplication(function, termList) }

  // <var> ::= <lowercase_id>
  def variable = lowercase_ID ^^ { Variable(_) }

  // sequence of alphabetical characters, starting with lower case
  def lowercase_ID = "[a-z]([a-zA-Z]|_)*".r ^^ { LowercaseID(_) }

  // <constant> ::= '<lowercase_id>' | <decimal_integer>
  def constant = "\'" ~ lowercase_ID ~ "\'" ^^ { case _ ~ name ~ _ => Constant(name) }

  // <fol_function> ::= <lowercase_id>
  def fol_function = lowercase_ID ^^ { FolFunction(_) }

  // <quantified_formula> ::= <quantifier>[<quantifier_arguments>]: <fofsequa_statement>
  def quantified_formula = quantifier ~ "[" ~ quantifier_arguments ~ "]:" ~ statement ^^ { case quant ~ _ ~ quantArguments ~ _ ~ stmt => QuantifiedStatement(quant, quantArguments, stmt) }

  // <quantifier> ::= ! | ?
  def quantifier = "!" ^^ {(x) => ForAll()} | "?" ^^ {x => Exists()}

  // <quantifier_arguments> ::= <fol_variable_list> | <var> from <constant_set>
  def quantifier_arguments = variable_list ~ "from" ~ constantSet ^^ {case v ~ _ ~ constant_set => ConstantSetQuantifierArguments(v, constant_set)} |
    variable_list ^^ { BasicQuantifierArguments(_) }

  // <fol_variable_list> ::= <var> | <fol_variable_list>, <fol_variable_list>
  def variable_list: Parser[List[Variable]] = variable ~ ("," ~ variable ^^ {case _comma ~ varname => varname}).* ^^ {case first_var ~ var_list => first_var :: var_list}

  // <constant_set> ::= {<constant_set_elements>} | <pattern_var>
  def constantSet = "{" ~ constantSetElements ~ "}" ^^ {case _ ~ elements ~ _ => BasicConstantSet(elements)} | patternVar

  // <constant_set_elements> ::= <constant> | <constant>, <constant_set_elements>
  def constantSetElements = constant_tuple.* // constant ^^ { List(_) } | constant ~ "," ~ constantSetElements ^^ { case const ~ _ ~ constSetElements => List(const) ++ constSetElements }

  // <constant_tuple> ::= <constant> | "<"<constant>(, <constant>)*">"
  def constant_tuple = constant ^^ {const => ConstantTuple(List(const))} | ("<" ~ constant ~ ("," ~ constant ^^ {case _ ~ const => const}).* ~ ">") ^^ {case _ ~ first_constant ~ constant_list ~ _ => ConstantTuple(first_constant :: constant_list) }

  // <pattern_var> ::= <lowercase_id>_
  def patternVar =  lowercase_ID ~ "_" ^^ { case id ~ _ => PatternVar(id) }
}

/*
  Data structures
*/

sealed abstract class Statement
case class BinaryConnectiveStatement(pre_statement: Statement, connective: BinaryConnective, post_statement: Statement) extends Statement {
  override def toString: String = pre_statement.toString + connective.toString + post_statement.toString
}
case class UnaryConnectiveStatement(connective: UnaryConnective, post_statement: Statement) extends Statement {
  override def toString: String = connective.toString + post_statement.toString
}
case class QuantifiedStatement(quantifier: Quantifier, arguments: QuantifierArguments, statement: Statement) extends Statement{
  override def toString: String = quantifier.toString + "[" + arguments.toString + "]:" + statement.toString
}

case class AtomStatement(predicate: FolPredicate, terms: Seq[FolTerm]) extends Statement{
  override def toString: String = predicate.toString + terms.map(_.toString).mkString("(", ",", ")")
}

sealed abstract class Quantifier {
  def inverse(): Quantifier
}
case class ForAll() extends Quantifier {
  override def toString: String = "!"

  override def inverse(): Quantifier = Exists()
}
case class Exists() extends Quantifier {
  override def toString: String = "?"

  override def inverse(): Quantifier = ForAll()
}

sealed abstract class QuantifierArguments
case class ConstantSetQuantifierArguments(variables: Seq[Variable], constant_set: ConstantSet) extends QuantifierArguments {
  override def toString: String = (
      if(variables.length == 1) variables.head.toString
      else variables.mkString(",")
    ) + " from " + constant_set.toString
}
case class BasicQuantifierArguments(variables: Seq[Variable]) extends QuantifierArguments {
  override def toString: String = variables.map(_.toString).mkString(",")
}

sealed abstract class ConstantSet
case class BasicConstantSet(constants: Seq[ConstantTuple]) extends ConstantSet {
  override def toString: String = constants.map(_.toString).mkString("{", ",", "}")
}
case class PatternVar(name: LowercaseID) extends ConstantSet {
  override def toString: String = name.toString + "_"
}

case class ConstantTuple(constants: Seq[Constant]) {
  override def toString(): String = if(constants.length == 1) {
    constants.head.toString()
  }
  else {
    constants.map(_.toString).mkString("<", ",", ">")
  }
}

case class Constant(id: LowercaseID)  {
  override def toString: String = "'" + id.toString + "'"
}
case class Variable(id: LowercaseID) {
  override def toString: String = id.toString
}
case class LowercaseID(name: String) {
  override def toString: String = name
}
case class UppercaseID(name: String) {
  override def toString: String = name
}

sealed abstract class UnaryConnective
case class Not() extends UnaryConnective {
  override def toString: String = "not"
}

sealed abstract class BinaryConnective
case class And() extends BinaryConnective {
  override def toString: String = "&"
}
case class Or() extends BinaryConnective {
  override def toString: String = "|"
}
case class IfThen() extends BinaryConnective {
  override def toString: String = "=>"
}
case class Iff() extends BinaryConnective {
  override def toString: String = "<=>"
}

case class FolPredicate(name: UppercaseID) {
  override def toString: String = name.toString
}

sealed abstract class FolTerm()
case class FunctionApplication(function: FolFunction, terms: Seq[FolTerm]) extends FolTerm {
  override def toString: String = function.toString + terms.mkString("(", ",", ")")
}
case class ConstantTerm(constant: Constant) extends FolTerm {
  override def toString: String = constant.toString
}
case class VariableTerm(variable: Variable) extends FolTerm {
  override def toString: String = variable.toString
}

case class FolFunction(name: LowercaseID) {
  override def toString: String = name.toString
}
