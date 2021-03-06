package colossus.metrics


import akka.actor._
import akka.testkit._

import scala.language.higherKinds

import org.scalatest._

class NewSpec extends WordSpec with MustMatchers with BeforeAndAfterAll{

  implicit val sys = ActorSystem("test")

  def localCProbe: (LocalCollection, TestProbe) = {
    val p = TestProbe()
    implicit val a = p.ref
    (new LocalCollection, p)
  }

  override def afterAll() {
    sys.shutdown()
  }

  "LocalCollection" must {
    "create a local collection" in {
      val (c, probe) = localCProbe
      1 must equal(1)
    }

    "create a rate" in {
      val (c, probe) = localCProbe
      val r: Rate = c.getOrAdd(Rate("/foo"))
      1 must equal(1)
    }

    "create a counter" in {
      val (c, probe) = localCProbe
      val co: Counter = c.getOrAdd(Counter("/foo"))
      1 must equal(1)
    }

    "return existing collector of same name and type" in {
      val (c, probe) = localCProbe
      val r: Rate = c.getOrAdd(Rate("/foo"))
      val r2: Rate = c.getOrAdd(Rate("/foo"))
      r must equal(r2)
    }

    "throw exception on creating wrong type on address match" in {
      val (c, probe) = localCProbe
      val r: Rate = c.getOrAdd(Rate("/foo"))
      a[DuplicateMetricException] must be thrownBy {
        val co: Counter = c.getOrAdd(Counter("/foo"))
      }
    }

    "create a subcollection" in {
      val (c, probe) = localCProbe
      val sub = c.subCollection("/bar")
      val r: Rate = sub.getOrAdd(Rate("/baz"))
      r.address must equal(MetricAddress("/bar/baz"))
    }

    "uniqueness of addresses in sub collections" in {
      val (c, probe) = localCProbe
      val sub = c.subCollection("/bar")
      val o: Counter = c.getOrAdd(Counter("/bar/baz"))
      a[DuplicateMetricException] must be thrownBy {
        val r: Rate = sub.getOrAdd(Rate("/baz"))
      }

    }
      
  }

  


}

