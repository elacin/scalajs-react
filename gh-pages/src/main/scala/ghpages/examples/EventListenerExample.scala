package ghpages.examples

import ghpages.GhPagesMacros
import org.scalajs.dom, dom.MouseEvent
import ghpages.examples.util.SingleSide
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra._
import japgolly.scalajs.react.vdom.prefix_<^._

object EventListenerExample {

  def content = SingleSide.Content(source, Main())

  val source = GhPagesMacros.exampleSource

  // EXAMPLE:START

  val Main = ReactComponentB[Unit]("EventListener Example")
    .initialState("Local mouseenter events + local/global click events will appear here.")
    .backendNoProps(new Backend(_))
    .render($ =>
      <.pre(
        ^.border  := "solid 1px black",
        ^.width   := "90ex",
        ^.height  := "20em",
        ^.padding := "2px 6px",
        $.state)
    )
    .configure(

      // Listen to mouseenter events within the component
      EventListener[MouseEvent].install("mouseenter", _.backend.logMouseEnter),

      // Listen to click events
      EventListener.install("click", _.backend.logLocalClick),
      EventListener.install("click", _.backend.logWindowClick, _ => dom.window)

    )
    .buildU

  class Backend($: BackendScope[Nothing, String]) extends OnUnmount {
    def logEvent(desc: String)       = $.modState(_ + "\n" + desc)
    def logMouseEnter(e: MouseEvent) = logEvent(s"Mouse enter @ ${e.pageX},${e.pageY}")
    val logWindowClick               = logEvent("Window clicked.")
    val logLocalClick                = logEvent("Component clicked.")
  }

  // EXAMPLE:END
}
