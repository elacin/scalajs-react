package japgolly.scalajs.react

import scala.scalajs.js

object RefHolder{
  def apply[N]: RefHolder[N]= new RefHolder[N]
}

final class RefHolder[N]{
  private var r: js.UndefOr[N] = js.undefined

  def apply() = get

  val set: N => Unit =
    t => r = js.UndefOr.any2undefOrA(t)

  val get: CallbackOption[N] =
    CallbackOption.liftOptionLike(r)

  val invalidate: Unit =
    Callback(r = js.undefined)
}