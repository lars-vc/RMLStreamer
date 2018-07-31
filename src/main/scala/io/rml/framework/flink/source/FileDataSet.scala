package io.rml.framework.flink.source

import io.rml.framework.core.model.{LogicalSource, Uri}
import io.rml.framework.core.vocabulary.RMLVoc
import io.rml.framework.flink.item.Item
import io.rml.framework.flink.item.xml.XMLItem
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.api.scala._
import org.apache.flink.hadoopcompatibility.scala.HadoopInputs
import org.apache.hadoop.io.{LongWritable, Text}
import org.apache.mahout.text.wikipedia.XmlInputFormat

abstract class FileDataSet extends Source {
  def dataset: DataSet[Item]
}

case class XMLDataSet(dataset: DataSet[Item]) extends FileDataSet

case class JSONDataSet(dataset: DataSet[Item]) extends FileDataSet
case class CSVDataSet(dataset: DataSet[Item]) extends FileDataSet

/**
  * Object for creating Flink Datasets from a LogicalSource
  */
object FileDataSet {

  def apply(logicalSource: LogicalSource)(implicit env: ExecutionEnvironment): FileDataSet = {
    logicalSource.referenceFormulation match {
      case Uri(RMLVoc.Class.CSV) => createCSVDataSet(logicalSource.source.uri.toString)
      case Uri(RMLVoc.Class.XPATH) => createXMLWithXPathDataSet(logicalSource.source.uri.toString, logicalSource.iterator.get.value)
      case Uri(RMLVoc.Class.JSONPATH) => createJSONWithJSONPathDataSet(logicalSource.source.uri.toString, logicalSource.iterator.get.value)
    }

  }

  def createCSVDataSet(path: String)(implicit env: ExecutionEnvironment): CSVDataSet = {
    val src = scala.io.Source.fromFile(path)
    var header:Array[String] =  Array.empty
    try{
      header =  src.getLines().next().split(",")
    }finally{
      src.close()
    }

    val dataset = env.createInput(new CSVInputFormat(path,header))
    CSVDataSet(dataset)
  }

  /**
    * Not used
    *
    * @param path
    * @param tag
    * @param env
    * @return
    */
  @Deprecated
  def createXMLDataSet(path: String, tag: String)(implicit env: ExecutionEnvironment): XMLDataSet = {
    println("Creating XMLDataSet from " + path + ", with tag " + tag)
    implicit val longWritableTypeInfo: TypeInformation[LongWritable] = TypeInformation.of(classOf[LongWritable])
    implicit val textTypeInfo: TypeInformation[Text] = TypeInformation.of(classOf[Text])
    val hInput = HadoopInputs.readHadoopFile(new XmlInputFormat(), classOf[LongWritable], classOf[Text], path)
    hInput.getConfiguration.set(XmlInputFormat.START_TAG_KEY, "<" + tag.split(' ').head + ">")
    hInput.getConfiguration.set(XmlInputFormat.END_TAG_KEY, "</" + tag.split(' ').head + ">")
    val hDataset = env.createInput(hInput)
    val dataset: DataSet[Item] = hDataset.map(item => {
      XMLItem.fromString(item._2.toString).asInstanceOf[Item]
    }) // needed since types of datasets can't be subclasses due to Flink implementation
    XMLDataSet(dataset)
  }

  def createXMLWithXPathDataSet(path: String, xpath: String)(implicit env: ExecutionEnvironment): XMLDataSet = {
    println("Creating XMLDataSet with XPath from " + path + ", with xpath " + xpath)
    val dataset = env.createInput(new XMLInputFormat(path, xpath))
    XMLDataSet(dataset)
  }

  def createJSONWithJSONPathDataSet(path: String, jsonPath: String)(implicit env: ExecutionEnvironment): JSONDataSet = {
    println("Creating JSONDataSet from " + path + ", with JsonPath " + jsonPath)
    val dataset = env.createInput(new JSONInputFormat(path, jsonPath))
    JSONDataSet(dataset)
  }


}