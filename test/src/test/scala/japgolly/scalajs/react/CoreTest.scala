package japgolly.scalajs.react

import japgolly.scalajs.react.Addons.{ReactCssTransitionGroupM, ReactCssTransitionGroup, ReactCloneWithProps}
import monocle.macros.Lenses
import utest._
import scala.scalajs.js, js.{Array => JArray}
import org.scalajs.dom.raw._
import vdom.all._
import TestUtil._
import japgolly.scalajs.react.test.{Simulation, DebugJs, ReactTestUtils}
import CompScope._
import CompState._

object CoreTest extends TestSuite {

  lazy val CA = ReactComponentB[Unit]("CA").render_C(c => div(c)).buildU
  lazy val CB = ReactComponentB[Unit]("CB").render_C(c => span(c)).buildU
  lazy val H1 = ReactComponentB[String]("H").render_P(p => h1(p)).build

  lazy val SI = ReactComponentB[Unit]("SI")
    .initialState(123)
    .render(T => input(value := T.state.toString))
    .domType[HTMLInputElement]
    .componentDidMount($ => Callback {
      val s: String = $.getDOMNode().value // Look, it knows its DOM node type
    })
    .buildU

  val tagmod  : TagMod       = cls := "ho"
  val reacttag: ReactTag     = span()
  val relement: ReactElement = span()

  @Lenses
  case class StrInt(str: String, int: Int)

  @Lenses
  case class StrIntWrap(strInt: StrInt)

