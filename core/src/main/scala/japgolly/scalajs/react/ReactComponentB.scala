package japgolly.scalajs.react

import scala.scalajs.js.{Any => JAny, Array => JArray, _}
import Internal._
import ComponentScope._

object ReactComponentB {

  @inline def apply[Props](name: String) = new P[Props](name)

  // ======
  // Stages
  // ======
  //
  // 1 = P
  // 2 = PS
  // 3 = PSB
  //     .render() mandatory
  // 4 = PSBR
  // 5 = ReactComponentB
  // 6 = ReactComponentB#Builder

  implicit def defaultStateless    [p](x: P[p])                                           = x.stateless
  implicit def defaultNoBackend    [p,s,X <% PS[p, s]](x: X)                              = x.noBackend
  implicit def defaultTopNode      [p,s,b](x: PSBR[p, s, b])                              = x.domType[TopNode]
  implicit def defaultPropsRequired[P,S,B,N<:TopNode,X <% ReactComponentB[P,S,B,N]](x: X) = x.propsRequired

  type InitStateFn[P, S] = DuringCallbackU[P, S, Any] => CallbackTo[S]
  type InitBackendFn[P, S, B] = (P, BackendScope[Nothing, S]) => B
  type RenderFn[P, S, -B] = DuringCallbackU[P, S, B] => ReactElement

  // ===================================================================================================================
  // Convenience

  /**
   * Create a component that always displays the same content, never needs to be redrawn, never needs vdom diffing.
   */
  def static(name: String, content: ReactElement) =
    ReactComponentB[Unit](name)
      .render(_ => content)
      .shouldComponentUpdateConstCB(alwaysFalse)

  private val alwaysFalse = CallbackTo.pure(false)

  // ===================================================================================================================
  final class P[Props] private[ReactComponentB](name: String) {

    // getInitialState is how it's named in React
    def getInitialState  [State](f: DuringCallbackU[Props, State, Any] => State)             = getInitialStateCB[State]($ => CallbackTo(f($)))
    def getInitialStateCB[State](f: DuringCallbackU[Props, State, Any] => CallbackTo[State]) = new PS[Props, State](name, f)

    // More convenient methods that don't need the full ComponentScope
    def initialState    [State](s: => State                  ) = initialStateCB(CallbackTo(s))
    def initialStateCB  [State](s: CallbackTo[State]         ) = getInitialStateCB[State](_ => s)
    def initialState_P  [State](f: Props => State            ) = getInitialStateCB[State]($ => CallbackTo(f($.props)))
    def initialStateCB_P[State](f: Props => CallbackTo[State]) = getInitialStateCB[State]($ => f($.props))

    def stateless = initialStateCB(Callback.empty)
  }

  // ===================================================================================================================
  final class PS[P, S] private[ReactComponentB](name: String, initF: InitStateFn[P, S]) {
    def backend[B] (f: InitBackendFn[P, S, B]) = new PSB(name, initF, f)
    def noBackend                              = backend((_, _) => ())
    def backendNoProps[B](f: BackendScope[Nothing, S] ⇒ B) =
      backend((p: P, $: BackendScope[Nothing, S]) ⇒ f($))
  }

  // ===================================================================================================================
  final class PSB[P, S, B] private[ReactComponentB](name: String, initF: InitStateFn[P, S], backF: InitBackendFn[P, S, B]) {

    def render(f: DuringCallbackU[P, S, B] => ReactElement): PSBR[P, S, B] =
      new PSBR(name, initF, backF, f)

    def renderPCS(f: (DuringCallbackU[P, S, B], P, PropsChildren, S) => ReactElement): PSBR[P, S, B] =
      render($ => f($, $.props, $.propsChildren, $.state))

    def renderPC(f: (DuringCallbackU[P, S, B], P, PropsChildren) => ReactElement): PSBR[P, S, B] =
      render($ => f($, $.props, $.propsChildren))

    def renderPS(f: (DuringCallbackU[P, S, B], P, S) => ReactElement): PSBR[P, S, B] =
      render($ => f($, $.props, $.state))

