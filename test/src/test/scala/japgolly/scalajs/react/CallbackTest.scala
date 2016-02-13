package japgolly.scalajs.react

import scala.concurrent._
import scala.concurrent.duration._
import utest._

object CallbackTest extends TestSuite {

  def testEvalStrategy(f: ( => Callback) => Callback, e1: Int, e2: Int, e3: Int): Unit = {
    var i = 9
    var j = 0
    def setJ(x: Int) = Callback(j = x)
    val cb = f(setJ(i))
    assert(j == e1)
    i = 1
    cb.runNow()
    assert(j == e2)
    j = 0
    i = 2
    cb.runNow()
    assert(j == e3)
  }

  def assertCompiles[A](f: => A): Unit = ()

  override def tests = TestSuite {
    'guard {
      def assertFails(f: CompileError): Unit = assert(f.msg contains "which will discard without running it")
      def cb = Callback.empty
      def cbI = CallbackTo(3)

      "Callback(unit)"       - assertCompiles[Callback](Callback(()))
      "Callback(boolean)"    - assertCompiles[Callback](Callback(false))
      "Callback(int)"        - assertCompiles[Callback](Callback(3))
      "Callback(Callback)"   - assertFails(compileError("Callback(cb)"))
      "Callback(CallbackTo)" - assertFails(compileError("Callback(cbI)"))
    }

    'lazily -
      testEvalStrategy(Callback lazily _, 0, 1, 1)

    'byName -
      testEvalStrategy(Callback byName _, 0, 1, 2)

    'future {
      'repeatable {
        import test.RunNowEC.Implicit._
        var runs = 0
        def modState = Callback(runs += 1)
        def f = Future(modState)
        val c = Callback.future(f)
        assert(runs == 0)
        c.runNow()
        c.runNow()
        assert(runs == 2)
      }

      'toFlatFuture {
        import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
        val c = CallbackTo(Future(666))
        val f = c.toFlatFuture
        f.map(i => assert(i == 666))
      }
    }

    'flatten {
      val a = CallbackTo(CallbackTo(3)).flatten
      val b: CallbackTo[Int] = a

      val x = CallbackTo(Callback.empty).flatten
      val y: Callback = x
    }
    'map_makes_sense {
      val cb1 = Callback.info("hola")

      "Not throw away callbacks" -
        assert(compileError("(cb1 map (_ => cb1)): Callback").msg.nonEmpty)

      "Convert Callback[Unit]" - {
        val cb1: CallbackTo[Unit] =
          CallbackTo("1") map println
        val cb2: Callback =
          cb1
      }

      "Convert CallbackOption[Unit]" -
        assertCompiles(CallbackOption[String](CallbackTo(Some("1"))) map println: Callback)

      "Convert CallbackOption[Unit]" - {
        val cbo1: CallbackOption[String] =
          CallbackOption(CallbackTo(Some("1")))

        val cbo2: CallbackOption[Unit] =
          cbo1 map println
        val cbo3: CallbackOption[Empty] =
          cbo1 map ((s: String) => println(s))

        val cb1: Callback =
          cbo2
        val cb2: Callback =
          cbo3
      }
    }
  }
}
