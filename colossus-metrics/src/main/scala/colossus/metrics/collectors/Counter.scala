package colossus.metrics

import akka.actor._

trait Counter extends EventCollector {
  def Δ(Δ: Long, tags: TagMap = TagMap.Empty) //HURR DURR LOOK AT MY FANCY METHOD NAME
  def delta(amount: Long, tags: TagMap = TagMap.Empty) {
    Δ(amount, tags)
  }
  def increment(tags: TagMap = TagMap.Empty) {
    Δ(1, tags)
  }
  def decrement(tags: TagMap = TagMap.Empty) {
    Δ(-1, tags)
  }
}
object Counter {
  
  case class Delta(address: MetricAddress, d: Long, tags: TagMap) extends MetricEvent

  def apply(address: MetricAddress) = CounterParams(address)

  implicit object LocalCounterGenerator extends Generator[LocalLocality, Counter, CounterParams] {
    def apply(params: CounterParams)(implicit collector: ActorRef) = new LocalCounter(params, collector)
  }

}

case class CounterParams(address: MetricAddress) extends MetricParams[Counter, CounterParams] {
  type E = Counter
  def transformAddress(f: MetricAddress => MetricAddress) = copy(address = f(address))
}

class BasicCounter(params: CounterParams) {
  var num: Long = 0
  protected val counters = collection.mutable.Map[TagMap, Long]()

  def Δ(Δ: Long, tags: TagMap = TagMap.Empty) {
    if (!counters.contains(tags)) {
      counters(tags) = Δ
    } else {
      counters(tags) += Δ
    }
  }
}

class LocalCounter(params: CounterParams, collector: ActorRef) extends BasicCounter(params) with Counter with LocalLocality[Counter] {
  import Counter._
  lazy val shared = new SharedCounter(params, collector)

  val address = params.address

  def metrics(context: CollectionContext): MetricMap = Map(
    params.address -> counters.toMap.map{case (tags, value) => (tags ++ context.globalTags, value)}
  )

  def event = {
    case Delta(_, d, t) => Δ(d, t)
  }

}

class SharedCounter(params: CounterParams, collector: ActorRef) extends Counter with SharedLocality[Counter] {
  def address = params.address
  def Δ(d: Long, tags: TagMap = TagMap.Empty) {
    collector ! Counter.Delta(params.address, d, tags)
  }
}