    def renderP(f: (DuringCallbackU[P, S, B], P) => ReactElement): PSBR[P, S, B] =
      render($ => f($, $.props))

    def renderCS(f: (DuringCallbackU[P, S, B], PropsChildren, S) => ReactElement): PSBR[P, S, B] =
      render($ => f($, $.propsChildren, $.state))

    def renderC(f: (DuringCallbackU[P, S, B], PropsChildren) => ReactElement): PSBR[P, S, B] =
      render($ => f($, $.propsChildren))

    def renderS(f: (DuringCallbackU[P, S, B], S) => ReactElement): PSBR[P, S, B] =
      render($ => f($, $.state))

    def render_P(f: P => ReactElement): PSBR[P, S, B] =
      render($ => f($.props))

    def render_C(f: PropsChildren => ReactElement): PSBR[P, S, B] =
      render($ => f($.propsChildren))

    def render_S(f: S => ReactElement): PSBR[P, S, B] =
      render($ => f($.state))
  }

  // ===================================================================================================================
  final class PSBR[P, S, B] private[ReactComponentB](name: String, initF: InitStateFn[P, S], backF: InitBackendFn[P, S, B], rendF: RenderFn[P, S, B]) {
    def domType[N <: TopNode]: ReactComponentB[P, S, B, N] =
      new ReactComponentB(name, initF, backF, rendF, emptyLifeCycle, Vector.empty)
  }

  // ===================================================================================================================
  private[react] case class LifeCycle[P,S,B,N <: TopNode](
    configureSpec            : UndefOr[ReactComponentSpec[P, S, B, N]       => Callback],
    getDefaultProps          : UndefOr[                                        CallbackTo[P]],
    componentWillMount       : UndefOr[DuringCallbackU[P, S, B]             => Callback],
    componentDidMount        : UndefOr[DuringCallbackM[P, S, B, N]          => Callback],
    componentWillUnmount     : UndefOr[DuringCallbackM[P, S, B, N]          => Callback],
    componentWillUpdate      : UndefOr[(WillUpdate[P, S, B, N], P, S)       => Callback],
    componentDidUpdate       : UndefOr[(DuringCallbackM[P, S, B, N], P, S)  => Callback],
    componentWillReceiveProps: UndefOr[(DuringCallbackM[P, S, B, N], P)     => Callback],
    shouldComponentUpdate    : UndefOr[(DuringCallbackM[P, S, B, N], P, S)  => CallbackB])

  private[react] def emptyLifeCycle[P,S,B,N <: TopNode] =
    LifeCycle[P,S,B,N](undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined)
}

import ReactComponentB.{InitStateFn, InitBackendFn, RenderFn, LifeCycle}

