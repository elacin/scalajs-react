package japgolly.scalajs.react.test

import japgolly.scalajs.react.CallbackOption

import scala.scalajs.js.Object
import ReactTestUtils.Simulate

/**
 * Allows composition and abstraction of `ReactTestUtils.Simulate` procedures.
 */
class Simulation(_run: (() => ReactOrDomNode) => Unit) {

  def run(n: => ReactOrDomNode): Unit =
    _run(() => n)

  def run[T](cbn: => CallbackOption[T])(implicit ev: T ⇒ ReactOrDomNode): Unit =
    cbn.map(n ⇒ run(ev(n))).runNow()

  def andThen(f: Simulation) =
    new Simulation(n => { _run(n); f.run(n()) })

  @inline final def >>     (f: Simulation) = this andThen f
  @inline final def compose(f: Simulation) = f andThen this

  final def runN(cs: ReactOrDomNode*): Unit =
    cs foreach (run(_))
}

object Simulation {

  def apply(run: (=> ReactOrDomNode) => Unit): Simulation =
    new Simulation(n => run(n()))

  // Don't use default arguments - they force parentheses on to caller.
  // Eg. Simulation.blur >> Simulation.focus becomes Simulation.blur() >> Simulation.focus(). Yuk.

  def beforeInput      = Simulation(Simulate.beforeInput      (_))
  def blur             = Simulation(Simulate.blur             (_))
  def change           = Simulation(Simulate.change           (_))
  def click            = Simulation(Simulate.click            (_))
  def compositionEnd   = Simulation(Simulate.compositionEnd   (_))
  def compositionStart = Simulation(Simulate.compositionStart (_))
  def compositionUpdate= Simulation(Simulate.compositionUpdate(_))
  def contextMenu      = Simulation(Simulate.contextMenu      (_))
  def copy             = Simulation(Simulate.copy             (_))
  def cut              = Simulation(Simulate.cut              (_))
  def doubleClick      = Simulation(Simulate.doubleClick      (_))
  def drag             = Simulation(Simulate.drag             (_))
  def dragEnd          = Simulation(Simulate.dragEnd          (_))
  def dragEnter        = Simulation(Simulate.dragEnter        (_))
  def dragExit         = Simulation(Simulate.dragExit         (_))
  def dragLeave        = Simulation(Simulate.dragLeave        (_))
  def dragOver         = Simulation(Simulate.dragOver         (_))
  def dragStart        = Simulation(Simulate.dragStart        (_))
  def drop             = Simulation(Simulate.drop             (_))
  def error            = Simulation(Simulate.error            (_))
  def focus            = Simulation(Simulate.focus            (_))
  def input            = Simulation(Simulate.input            (_))
  def keyDown          = Simulation(Simulate.keyDown          (_))
  def keyPress         = Simulation(Simulate.keyPress         (_))
  def keyUp            = Simulation(Simulate.keyUp            (_))
  def load             = Simulation(Simulate.load             (_))
  def mouseDown        = Simulation(Simulate.mouseDown        (_))
  def mouseEnter       = Simulation(Simulate.mouseEnter       (_))
  def mouseLeave       = Simulation(Simulate.mouseLeave       (_))
  def mouseMove        = Simulation(Simulate.mouseMove        (_))
  def mouseOut         = Simulation(Simulate.mouseOut         (_))
  def mouseOver        = Simulation(Simulate.mouseOver        (_))
  def mouseUp          = Simulation(Simulate.mouseUp          (_))
  def paste            = Simulation(Simulate.paste            (_))
  def reset            = Simulation(Simulate.reset            (_))
  def scroll           = Simulation(Simulate.scroll           (_))
  def select           = Simulation(Simulate.select           (_))
  def submit           = Simulation(Simulate.submit           (_))
  def touchCancel      = Simulation(Simulate.touchCancel      (_))
  def touchEnd         = Simulation(Simulate.touchEnd         (_))
  def touchMove        = Simulation(Simulate.touchMove        (_))
  def touchStart       = Simulation(Simulate.touchStart       (_))
  def wheel            = Simulation(Simulate.wheel            (_))

