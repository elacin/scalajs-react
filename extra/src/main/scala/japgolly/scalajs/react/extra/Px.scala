package japgolly.scalajs.react.extra

/**
 * A mechanism for caching data with dependencies.
 *
 * This is basically a performance-focused, lightweight implementation of pull-based
 * <a href="http://en.wikipedia.org/wiki/Functional_reactive_programming">FRP</a>,
 * pull-based meaning that in the chain A→B→C, an update to A doesn't affect C until the value of C is requested.
 *
 * What does Px mean? I don't know, I just needed a name and I liked the way @lihaoyi's Rx type name looked in code.
 * You can consider this "Performance eXtension". If this were Java it'd be named
 * `AutoRefreshOnRequestDependentCachedVariable`.
 */
sealed abstract class Px[A] {

  /** Revision of its value. Increments when value changes. */
  def rev: Int

  /** Get the latest value, updating itself if necessary. */
  def value(): A

  /** Get the last used value without updating. */
  def peek: A

  final def valueSince(r: Int): Option[A] = {
    val v = value() // Ensure value and rev are up-to-date
    if (rev != r)
      Some(v)
    else
      None
  }

  def map[B](f: A => B): Px[B] =
    new Px.Map(this, f)

  def flatMap[B](f: A => Px[B]): Px[B] =
    new Px.FlatMap(this, f)

  // override def toString = value().toString
}

object Px {
  sealed abstract class Root[A] extends Px[A] {
    protected val ignoreChange: (A, A) => Boolean

    protected var _rev = 0
    protected var _value: A

    override final def rev = _rev

    override final def peek = _value

    protected def setMaybe(a: A): Unit =
      if (!ignoreChange(_value, a)) {
        _rev += 1
        _value = a
      }
  }

  /**
   * A variable in the traditional sense.
   *
   * Doesn't change until you explicitly call `set()`.
   */
  final class Var[A](initialValue: A, protected val ignoreChange: (A, A) => Boolean) extends Root[A] {
    override protected var _value = initialValue

    override def value() = _value

    def set(a: A): Unit =
      setMaybe(a)
  }

  /**
   * The value of a zero-param function.
   *
   * The `M` in `ThunkM` denotes "Manual refresh", meaning that the value will not update until you explicitly call
   * `refresh()`.
   */
  final class ThunkM[A](next: () => A, protected val ignoreChange: (A, A) => Boolean) extends Root[A] {
    override protected var _value = next()

    override def value() = _value

    def refresh(): Unit =
      setMaybe(next())
  }

  /**
   * The value of a zero-param function.
   *
   * The `A` in `ThunkA` denotes "Auto refresh", meaning that the function will be called every time the value is
   * requested, and the value updated if necessary.
   */
  final class ThunkA[A](next: () => A, protected val ignoreChange: (A, A) => Boolean) extends Root[A] {
    override protected var _value = next()

    override def value() = {
      setMaybe(next())
      _value
    }
  }

  /**
   * A value `B` dependent on the value of some `Px[A]`.
   */
  final class Map[A, B](xa: Px[A], f: A => B) extends Px[B] {
    private var _value = f(xa.value())
    private var _revA  = xa.rev

    override def rev  = _revA
    override def peek = _value

    override def map[C](g: B => C): Px[C] =
      new Map(xa, g compose f)

    override def flatMap[C](g: B => Px[C]): Px[C] =
      new Px.FlatMap(xa, g compose f)

    override def value(): B = {
      xa.valueSince(_revA).foreach { a =>
        _value = f(a)
        _revA = xa.rev
      }
      _value
    }
  }

  /**
   * A `Px[B]` dependent on the value of some `Px[A]`.
   */
  final class FlatMap[A, B](xa: Px[A], f: A => Px[B]) extends Px[B] {
    private var _value = f(xa.value())
    private var _revA  = xa.rev

    override def rev  = _revA + _value.rev
    override def peek = _value.peek