  val tests = TestSuite {

    'scalatags {
      def test(subj: ReactElement, exp: String): Unit =
        ReactComponentB[Unit]("tmp").render(_ => subj).buildU.apply() shouldRender exp
      def reactNode: ReactNode = H1("cool")
      def checkbox(check: Boolean) = input(`type` := "checkbox", checked := check)

      'short     - test(div(45: Short),                        "<div>45</div>")
      'byte      - test(div(50: Byte),                         "<div>50</div>")
      'int       - test(div(666),                              "<div>666</div>")
      'long      - test(div(123L),                             "<div>123</div>")
      'double    - test(div(12.3),                             "<div>12.3</div>")
      'string    - test(div("yo"),                             "<div>yo</div>")
      'reactNode - test(div(reactNode),                        "<div><h1>cool</h1></div>")
      'comp      - test(div(H1("a")),                          "<div><h1>a</h1></div>")
      'seqTag    - test(div(Seq (span(1), span(2))),           "<div><span>1</span><span>2</span></div>")
      'listTag   - test(div(List(span(1), span(2))),           "<div><span>1</span><span>2</span></div>")
      'listComp  - test(div(List(H1("a"), H1("b"))),           "<div><h1>a</h1><h1>b</h1></div>")
      'list2jAry - test(div(List(H1("a"), H1("b")).toJsArray), "<div><h1>a</h1><h1>b</h1></div>")
      'jAryTag   - test(div(JArray(span(1), span(2))),         "<div><span>1</span><span>2</span></div>")
      'jAryComp  - test(div(JArray(H1("a"), H1("b"))),         "<div><h1>a</h1><h1>b</h1></div>")
      'checkboxT - test(checkbox(true),                      """<input type="checkbox" checked>""")
      'checkboxF - test(checkbox(false),                     """<input type="checkbox">""")
      'aria      - test(div(aria.label := "ow", "a"),        """<div aria-label="ow">a</div>""")

      'dangerouslySetInnerHtml - test(div(dangerouslySetInnerHtml("<span>")), "<div><span></div>")

      'optional {
        'option {
          'attr_some    - test(div(cls := "hi".some),    """<div class="hi"></div>""")
          'attr_none    - test(div(cls := "hi".none),    """<div></div>""")
          'style_some   - test(div(color := "red".some), """<div style="color:red;"></div>""")
          'style_none   - test(div(color := "red".none), """<div></div>""")
          'tagmod_some  - test(div(tagmod.some),         """<div class="ho"></div>""")
          'tagmod_none  - test(div(tagmod.none),         """<div></div>""")
          'tag_some     - test(div(reacttag.some),       """<div><span></span></div>""")
          'tag_none     - test(div(reacttag.none),       """<div></div>""")
          'element_some - test(div(relement.some),       """<div><span></span></div>""")
          'element_none - test(div(relement.none),       """<div></div>""")
          'comp_some    - test(div(H1("yoo").some),      """<div><h1>yoo</h1></div>""")
          'comp_none    - test(div(H1("yoo").none),      """<div></div>""")
        }
        'jsUndefOr {
          'attr_def      - test(div(cls := "hi".jsdef),    """<div class="hi"></div>""")
          'attr_undef    - test(div(cls := "h1".undef),    """<div></div>""")
          'style_def     - test(div(color := "red".jsdef), """<div style="color:red;"></div>""")
          'style_undef   - test(div(color := "red".undef), """<div></div>""")
          'tagmod_def    - test(div(tagmod.jsdef),         """<div class="ho"></div>""")
          'tagmod_undef  - test(div(tagmod.undef),         """<div></div>""")
          'tag_def       - test(div(reacttag.jsdef),       """<div><span></span></div>""")
          'tag_undef     - test(div(reacttag.undef),       """<div></div>""")
          'element_def   - test(div(relement.jsdef),       """<div><span></span></div>""")
          'element_undef - test(div(relement.undef),       """<div></div>""")
          'comp_def      - test(div(H1("yoo").jsdef),      """<div><h1>yoo</h1></div>""")
          'comp_undef    - test(div(H1("yoo").undef),      """<div></div>""")
        }
        'maybe {
          import ScalazReact._
          'attr_just     - test(div(cls := "hi".just),        """<div class="hi"></div>""")
          'attr_empty    - test(div(cls := "h1".maybeNot),    """<div></div>""")
          'style_just    - test(div(color := "red".just),     """<div style="color:red;"></div>""")
          'style_empty   - test(div(color := "red".maybeNot), """<div></div>""")
          'tagmod_just   - test(div(tagmod.just),             """<div class="ho"></div>""")
          'tagmod_empty  - test(div(tagmod.maybeNot),         """<div></div>""")
          'tag_just      - test(div(reacttag.just),           """<div><span></span></div>""")
          'tag_empty     - test(div(reacttag.maybeNot),       """<div></div>""")
          'element_just  - test(div(relement.just),           """<div><span></span></div>""")
          'element_empty - test(div(relement.maybeNot),       """<div></div>""")
          'comp_just     - test(div(H1("yoo").just),          """<div><h1>yoo</h1></div>""")
          'comp_empty    - test(div(H1("yoo").maybeNot),      """<div></div>""")
        }
      }

      'tagmodComposition {
        val a: TagMod = cls := "hehe"
        val b: TagMod = h3("Good")
        val c = a compose b
        test(div(c), """<div class="hehe"><h3>Good</h3></div>""")
      }

      'combination - test(
        div(cls := "hi", "Str: ", 123, JArray(H1("a"), H1("b")), p(cls := "pp")("!")),
        """<div class="hi">Str: 123<h1>a</h1><h1>b</h1><p class="pp">!</p></div>""")

      'styles - test(
        div(backgroundColor := "red", marginTop := "10px", "!"),
        """<div style="background-color:red;margin-top:10px;">!</div>""")

      'noImplicitUnit - assertTypeMismatch(compileError("""val x: TagMod = ()"""))

      'numericStyleUnits {
        'px  - test(div(marginTop := 2.px),  """<div style="margin-top:2px;"></div>""")
        'ex  - test(div(marginTop := 2.ex),  """<div style="margin-top:2ex;"></div>""")
        'em  - test(div(marginTop := 2.em),  """<div style="margin-top:2em;"></div>""")
        'str - assertContains(compileError("""div(marginTop := "hehe".em)""").msg, "not a member of String")
      }

      "?=" - test(
        span(
          true ?= (color := "red"), false ?= (color := "black"),
          true ?= (cls := "great"), false ?= (cls := "saywhat"),
          "ok"),
        """<span class="great" style="color:red;">ok</span>""")

      // Copied from Scalatags
      'copied {

        'attributeChaining - test(
          div(`class` := "thing lol", id := "cow"),
          """<div id="cow" class="thing lol"></div>""")

        'mixingAttributesStylesAndChildren - test(
          div(id := "cow", float.left, p("i am a cow")),
          """<div id="cow" style="float:left;"><p>i am a cow</p></div>""")

        //class/style after attr appends, but attr after class/style overwrites
