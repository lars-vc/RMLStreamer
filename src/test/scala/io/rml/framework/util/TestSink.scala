package io.rml.framework.util


import org.apache.flink.streaming.api.functions.sink.SinkFunction

/**
  * This object will collect the output of rml generation from a data stream into
  * list of string. The list will be synchronized across the threads.
  */
object TestSink {

  private val lock = AnyRef

  private var triples: List[String] = List[String]()


  def apply(): TestSink = new TestSink()


  def getTriples: List[String] = lock.synchronized {
    triples
  }


  def empty(): Unit = lock.synchronized {
    triples = List()
  }


}

class TestSink extends SinkFunction[String] {
  override def invoke(value: String): Unit = {

    synchronized {
      for (el <- value.split('\n')) {
        TestSink.lock.synchronized {
          // List in scala is linked list so prepending is faster
          TestSink.triples = el :: TestSink.triples
        }
      }
    }
  }

}