    override def value(): B = {
      xa.valueSince(_revA).foreach { a =>
        _value = f(a)
        _revA = xa.rev
      }
      _value.value()
    }
  }

  final class Const[A](a: A) extends Px[A] {
    override def rev     = 0
    override def peek    = a
    override def value() = a
  }

  final class LazyConst[A](a: => A) extends Px[A] {
    override def      rev     = 0
    override lazy val peek    = a
    override def      value() = peek
  }

  // ===================================================================================================================

  /** Import this to avoid the need to call `.value()` on your `Px`s. */
  object AutoValue {
    @inline implicit def autoPxValue[A](x: Px[A]): A = x.value()
  }

  /** Refresh multiple [[ThunkM]]s at once. */
  @inline def refresh(xs: ThunkM[_]*): Unit =
    xs.foreach(_.refresh())

  // ===================================================================================================================

  @inline def const    [A](a: A)   : Px[A] = new Const(a)
  @inline def lazyConst[A](a: => A): Px[A] = new LazyConst(a)

  def apply [A](a: A)   (implicit r: Reusability[A]) = new Var(a, r.test)
  def thunkM[A](f: => A)(implicit r: Reusability[A]) = new ThunkM(() => f, r.test)
  def thunkA[A](f: => A)(implicit r: Reusability[A]) = new ThunkA(() => f, r.test)

  object NoReuse {
    private val noReuse: (Any, Any) => Boolean = (_, _) => false
    def apply [A](a: A)    = new Var(a, noReuse)
    def thunkM[A](f: => A) = new ThunkM(() => f, noReuse)
    def thunkA[A](f: => A) = new ThunkA(() => f, noReuse)
  }

  // Generated by bin/gen-px

  @inline def apply2[A,B,Z](pa:Px[A], pb:Px[B])(z:(A,B)⇒Z): Px[Z] =
    for {a←pa;b←pb} yield z(a,b)

  @inline def apply3[A,B,C,Z](pa:Px[A], pb:Px[B], pc:Px[C])(z:(A,B,C)⇒Z): Px[Z] =
    for {a←pa;b←pb;c←pc} yield z(a,b,c)

  @inline def apply4[A,B,C,D,Z](pa:Px[A], pb:Px[B], pc:Px[C], pd:Px[D])(z:(A,B,C,D)⇒Z): Px[Z] =
    for {a←pa;b←pb;c←pc;d←pd} yield z(a,b,c,d)

  @inline def apply5[A,B,C,D,E,Z](pa:Px[A], pb:Px[B], pc:Px[C], pd:Px[D], pe:Px[E])(z:(A,B,C,D,E)⇒Z): Px[Z] =
    for {a←pa;b←pb;c←pc;d←pd;e←pe} yield z(a,b,c,d,e)

  @inline def apply6[A,B,C,D,E,F,Z](pa:Px[A], pb:Px[B], pc:Px[C], pd:Px[D], pe:Px[E], pf:Px[F])(z:(A,B,C,D,E,F)⇒Z): Px[Z] =
    for {a←pa;b←pb;c←pc;d←pd;e←pe;f←pf} yield z(a,b,c,d,e,f)

  @inline def apply7[A,B,C,D,E,F,G,Z](pa:Px[A], pb:Px[B], pc:Px[C], pd:Px[D], pe:Px[E], pf:Px[F], pg:Px[G])(z:(A,B,C,D,E,F,G)⇒Z): Px[Z] =
    for {a←pa;b←pb;c←pc;d←pd;e←pe;f←pf;g←pg} yield z(a,b,c,d,e,f,g)

  @inline def apply8[A,B,C,D,E,F,G,H,Z](pa:Px[A], pb:Px[B], pc:Px[C], pd:Px[D], pe:Px[E], pf:Px[F], pg:Px[G], ph:Px[H])(z:(A,B,C,D,E,F,G,H)⇒Z): Px[Z] =
    for {a←pa;b←pb;c←pc;d←pd;e←pe;f←pf;g←pg;h←ph} yield z(a,b,c,d,e,f,g,h)

