package ghpages

import org.scalajs.dom
import scala.scalajs.js.JSApp
import scala.scalajs.js.annotation.JSExport
import japgolly.scalajs.react._, vdom.prefix_<^._, ScalazReact._
import japgolly.scalajs.react.extra.router2._
import pages._

object ReactApp extends JSApp {

  sealed trait Page
  case object Home                 extends Page
  case class Examples(eg: Example) extends Page
  case object Doco                 extends Page

  val routerConfig = RouterConfig.build[Page] { dsl =>
    import dsl._

    def exampleRoute(e: Example): Rule =
      staticRoute("#example" / e.routerPath, Examples(e)) ~>
        renderR(r => ExamplesPage.component(ExamplesPage.Props(e, r contramap Examples)))

    def exampleRoutes: Rule =
      Example.values.map(exampleRoute).reduce(_ | _)

    (removeTrailingSlashes
    | staticRoute(root,   Home) ~> render(HomePage.component())
    | staticRoute("#doc", Doco) ~> render(DocoPage.component())
    | exampleRoutes
    )
      .notFound(redirectToPage(Home)(Redirect.Replace))
      .renderWith(layout)
      .verify(Home, Examples(Example.Hello), Examples(Example.Todo), Doco)
      .logToConsole
  }

  def layout(c: RouterCtl[Page], r: Resolution[Page]) =
    <.div(
      navMenu(c),
      <.div(^.cls := "container", r.render()))

  lazy val navMenu = ReactComponentB[RouterCtl[Page]]("Menu")
    .render { ctl =>
      def nav(name: String, target: Page) =
        <.li(
          ^.cls := "navbar-brand active",
          ctl setOnClick target,
          name)
      <.div(
        ^.cls := "navbar navbar-default",
        <.ul(
          ^.cls := "navbar-header",
          nav("Home",          Home),
          nav("Examples",      Examples(Example.default)),
          nav("Documentation", Doco)))
    }.build

  val router = Router(BaseUrl.fromWindowOrigin, routerConfig)

  @JSExport
  override def main(): Unit =
    router() render dom.document.body
}
