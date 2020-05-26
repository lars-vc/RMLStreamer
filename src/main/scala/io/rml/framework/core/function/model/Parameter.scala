package io.rml.framework.core.function.model

import io.rml.framework.core.model.{Node, Uri}

import scala.util.parsing.json.JSON

/**
 * Case classes which could be useful in pattern matching empty parameter values
 */

case class EmptyParameter(paramType: Class[_], paramUri: Uri, position: Int) extends Parameter {
  override val paraValue: Option[String] = None

}

case class DefinedParameter(paramType: Class[_] = classOf[String], paramUri: Uri, paraValue: Option[String], position: Int) extends Parameter


abstract class Parameter extends Node {
  /**
   * Models the parameters used by the functions.
   *
   * `paramType` type of the parameter
   * `paramUri`  Uri representation of the parameter (as defined in functions.ttl)
   * `paraValue` [[String]] representation of the parameter
   * `position`  [[Int]] position of the parameter in the argument of the function needed for reflection
   *
   */
  val paramType: Class[_]
  val paramUri: Uri
  val paraValue: Option[String]
  val position: Int

  override def identifier: String = paramUri.toString + " " + paraValue.getOrElse("None")

  def getValue: Option[Any] = {
    getValue(paraValue.getOrElse(throw new IllegalStateException(s"${this}'s value option is empty.")))
  }

  /**
   * Get the parameter value and cast it according to the given
   * [[paramType]].
   *
   * @return value of the parameter of type specified by [[paramType]]
   */

  def getValue(paraValue: Any): Option[Any] = {
    val ScalaString = classOf[String].getName
    val IntegerString = classOf[Int].getName
    val DoubleString = classOf[Double].getName
    val ListString = classOf[List[_]].getName
    val ArrayString = classOf[Array[_]].getName
    try {
      paramType.getName match {
        case ScalaString | "java.lang.String" => Some(paraValue.toString)
        case IntegerString | "int" => Some(paraValue.toString.toInt)
        case DoubleString | "double" => Some(paraValue.toString.toDouble)
        case ListString | ArrayString | "java.util.List" =>

          val parsedListEither = JSON.parseFull(paraValue.toString).toRight("Value can't be parsed as List")

          parsedListEither match {
            case Right(parsed) =>
              parsed match {
                case value: List[_] =>
                  Some(value)
                case _ =>
                  None
              }
            case Left(exMessage) => throw new IllegalArgumentException(exMessage)
          }
        case _ => None

      }
    }catch {
      case _: Throwable => None
    }
  }


}

/**
 * OBJECT IMPLEMENTATION OF PARAMETER HERE
 */

object Parameter {
  /**
   * Implicit default ordering which will be used to order lists/sequences of [[Parameter]]
   *
   * @tparam A subclasses of [[Parameter]]
   */
  implicit def orderingByPosition[A <: Parameter]: Ordering[A] = {
    Ordering.by(elem => elem.position)
  }

  def apply(paramType: Class[_], paramUri: Uri, paraValue: String, position: Int): Parameter = {
    DefinedParameter(paramType, paramUri, Some(paraValue), position)
  }

  def apply(paramType:Class[_], paraValue:String): Parameter ={
    DefinedParameter(paramType, Uri(""),Some(paraValue), 0)
  }


  def apply(paramType: Class[_], paramUri: Uri, position: Int): Parameter = {
    EmptyParameter(paramType, paramUri, position)
  }

  def apply(paramUri: Uri, paraValue: String, position: Int): Parameter = {
    DefinedParameter(paramUri = paramUri, paraValue = Some(paraValue), position = position)
  }

}