  @inline def apply9[A,B,C,D,E,F,G,H,I,Z](pa:Px[A], pb:Px[B], pc:Px[C], pd:Px[D], pe:Px[E], pf:Px[F], pg:Px[G], ph:Px[H], pi:Px[I])(z:(A,B,C,D,E,F,G,H,I)⇒Z): Px[Z] =
    for {a←pa;b←pb;c←pc;d←pd;e←pe;f←pf;g←pg;h←ph;i←pi} yield z(a,b,c,d,e,f,g,h,i)

  @inline def apply10[A,B,C,D,E,F,G,H,I,J,Z](pa:Px[A], pb:Px[B], pc:Px[C], pd:Px[D], pe:Px[E], pf:Px[F], pg:Px[G], ph:Px[H], pi:Px[I], pj:Px[J])(z:(A,B,C,D,E,F,G,H,I,J)⇒Z): Px[Z] =
    for {a←pa;b←pb;c←pc;d←pd;e←pe;f←pf;g←pg;h←ph;i←pi;j←pj} yield z(a,b,c,d,e,f,g,h,i,j)

  @inline def apply11[A,B,C,D,E,F,G,H,I,J,K,Z](pa:Px[A], pb:Px[B], pc:Px[C], pd:Px[D], pe:Px[E], pf:Px[F], pg:Px[G], ph:Px[H], pi:Px[I], pj:Px[J], pk:Px[K])(z:(A,B,C,D,E,F,G,H,I,J,K)⇒Z): Px[Z] =
    for {a←pa;b←pb;c←pc;d←pd;e←pe;f←pf;g←pg;h←ph;i←pi;j←pj;k←pk} yield z(a,b,c,d,e,f,g,h,i,j,k)

  @inline def apply12[A,B,C,D,E,F,G,H,I,J,K,L,Z](pa:Px[A], pb:Px[B], pc:Px[C], pd:Px[D], pe:Px[E], pf:Px[F], pg:Px[G], ph:Px[H], pi:Px[I], pj:Px[J], pk:Px[K], pl:Px[L])(z:(A,B,C,D,E,F,G,H,I,J,K,L)⇒Z): Px[Z] =
    for {a←pa;b←pb;c←pc;d←pd;e←pe;f←pf;g←pg;h←ph;i←pi;j←pj;k←pk;l←pl} yield z(a,b,c,d,e,f,g,h,i,j,k,l)

  @inline def apply13[A,B,C,D,E,F,G,H,I,J,K,L,M,Z](pa:Px[A], pb:Px[B], pc:Px[C], pd:Px[D], pe:Px[E], pf:Px[F], pg:Px[G], ph:Px[H], pi:Px[I], pj:Px[J], pk:Px[K], pl:Px[L], pm:Px[M])(z:(A,B,C,D,E,F,G,H,I,J,K,L,M)⇒Z): Px[Z] =
    for {a←pa;b←pb;c←pc;d←pd;e←pe;f←pf;g←pg;h←ph;i←pi;j←pj;k←pk;l←pl;m←pm} yield z(a,b,c,d,e,f,g,h,i,j,k,l,m)

  @inline def apply14[A,B,C,D,E,F,G,H,I,J,K,L,M,N,Z](pa:Px[A], pb:Px[B], pc:Px[C], pd:Px[D], pe:Px[E], pf:Px[F], pg:Px[G], ph:Px[H], pi:Px[I], pj:Px[J], pk:Px[K], pl:Px[L], pm:Px[M], pn:Px[N])(z:(A,B,C,D,E,F,G,H,I,J,K,L,M,N)⇒Z): Px[Z] =
    for {a←pa;b←pb;c←pc;d←pd;e←pe;f←pf;g←pg;h←ph;i←pi;j←pj;k←pk;l←pl;m←pm;n←pn} yield z(a,b,c,d,e,f,g,h,i,j,k,l,m,n)

