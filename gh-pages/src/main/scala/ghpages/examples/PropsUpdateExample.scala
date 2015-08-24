package ghpages.examples

import ghpages.examples.util.SideBySide
import japgolly.scalajs.react.extra.Reusability
import japgolly.scalajs.react.{Callback, BackendScope, ReactComponentB}
import japgolly.scalajs.react.vdom.prefix_<^._

object PropsUpdateExample {

  def content = SideBySide.Content("", "", main())

  lazy val main = addIntro(Outer, identity)
  
  case class InnerProps(onIncrement: Callback, onRefresh: Callback, i: Int)
  case class InnerBackend(p: InnerProps, $: BackendScope[_, Unit]){
    println(s"Initialized InnerBackend($p, ${$})")
    val incrementBtn = <.button("increment " + p.i.toString, ^.onClick --> p.onIncrement)
    val refreshBtn   = <.button("refresh", ^.onClick --> p.onRefresh)
  }

  implicit val r0 = Reusability.by((p: InnerProps) ⇒ p.i)

  val Inner = ReactComponentB[InnerProps]("Inner")
    .backend(InnerBackend)
    .render($ ⇒ <.div($.backend.incrementBtn, $.backend.refreshBtn))
    .configure(Reusability.shouldComponentUpdate)
    .build

  case class OuterBackend($: BackendScope[_, Int]){
    val increment = $.modState(1 +)
    val refresh   = $.modState(0 +)
  }

  val Outer = ReactComponentB[Unit]("Outer")
    .initialState(0)
    .backendNoProps(OuterBackend)
    .render($ => Inner(InnerProps($.backend.increment, $.backend.refresh, $.state)))
    .buildU

}
