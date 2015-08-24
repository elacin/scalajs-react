package ghpages.examples

import ghpages.examples.util.SideBySide
import japgolly.scalajs.react.{Callback, BackendScope, ReactComponentB}
import japgolly.scalajs.react.vdom.prefix_<^._

object PropsUpdateExample {

  def content = SideBySide.Content("", "", main())

  lazy val main = addIntro(Outer, identity)
  
  case class InnerProps(onClick: Callback, i: Int)
  case class InnerBackend(p: InnerProps, $: BackendScope[_, Unit]){
    println(s"Initialized InnerBackend($p, ${$})")
    val button = <.button("increment " + p.i.toString, ^.onClick --> p.onClick)
  }

  val Inner = ReactComponentB[InnerProps]("Inner")
    .backend(InnerBackend)
    .render(_.backend.button)
    .build

  val Outer = ReactComponentB[Unit]("Outer")
    .initialState(0)
    .render($ => Inner(InnerProps($.modState(1 +), $.state)))
    .buildU

}