  @inline def apply15[A,B,C,D,E,F,G,H,I,J,K,L,M,N,O,Z](pa:Px[A], pb:Px[B], pc:Px[C], pd:Px[D], pe:Px[E], pf:Px[F], pg:Px[G], ph:Px[H], pi:Px[I], pj:Px[J], pk:Px[K], pl:Px[L], pm:Px[M], pn:Px[N], po:Px[O])(z:(A,B,C,D,E,F,G,H,I,J,K,L,M,N,O)⇒Z): Px[Z] =
    for {a←pa;b←pb;c←pc;d←pd;e←pe;f←pf;g←pg;h←ph;i←pi;j←pj;k←pk;l←pl;m←pm;n←pn;o←po} yield z(a,b,c,d,e,f,g,h,i,j,k,l,m,n,o)

  @inline def apply16[A,B,C,D,E,F,G,H,I,J,K,L,M,N,O,P,Z](pa:Px[A], pb:Px[B], pc:Px[C], pd:Px[D], pe:Px[E], pf:Px[F], pg:Px[G], ph:Px[H], pi:Px[I], pj:Px[J], pk:Px[K], pl:Px[L], pm:Px[M], pn:Px[N], po:Px[O], pp:Px[P])(z:(A,B,C,D,E,F,G,H,I,J,K,L,M,N,O,P)⇒Z): Px[Z] =
    for {a←pa;b←pb;c←pc;d←pd;e←pe;f←pf;g←pg;h←ph;i←pi;j←pj;k←pk;l←pl;m←pm;n←pn;o←po;p←pp} yield z(a,b,c,d,e,f,g,h,i,j,k,l,m,n,o,p)

  @inline def apply17[A,B,C,D,E,F,G,H,I,J,K,L,M,N,O,P,Q,Z](pa:Px[A], pb:Px[B], pc:Px[C], pd:Px[D], pe:Px[E], pf:Px[F], pg:Px[G], ph:Px[H], pi:Px[I], pj:Px[J], pk:Px[K], pl:Px[L], pm:Px[M], pn:Px[N], po:Px[O], pp:Px[P], pq:Px[Q])(z:(A,B,C,D,E,F,G,H,I,J,K,L,M,N,O,P,Q)⇒Z): Px[Z] =
    for {a←pa;b←pb;c←pc;d←pd;e←pe;f←pf;g←pg;h←ph;i←pi;j←pj;k←pk;l←pl;m←pm;n←pn;o←po;p←pp;q←pq} yield z(a,b,c,d,e,f,g,h,i,j,k,l,m,n,o,p,q)

  @inline def apply18[A,B,C,D,E,F,G,H,I,J,K,L,M,N,O,P,Q,R,Z](pa:Px[A], pb:Px[B], pc:Px[C], pd:Px[D], pe:Px[E], pf:Px[F], pg:Px[G], ph:Px[H], pi:Px[I], pj:Px[J], pk:Px[K], pl:Px[L], pm:Px[M], pn:Px[N], po:Px[O], pp:Px[P], pq:Px[Q], pr:Px[R])(z:(A,B,C,D,E,F,G,H,I,J,K,L,M,N,O,P,Q,R)⇒Z): Px[Z] =
    for {a←pa;b←pb;c←pc;d←pd;e←pe;f←pf;g←pg;h←ph;i←pi;j←pj;k←pk;l←pl;m←pm;n←pn;o←po;p←pp;q←pq;r←pr} yield z(a,b,c,d,e,f,g,h,i,j,k,l,m,n,o,p,q,r)