  def beforeInput      (eventData: Object) = Simulation(Simulate.beforeInput      (_, eventData))
  def blur             (eventData: Object) = Simulation(Simulate.blur             (_, eventData))
  def change           (eventData: Object) = Simulation(Simulate.change           (_, eventData))
  def click            (eventData: Object) = Simulation(Simulate.click            (_, eventData))
  def compositionEnd   (eventData: Object) = Simulation(Simulate.compositionEnd   (_, eventData))
  def compositionStart (eventData: Object) = Simulation(Simulate.compositionStart (_, eventData))
  def compositionUpdate(eventData: Object) = Simulation(Simulate.compositionUpdate(_, eventData))
  def contextMenu      (eventData: Object) = Simulation(Simulate.contextMenu      (_, eventData))
  def copy             (eventData: Object) = Simulation(Simulate.copy             (_, eventData))
  def cut              (eventData: Object) = Simulation(Simulate.cut              (_, eventData))
  def doubleClick      (eventData: Object) = Simulation(Simulate.doubleClick      (_, eventData))
  def drag             (eventData: Object) = Simulation(Simulate.drag             (_, eventData))
  def dragEnd          (eventData: Object) = Simulation(Simulate.dragEnd          (_, eventData))
  def dragEnter        (eventData: Object) = Simulation(Simulate.dragEnter        (_, eventData))
  def dragExit         (eventData: Object) = Simulation(Simulate.dragExit         (_, eventData))
  def dragLeave        (eventData: Object) = Simulation(Simulate.dragLeave        (_, eventData))
  def dragOver         (eventData: Object) = Simulation(Simulate.dragOver         (_, eventData))
  def dragStart        (eventData: Object) = Simulation(Simulate.dragStart        (_, eventData))
  def drop             (eventData: Object) = Simulation(Simulate.drop             (_, eventData))
  def error            (eventData: Object) = Simulation(Simulate.error            (_, eventData))
  def focus            (eventData: Object) = Simulation(Simulate.focus            (_, eventData))
  def input            (eventData: Object) = Simulation(Simulate.input            (_, eventData))
  def keyDown          (eventData: Object) = Simulation(Simulate.keyDown          (_, eventData))
  def keyPress         (eventData: Object) = Simulation(Simulate.keyPress         (_, eventData))
  def keyUp            (eventData: Object) = Simulation(Simulate.keyUp            (_, eventData))
  def load             (eventData: Object) = Simulation(Simulate.load             (_, eventData))
  def mouseDown        (eventData: Object) = Simulation(Simulate.mouseDown        (_, eventData))
  def mouseEnter       (eventData: Object) = Simulation(Simulate.mouseEnter       (_, eventData))
  def mouseLeave       (eventData: Object) = Simulation(Simulate.mouseLeave       (_, eventData))
  def mouseMove        (eventData: Object) = Simulation(Simulate.mouseMove        (_, eventData))
  def mouseOut         (eventData: Object) = Simulation(Simulate.mouseOut         (_, eventData))
  def mouseOver        (eventData: Object) = Simulation(Simulate.mouseOver        (_, eventData))
  def mouseUp          (eventData: Object) = Simulation(Simulate.mouseUp          (_, eventData))
  def paste            (eventData: Object) = Simulation(Simulate.paste            (_, eventData))
  def reset            (eventData: Object) = Simulation(Simulate.reset            (_, eventData))
  def scroll           (eventData: Object) = Simulation(Simulate.scroll           (_, eventData))
  def select           (eventData: Object) = Simulation(Simulate.select           (_, eventData))
  def submit           (eventData: Object) = Simulation(Simulate.submit           (_, eventData))
  def touchCancel      (eventData: Object) = Simulation(Simulate.touchCancel      (_, eventData))
  def touchEnd         (eventData: Object) = Simulation(Simulate.touchEnd         (_, eventData))
  def touchMove        (eventData: Object) = Simulation(Simulate.touchMove        (_, eventData))
  def touchStart       (eventData: Object) = Simulation(Simulate.touchStart       (_, eventData))
  def wheel            (eventData: Object) = Simulation(Simulate.wheel            (_, eventData))

  // Helpers for common scenarios

  def focusSimBlur(s: Simulation) =
    focus >> s >> blur

  def focusChangeBlur(newValue: String) =
    focusSimBlur(ChangeEventData(value = newValue).simulation)
}