//        'classStyleAttrOverwriting - test(
//          div(cls := "my-class", style := "background-color: red;", float.left, p("i am a cow")),
//          """<div class="my-class" style="background-color:red;float:left;"><p>i am a cow</p></div>""")

        'intSeq - test(
          div(h1("Hello"), for (i <- 0 until 5) yield i),
          """<div><h1>Hello</h1>01234</div>""")

        'stringArray - {
          val strArr = Array("hello")
          test(div("lol".some, 1.some, None: Option[String], h1("Hello"), Array(1, 2, 3), strArr, EmptyTag),
            """<div>lol1<h1>Hello</h1>123hello</div>""")
        }

        'applyChaining - test(
          a(tabIndex := 1, cls := "lol")(href := "boo", alt := "g"),
          """<a tabindex="1" href="boo" alt="g" class="lol"></a>""")
      }

      'customAttr  - test(div("accept".reactAttr := "yay"), """<div accept="yay"></div>""")
      'customStyle - test(div("face".reactStyle := "yay"),  """<div style="face:yay;"></div>""")
      'customTag   - test(("ass".reactTag)("Snicker"),      """<ass>Snicker</ass>""")
    }

    'classSet {
      'allConditional {
        val r = ReactComponentB[(Boolean,Boolean)]("C").render_P(p => div(classSet("p1" -> p._1, "p2" -> p._2))("x")).build
        r((false, false)) shouldRender """<div>x</div>"""
        r((true,  false)) shouldRender """<div class="p1">x</div>"""
        r((false, true))  shouldRender """<div class="p2">x</div>"""
        r((true,  true))  shouldRender """<div class="p1 p2">x</div>"""
      }
      'hasMandatory {
        val r = ReactComponentB[Boolean]("C").render_P(p => div(classSet1("mmm", "ccc" -> p))("x")).build
        r(false) shouldRender """<div class="mmm">x</div>"""
        r(true)  shouldRender """<div class="mmm ccc">x</div>"""
      }
      'appends {
        val r = ReactComponentB[Boolean]("C").render_P(p =>
          div(cls := "neat", classSet1("mmm", "ccc" -> p), cls := "slowclap", "x")).build
        r(false) shouldRender """<div class="neat mmm slowclap">x</div>"""
        r(true)  shouldRender """<div class="neat mmm ccc slowclap">x</div>"""
      }
    }

    'props {
      'unit {
        val r = ReactComponentB[Unit]("U").render_C(c => h1(c)).buildU
        r(div("great")) shouldRender "<h1><div>great</div></h1>"
      }

      'required {
        val r = ReactComponentB[String]("C").render_P(name => div("Hi ", name)).build
        r("Mate") shouldRender "<div>Hi Mate</div>"
      }

      val O = ReactComponentB[String]("C").render_P(name => div("Hey ", name)).propsDefault("man").build
      'optionalNone {
        O() shouldRender "<div>Hey man</div>"
      }
      'optionalSome {
        O(Some("dude")) shouldRender "<div>Hey dude</div>"
      }

      'always {
        val r = ReactComponentB[String]("C").render_P(name => div("Hi ", name)).propsConst("there").build
        r() shouldRender "<div>Hi there</div>"
      }
    }

    'builder {
      'configure {
        var called = 0
        val f = (_: ReactComponentB[Unit,Unit,Unit,TopNode]).componentWillMount(_ => Callback(called += 1))
        val c = ReactComponentB[Unit]("X").render(_ => div("")).configure(f, f).buildU
        ReactTestUtils.renderIntoDocument(c())
        assert(called == 2)
      }
    }

    'keys {
      'specifiableThruCtor {
        val k1 = "heasdf"
        val xx = CA.withKey(k1)()
        val k2 = xx.key
        k2 mustEqual k1
      }
    }

    'children {
      'argsToComponents {
        'listOfScalatags {
          CA(List(h1("nice"), h2("good"))) shouldRender "<div><h1>nice</h1><h2>good</h2></div>" }

        'listOfReactComponents {
          CA(List(CB(h1("nice")), CB(h2("good")))) shouldRender
            "<div><span><h1>nice</h1></span><span><h2>good</h2></span></div>" }
      }

      'rendersGivenChildren {
        'none { CA() shouldRender "<div></div>" }
        'one { CA(h1("yay")) shouldRender "<div><h1>yay</h1></div>" }
        'two { CA(h1("yay"), h3("good")) shouldRender "<div><h1>yay</h1><h3>good</h3></div>" }
        'nested { CA(CB(h1("nice"))) shouldRender "<div><span><h1>nice</h1></span></div>" }
      }

      'forEach {
        val C1 = collectorNC[ReactNode]((l, c) => c.forEach(l append _))
        val C2 = collectorNC[(ReactNode, Int)]((l, c) => c.forEach((a, b) => l.append((a, b))))

        'withoutIndex {
          val x = runNC(C1, h1("yay"), h3("good"))
          assert(x.size == 2)
        }

        'withIndex {
          val x = runNC(C2, h1("yay"), h3("good"))
          assert(x.size == 2)
          assert(x.toList.map(_._2) == List(0,1))
        }
      }

      'only {
        val A = collector1C[Option[ReactNode]](_.only)

        'one {
          val r = run1C(A, div("Voyager (AU) is an awesome band"))
          assert(r.isDefined)
        }

        'two {
          val r = run1C(A, div("The Pensive Disarray"), div("is such a good song"))
          assert(r == None)
        }
      }
    }

    'stateFocus {
      // def inc(s: CompStateFocus[Int]) = s.modState(_ * 3)
      case class SI(s: String, i: Int)
      val C = ReactComponentB[SI]("C").initialState_P(p => p).render($ => {
        val f = $.zoom(_.i)((a,b) => a.copy(i = b))
        // inc(f)
        div($.state.s + "/" + (f.state*3))
      }).build
      C(SI("Me",7)) shouldRender "<div>Me/21</div>"
    }

    'mountedStateAccess {
      val c = ReactTestUtils.renderIntoDocument(SI())
      assert(c.state == 123)
    }

    'builtWithDomType {
      val c = ReactTestUtils.renderIntoDocument(SI())
      val v = c.getDOMNode().value // Look, it knows its DOM node type
      assert(v == "123")
    }

    'selectWithMultipleValues {
      val s = ReactComponentB[Unit]("s")
        .render(T =>
          select(multiple := true, value := js.Array("a", "c"))(
            option(value := "a")("a"),
            option(value := "b")("b"),
            option(value := "c")("c")
          )
        )
        .domType[HTMLSelectElement]
        .buildU

      val c = ReactTestUtils.renderIntoDocument(s())
      val sel = c.getDOMNode()
      val options = sel.options.asInstanceOf[js.Array[HTMLOptionElement]] // https://github.com/scala-js/scala-js-dom/pull/107
      val selectedOptions = options filter (_.selected) map (_.value)
      assert(selectedOptions.toSet == Set("a", "c"))
    }

    'refs {
      class WB(t: BackendScope[String,_]) { def getName = t.props.runNow() }
      type W = ReactComponentM[String, Unit, WB, TopNode]
      val W = ReactComponentB[String]("").stateless.backend(new WB(_)).render_C(c => div(c)).build

      // 'simple - simple refs are tested in TestTest

      'parameterised {
        val r = (0 to 3).map(_ => RefHolder[ReactComponentM_[HTMLParagraphElement]])
        val C = ReactComponentB[Unit]("").render(_ => div(p.withRef(r(1).set)( "One"), p.withRef(r(2).set)("Two"))).buildU
        val c = ReactTestUtils.renderIntoDocument(C())
        val t1 = r(1)().map(_.getDOMNode().innerHTML mustEqual "One")
        val t2 = r(2)().map(_.getDOMNode().innerHTML mustEqual "Two")
        val t3 = r(3)().toCallbackB.map(exists => assert(!exists))
        t1 >> t2 >> t3
      }

      'onOwnedComponenets {
        var innerRef = RefHolder[W]
        var outerRef = RefHolder[W]
        val innerWName = "My name is IN"
        val outerWName = "My name is OUT"
        var tested = false
        val C = ReactComponentB[Unit]("")
          .render(P => {
            val inner = W.withRef(innerRef.set)(innerWName)
            val outer = W.withRef(outerRef.set)(outerWName, inner)
            div(outer)
           }).buildU

        ReactTestUtils renderIntoDocument C()

        val c: Callback =
          innerRef().map(_.backend.getName mustEqual innerWName) >>
          outerRef().map(_.backend.getName mustEqual outerWName) >>
          Callback(tested = true)

        c.runNow()

        assert(tested) // just in case
      }

      'shouldNotHaveRefsOnUnmountedComponents {
        var ref: js.UndefOr[ReactComponentM_[dom.html.Div]] = js.undefined
        val C = ReactComponentB[Unit]("child").render(_ => div()).buildU
        val P = ReactComponentB[Unit]("parent")
          .render(P => C(div.withRef(c => ref = c)))
          .componentDidMount(scope => Callback(assert(ref.isEmpty)))
      }

      'refToThirdPartyComponents {
        class RB(t:BackendScope[_,_]) {
          val addonRef = RefHolder[ReactCssTransitionGroupM]
          val test: Callback = addonRef().toCallbackB.map(assert(_))
        }

        val C = ReactComponentB[Unit]("C")
          .backend(new RB(_))
          .render($ => div(ReactCssTransitionGroup(name = "testname", ref = $.backend.addonRef.set)()))
          .componentDidMount(_.backend.test)
          .buildU
        ReactTestUtils renderIntoDocument C()
      }


      'passCallbackTo {
        var ref: js.UndefOr[HTMLDivElement]= js.undefined
        val C = ReactComponentB[Unit]("C")
          .render(_ => div.withRef(t ⇒ ref = t.getDOMNode())("Hola"))
          .buildU
        ReactTestUtils renderIntoDocument C()
        assert(ref.get.textContent == "Hola")
      }

      // Added in React 0.13
      'passCallback {
        var i: js.UndefOr[ReactComponentM_[HTMLInputElement]] = js.undefined
        val C = ReactComponentB[Unit]("C")
          .render(_ => div(input.withRef(c => i = c)(value := "yay")))
          .buildU
        ReactTestUtils renderIntoDocument C()
        assert(i.isDefined)
        assert(i.get.getDOMNode().value == "yay")
      }
    }

    'inference {
      import TestUtil.Inference._
      def st_get: S => T = null
      def st_set: (S, T) => S = null

      "DuringCallbackU ops" - test[DuringCallbackU[P, S, U]   ](_.zoom(st_get)(st_set)).expect[ReadDirectWriteCallbackOps[T]]
      "DuringCallbackM ops" - test[DuringCallbackM[P, S, U, N]](_.zoom(st_get)(st_set)).expect[ReadDirectWriteCallbackOps[T]]
      "BackendScope    ops" - test[BackendScope   [P, S]      ](_.zoom(st_get)(st_set)).expect[ReadCallbackWriteCallbackOps[T]]
      "ReactComponentM ops" - test[ReactComponentM[P, S, U, N]](_.zoom(st_get)(st_set)).expect[ReadDirectWriteDirectOps[T]]

      "DuringCallbackU props" - test[DuringCallbackU[P, S, U]   ](_.props).expect[P]
      "DuringCallbackM props" - test[DuringCallbackM[P, S, U, N]](_.props).expect[P]
      "WillUpdate      props" - test[WillUpdate     [P, S, U, N]](_.props).expect[P]
      "BackendScope    props" - test[BackendScope   [P, S]      ](_.props).expect[CallbackTo[P]]
      "ReactComponentM props" - test[ReactComponentM[P, S, U, N]](_.props).expect[P]

      "DuringCallbackU state" - test[DuringCallbackU[P, S, U]   ](_.state).expect[S]
      "DuringCallbackM state" - test[DuringCallbackM[P, S, U, N]](_.state).expect[S]
      "WillUpdate      state" - test[WillUpdate     [P, S, U, N]](_.state).expect[S]
      "BackendScope    state" - test[BackendScope   [P, S]      ](_.state).expect[CallbackTo[S]]
      "ReactComponentM state" - test[ReactComponentM[P, S, U, N]](_.state).expect[S]

      "DuringCallbackU state" - test[DuringCallbackU[P, S, U]   ](_.zoom(st_get)(st_set).state).expect[T]
      "DuringCallbackM state" - test[DuringCallbackM[P, S, U, N]](_.zoom(st_get)(st_set).state).expect[T]
      "BackendScope    state" - test[BackendScope   [P, S]      ](_.zoom(st_get)(st_set).state).expect[CallbackTo[T]]
      "ReactComponentM state" - test[ReactComponentM[P, S, U, N]](_.zoom(st_get)(st_set).state).expect[T]
    }

    'shouldCorrectlyDetermineIfComponentIsMounted {
      val C = ReactComponentB[Unit]("IsMountedTestComp")
          .render(P => div())
          .componentWillMount(scope => Callback(assert(!scope.isMounted())))
          .componentDidMount(scope => Callback(assert(scope.isMounted())))
          .buildU
      val instance =  ReactTestUtils.renderIntoDocument(C())
      assert(instance.isMounted())
    }

    'cloneWithProps {
      'shouldCloneDomComponentWithNewProps {
        val Parent = ReactComponentB[Unit]("Parent")
          .render_C(c => {
            div(cls := "parent")(
              ReactCloneWithProps(React.Children.only(c),Map("className" -> "xyz"))
            )
          })
          .buildU
        val GrandParent = ReactComponentB[Unit]("GrandParent")
          .render(P => Parent(div(cls := "child")))
          .buildU
        val instance = ReactTestUtils.renderIntoDocument(GrandParent())
        val n = ReactTestUtils.findRenderedDOMComponentWithClass(instance, "xyz").getDOMNode()
        assert(n.matchesBy[HTMLElement](_.className == "xyz child"))
      }
    }

    'findDOMNode {
      val m = ReactTestUtils renderIntoDocument H1("good")
      val n = React.findDOMNode(m)
      removeReactDataAttr(n.outerHTML) mustEqual "<h1>good</h1>"
    }

    'domTypeBeforeCallbacks {
      ReactComponentB[Unit]("").stateless
        .render(_ => canvas())
        .domType[HTMLCanvasElement]
        .componentDidMount($ => Callback($.getDOMNode().getContext("2d")))
        .buildU
    }

    'multiModState {
      'simple {
        val C = ReactComponentB[Unit]("multiModState")
          .initialState(3)
          .render { $ =>
            val add7 = $.modState(_ + 7)
            val add1 = $.modState(_ + 1)
            button(onClick --> (add1 >> add7))
          }
          .buildU
        val c = ReactTestUtils.renderIntoDocument(C())
        c.state mustEqual 3
        Simulation.click run c
        c.state mustEqual 11
      }
      'zoom {
        val C = ReactComponentB[Unit]("multiModState")
          .initialState(StrInt("yay", 3))
          .render { $ =>
            val $$ = $.zoom(_.int)((a,b) => a.copy(int = b))
            val add7 = $$.modState(_ + 7)
            val add1 = $$.modState(_ + 1)
            button(onClick --> (add1 >> add7))
          }
          .buildU
        val c = ReactTestUtils.renderIntoDocument(C())
        c.state mustEqual StrInt("yay", 3)
        Simulation.click run c
        c.state mustEqual StrInt("yay", 11)
        c.setState(StrInt("oh", 100))
        Simulation.click run c
        c.state mustEqual StrInt("oh", 108)
      }
      'zoomL {
        import MonocleReact._ // TODO Move
        val C = ReactComponentB[Unit]("multiModState")
          .initialState(StrInt("yay", 3))
          .render { $ =>
            val $$ = $ zoomL StrInt.int
            val add7 = $$.modState(_ + 7)
            val add1 = $$.modState(_ + 1)
            button(onClick --> (add1 >> add7))
          }
          .buildU
        val c = ReactTestUtils.renderIntoDocument(C())
        c.state mustEqual StrInt("yay", 3)
        Simulation.click run c
        c.state mustEqual StrInt("yay", 11)
        c.setState(StrInt("oh", 100))
        Simulation.click run c
        c.state mustEqual StrInt("oh", 108)
      }
      'zoomL2 {
        import MonocleReact._ // TODO Move
        val C = ReactComponentB[Unit]("multiModState")
          .initialState(StrIntWrap(StrInt("yay", 3)))
          .render { $ =>
            val $$ = $ zoomL StrIntWrap.strInt zoomL StrInt.int
            val add7 = $$.modState(_ + 7)
            val add1 = $$.modState(_ + 1)
            button(onClick --> (add1 >> add7))
          }
          .buildU
        val c = ReactTestUtils.renderIntoDocument(C())
        c.state mustEqual StrIntWrap(StrInt("yay", 3))
        Simulation.click run c
        c.state mustEqual StrIntWrap(StrInt("yay", 11))
        c.setState(StrIntWrap(StrInt("oh", 100)))
        Simulation.click run c
        c.state mustEqual StrIntWrap(StrInt("oh", 108))
      }
    }
  }
}
