package ghpages.examples

import ghpages.examples.util.SideBySide
import japgolly.scalajs.react.extra._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

object PropsUpdateExample {

  def content = SideBySide.Content("", "", main())

  lazy val main = addIntro(Outer, identity)

  case class InnerProps(
    printer:     Int ~=> Callback,
    onIncrement: Callback, 
    onRefresh:   Callback, 
    value:       Int
  )
  
  case class InnerBackend(p: InnerProps, $: BackendScope[Nothing, Unit]){
    println(s"Initialized InnerBackend($p, ${$})")

    val incrementBtn = <.button(
      "increment " + p.value.toString,
      ^.onClick --> (p.onIncrement >> p.printer(p.value))
    )

    val refreshBtn = <.button(
      "refresh",
      ^.onClick --> p.onRefresh
    )
  }

  implicit val r0 = Reusability.by((p: InnerProps) ⇒ p.value)

  val Inner = ReactComponentB[InnerProps]("Inner")
    .backend(InnerBackend)
    .render($ ⇒ <.div($.backend.incrementBtn, $.backend.refreshBtn))
    .configure(Reusability.shouldComponentUpdate)
    .build
  
  case class OuterBackend($: BackendScope[Nothing, Int]){
    val increment = $.modState(1 +)
    val refresh   = $.modState(0 +)
  }

  object OuterStatic{
    val print: Int ~=> Callback = 
      ReusableFn(i ⇒ Callback(println(i)))
  }
  
  val Outer = ReactComponentB[Unit]("Outer")
    .initialState(0)
    .backendNoProps(OuterBackend)
    .render($ => Inner(InnerProps(OuterStatic.print, $.backend.increment, $.backend.refresh, $.state)))
    .buildU

}