  @inline def apply19[A,B,C,D,E,F,G,H,I,J,K,L,M,N,O,P,Q,R,S,Z](pa:Px[A], pb:Px[B], pc:Px[C], pd:Px[D], pe:Px[E], pf:Px[F], pg:Px[G], ph:Px[H], pi:Px[I], pj:Px[J], pk:Px[K], pl:Px[L], pm:Px[M], pn:Px[N], po:Px[O], pp:Px[P], pq:Px[Q], pr:Px[R], ps:Px[S])(z:(A,B,C,D,E,F,G,H,I,J,K,L,M,N,O,P,Q,R,S)⇒Z): Px[Z] =
    for {a←pa;b←pb;c←pc;d←pd;e←pe;f←pf;g←pg;h←ph;i←pi;j←pj;k←pk;l←pl;m←pm;n←pn;o←po;p←pp;q←pq;r←pr;s←ps} yield z(a,b,c,d,e,f,g,h,i,j,k,l,m,n,o,p,q,r,s)

  @inline def apply20[A,B,C,D,E,F,G,H,I,J,K,L,M,N,O,P,Q,R,S,T,Z](pa:Px[A], pb:Px[B], pc:Px[C], pd:Px[D], pe:Px[E], pf:Px[F], pg:Px[G], ph:Px[H], pi:Px[I], pj:Px[J], pk:Px[K], pl:Px[L], pm:Px[M], pn:Px[N], po:Px[O], pp:Px[P], pq:Px[Q], pr:Px[R], ps:Px[S], pt:Px[T])(z:(A,B,C,D,E,F,G,H,I,J,K,L,M,N,O,P,Q,R,S,T)⇒Z): Px[Z] =
    for {a←pa;b←pb;c←pc;d←pd;e←pe;f←pf;g←pg;h←ph;i←pi;j←pj;k←pk;l←pl;m←pm;n←pn;o←po;p←pp;q←pq;r←pr;s←ps;t←pt} yield z(a,b,c,d,e,f,g,h,i,j,k,l,m,n,o,p,q,r,s,t)

  @inline def apply21[A,B,C,D,E,F,G,H,I,J,K,L,M,N,O,P,Q,R,S,T,U,Z](pa:Px[A], pb:Px[B], pc:Px[C], pd:Px[D], pe:Px[E], pf:Px[F], pg:Px[G], ph:Px[H], pi:Px[I], pj:Px[J], pk:Px[K], pl:Px[L], pm:Px[M], pn:Px[N], po:Px[O], pp:Px[P], pq:Px[Q], pr:Px[R], ps:Px[S], pt:Px[T], pu:Px[U])(z:(A,B,C,D,E,F,G,H,I,J,K,L,M,N,O,P,Q,R,S,T,U)⇒Z): Px[Z] =
    for {a←pa;b←pb;c←pc;d←pd;e←pe;f←pf;g←pg;h←ph;i←pi;j←pj;k←pk;l←pl;m←pm;n←pn;o←po;p←pp;q←pq;r←pr;s←ps;t←pt;u←pu} yield z(a,b,c,d,e,f,g,h,i,j,k,l,m,n,o,p,q,r,s,t,u)

  @inline def apply22[A,B,C,D,E,F,G,H,I,J,K,L,M,N,O,P,Q,R,S,T,U,V,Z](pa:Px[A], pb:Px[B], pc:Px[C], pd:Px[D], pe:Px[E], pf:Px[F], pg:Px[G], ph:Px[H], pi:Px[I], pj:Px[J], pk:Px[K], pl:Px[L], pm:Px[M], pn:Px[N], po:Px[O], pp:Px[P], pq:Px[Q], pr:Px[R], ps:Px[S], pt:Px[T], pu:Px[U], pv:Px[V])(z:(A,B,C,D,E,F,G,H,I,J,K,L,M,N,O,P,Q,R,S,T,U,V)⇒Z): Px[Z] =
    for {a←pa;b←pb;c←pc;d←pd;e←pe;f←pf;g←pg;h←ph;i←pi;j←pj;k←pk;l←pl;m←pm;n←pn;o←po;p←pp;q←pq;r←pr;s←ps;t←pt;u←pu;v←pv} yield z(a,b,c,d,e,f,g,h,i,j,k,l,m,n,o,p,q,r,s,t,u,v)
}