final class ReactComponentB[P,S,B,N <: TopNode](val name: String,
                                                initF   : InitStateFn[P, S],
                                                backF   : InitBackendFn[P, S, B],
                                                rendF   : RenderFn[P, S, B],
                                                lc      : LifeCycle[P, S, B, N],
                                                jsMixins: Vector[JAny]) {

  @inline private def copy(name    : String                  = name,
                           initF   : InitStateFn[P, S]       = initF,
                           backF   : InitBackendFn[P, S, B]  = backF,
                           rendF   : RenderFn[P, S, B]       = rendF,
                           lc      : LifeCycle[P, S, B, N]   = lc,
                           jsMixins: Vector[JAny]            = jsMixins): ReactComponentB[P, S, B, N] =
    new ReactComponentB(name, initF, backF, rendF, lc, jsMixins)

  @inline private implicit def lcmod(a: LifeCycle[P, S, B, N]): ReactComponentB[P, S, B, N] =
    copy(lc = a)

  def configureSpec(modify: ReactComponentSpec[P, S, B, N] => Callback): ReactComponentB[P, S, B, N] =
       lc.copy(configureSpec = modify)

  def configure(fs: (ReactComponentB[P, S, B, N] => ReactComponentB[P, S, B, N])*): ReactComponentB[P, S, B, N] =
    fs.foldLeft(this)((a,f) => f(a))


  def getDefaultProps(p: => P): ReactComponentB[P, S, B, N] =
    getDefaultPropsCB(CallbackTo(p))

  def getDefaultPropsCB(p: CallbackTo[P]): ReactComponentB[P, S, B, N] =
    lc.copy(getDefaultProps = p)


  def componentWillMount(f: DuringCallbackU[P, S, B] => Callback): ReactComponentB[P, S, B, N] =
    lc.copy(componentWillMount = fcUnit(lc.componentWillMount, f))

  def componentDidMount(f: DuringCallbackM[P, S, B, N] => Callback): ReactComponentB[P, S, B, N] =
    lc.copy(componentDidMount = fcUnit(lc.componentDidMount, f))

  def componentWillUnmount(f: DuringCallbackM[P, S, B, N] => Callback): ReactComponentB[P, S, B, N] =
    lc.copy(componentWillUnmount = fcUnit(lc.componentWillUnmount, f))

  def componentWillUpdate(f: (WillUpdate[P, S, B, N], P, S) => Callback): ReactComponentB[P, S, B, N] =
    lc.copy(componentWillUpdate = fcUnit(lc.componentWillUpdate, f))

  def componentDidUpdate(f: (DuringCallbackM[P, S, B, N], P, S) => Callback): ReactComponentB[P, S, B, N] =
    lc.copy(componentDidUpdate = fcUnit(lc.componentDidUpdate, f))

  def componentWillReceiveProps(f: (DuringCallbackM[P, S, B, N], P) => Callback): ReactComponentB[P, S, B, N] =
    lc.copy(componentWillReceiveProps = fcUnit(lc.componentWillReceiveProps, f))

  def componentWillMountCB(cb: Callback): ReactComponentB[P, S, B, N] =
      componentWillMount(_ => cb)

  def componentDidMountCB(cb: Callback): ReactComponentB[P, S, B, N] =
      componentDidMount(_ => cb)

  def componentWillUnmountCB(cb: Callback): ReactComponentB[P, S, B, N] =
      componentWillUnmount(_ => cb)

  def componentWillUpdateCB(cb: Callback): ReactComponentB[P, S, B, N] =
      componentWillUpdate((_, _, _) => cb)

  def componentDidUpdateCB(cb: Callback): ReactComponentB[P, S, B, N] =
      componentDidUpdate((_, _, _) => cb)

  def componentWillReceivePropsCB(cb: Callback): ReactComponentB[P, S, B, N] =
      componentWillReceiveProps((_, _) => cb)


  def shouldComponentUpdate(f: (DuringCallbackM[P, S, B, N], P, S) => Boolean): ReactComponentB[P, S, B, N] =
    shouldComponentUpdateCB(($, p, s) => CallbackTo(f($, p, s)))

  def shouldComponentUpdateCB(f: (DuringCallbackM[P, S, B, N], P, S) => CallbackB): ReactComponentB[P, S, B, N] =
    lc.copy(shouldComponentUpdate = fcEither(lc.shouldComponentUpdate, f))

  def shouldComponentUpdateConst(f: => Boolean): ReactComponentB[P, S, B, N] =
    shouldComponentUpdateConstCB(CallbackTo(f))

  def shouldComponentUpdateConstCB(f: CallbackB): ReactComponentB[P, S, B, N] =
    shouldComponentUpdateCB((_, _, _) => f)

  /**
   * Install a pure-JS React mixin.
   *
   * Beware: There will be mixins that won't work correctly as they make assumptions that don't hold for Scala.
   * If a mixin expects to inspect your props or state, forget about it; Scala-land owns that data.
   */
  def mixinJS(mixins: JAny*): ReactComponentB[P, S, B, N] =
    copy(jsMixins = jsMixins ++ mixins)

  /**
   * Modify the render function.
   */
  def reRender(f: RenderFn[P, S, B] => RenderFn[P, S, B]): ReactComponentB[P, S, B, N] =
    copy(rendF = f(rendF))

  // ===================================================================================================================
  @inline private def builder[C](cc: ReactComponentCU[P,S,B,N] => C) = new Builder(cc)

  def propsRequired         = builder(new ReactComponentC.ReqProps    [P,S,B,N](_, undefined, undefined))
  def propsDefault(p: => P) = builder(new ReactComponentC.DefaultProps[P,S,B,N](_, undefined, undefined, () => p))
  def propsConst  (p: => P) = builder(new ReactComponentC.ConstProps  [P,S,B,N](_, undefined, undefined, () => p))

  def propsUnit(implicit ev: Unit =:= P) = propsConst(ev(()))
  def buildU   (implicit ev: Unit =:= P) = propsUnit.build

  final class Builder[C] private[ReactComponentB](cc: ReactComponentCU[P,S,B,N] => C) {

    def buildSpec: ReactComponentSpec[P, S, B, N] = {
      val spec = Dynamic.literal(
        "displayName" -> name,
        "backend" -> 0,
        "render" -> (rendF: ThisFunction)
      )

      @inline def setFnPS[T, R](fn: UndefOr[(T, P, S) => CallbackTo[R]], name: String): Unit =
        fn.foreach { f =>
          val g = (t: T, p: WrapObj[P], s: WrapObj[S]) => f(t, p.v, s.v).runNow()
          spec.updateDynamic(name)(g: ThisFunction)
        }

      @inline def setThisFn1[A](fn: UndefOr[A => Callback], name: String): Unit =
        fn.foreach { f =>
          val g = (a: A) => f(a).runNow()
          spec.updateDynamic(name)(g: ThisFunction)
        }

      def updateBackend(t: AnyUnmounted[P, S, B], newProps: P) = {
        val scopeB = t.asInstanceOf[BackendScope[Nothing, S]]
        t.asInstanceOf[Dynamic].updateDynamic("backend")(backF(newProps, scopeB).asInstanceOf[JAny])
      }

      val componentWillMount2 = (t: DuringCallbackU[P, S, B]) => {
        updateBackend(t, t.props)
        lc.componentWillMount.foreach(g => g(t).runNow())
      }

      spec.updateDynamic("componentWillMount")(componentWillMount2: ThisFunction)

      val initStateFn: DuringCallbackU[P, S, B] => WrapObj[S] = $ => WrapObj(initF($).runNow())
      spec.updateDynamic("getInitialState")(initStateFn: ThisFunction)

      lc.getDefaultProps.flatMap(_.toJsCallback).foreach(f => spec.updateDynamic("getDefaultProps")(f))
      setThisFn1(lc.componentWillUnmount,  "componentWillUnmount")
      setThisFn1(lc.componentDidMount,     "componentDidMount")
      setFnPS   (lc.componentWillUpdate,   "componentWillUpdate")
      setFnPS   (lc.componentDidUpdate,    "componentDidUpdate")
      setFnPS   (lc.shouldComponentUpdate, "shouldComponentUpdate")

      val willReceiveProps = (t: DuringCallbackM[P, S, B, N], p: WrapObj[P]) => {
        lc.componentWillReceiveProps.foreach { f =>
          f(t, p.v).runNow()
        }
        val update = lc.shouldComponentUpdate.fold(true)(_.apply(t, p.v, t.state).runNow())
        if (update) updateBackend(t, p)
      }
      spec.updateDynamic("componentWillReceiveProps")(willReceiveProps: ThisFunction)

      if (jsMixins.nonEmpty) {
        val mixins = JArray(jsMixins: _*)
        spec.updateDynamic("mixins")(mixins)
      }

      val spec2 = spec.asInstanceOf[ReactComponentSpec[P, S, B, N]]
      lc.configureSpec.foreach(_(spec2).runNow())
      spec2
    }

    def build: C =
      cc(React.createFactory(React.createClass(buildSpec)))
  }
}
