package izumi.distage.model.definition.dsl

import izumi.distage.constructors.macros.AnyConstructorMacro
import izumi.distage.model.definition.Binding.{EmptySetBinding, ImplBinding, SetElementBinding, SingletonBinding}
import izumi.distage.model.definition.dsl.AbstractBindingDefDSL.SetElementInstruction.ElementAddTags
import izumi.distage.model.definition.dsl.AbstractBindingDefDSL.SetInstruction.{AddTagsAll, SetIdAll}
import izumi.distage.model.definition.dsl.AbstractBindingDefDSL.SingletonInstruction._
import izumi.distage.model.definition.dsl.AbstractBindingDefDSL._
import izumi.distage.model.definition.{Binding, BindingTag, Bindings, ImplDef}
import izumi.distage.model.providers.ProviderMagnet
import izumi.distage.model.reflection.{DIKey, IdContract}
import izumi.fundamentals.platform.language.{CodePositionMaterializer, SourceFilePosition}
import izumi.reflect.Tag

import scala.collection.mutable
import scala.language.experimental.macros

trait AbstractBindingDefDSL[BindDSL[_], BindDSLAfterFrom[_], SetDSL[_]] {
  private[this] final val mutableState: mutable.ArrayBuffer[BindingRef] = _initialState
  protected[this] def _initialState: mutable.ArrayBuffer[BindingRef] = mutable.ArrayBuffer.empty

  private[definition] def _bindDSL[T](ref: SingletonRef): BindDSL[T]
  private[definition] def _bindDSLAfterFrom[T](ref: SingletonRef): BindDSLAfterFrom[T]
  private[definition] def _setDSL[T](ref: SetRef): SetDSL[T]

  private[definition] def frozenState: collection.Seq[Binding] = {
    mutableState.flatMap(_.interpret)
  }

  protected[this] def _registered[T <: BindingRef](bindingRef: T): T = {
    mutableState += bindingRef
    bindingRef
  }

  final protected[this] def make[T]: BindDSL[T] = macro AnyConstructorMacro.make[BindDSL, T]

  /**
    * Set bindings are useful for implementing event listeners, plugins, hooks, http routes, etc.
    *
    * To define a multibinding use `.many` and `.add` methods in ModuleDef DSL:
    *
    * {{{
    * import cats.effect._, org.http4s._, org.http4s.dsl.io._, scala.concurrent.ExecutionContext.Implicits.global
    * import distage._
    *
    * object HomeRouteModule extends ModuleDef {
    *   many[HttpRoutes[IO]].add {
    *     HttpRoutes.of[IO] { case GET -> Root / "home" => Ok(s"Home page!") }
    *   }
    * }
    * }}}
    *
    * Set bindings defined in different modules will be merged together into a single Set.
    * You can summon a created Set by type `Set[T]`:
    *
    * {{{
    * import cats.implicits._, import org.http4s.server.blaze._, import org.http4s.implicits._
    *
    * object BlogRouteModule extends ModuleDef {
    *   many[HttpRoutes[IO]].add {
    *     HttpRoutes.of[IO] { case GET -> Root / "blog" / post => Ok("Blog post ``$post''!") }
    *   }
    * }
    *
    * class HttpServer(routes: Set[HttpRoutes[IO]]) {
    *   val router = routes.foldK
    *
    *   def serve = BlazeBuilder[IO]
    *     .bindHttp(8080, "localhost")
    *     .mountService(router, "/")
    *     .start
    * }
    *
    * val objects = Injector().produce(HomeRouteModule ++ BlogRouteModule)
    * val server = objects.get[HttpServer]
    *
    * val testRouter = server.router.orNotFound
    *
    * testRouter.run(Request[IO](uri = uri("/home"))).flatMap(_.as[String]).unsafeRunSync
    * // Home page!
    *
    * testRouter.run(Request[IO](uri = uri("/blog/1"))).flatMap(_.as[String]).unsafeRunSync
    * // Blog post ``1''!
    * }}}
    *
    * @see Guice wiki on Multibindings: https://github.com/google/guice/wiki/Multibindings
    */
  final protected[this] def many[T](implicit tag: Tag[Set[T]], pos: CodePositionMaterializer): SetDSL[T] = {
    val setRef = _registered(new SetRef(Bindings.emptySet[T]))
    _setDSL(setRef)
  }

  /** Same as `make[T].from(implicitly[T])` **/
  final protected[this] def addImplicit[T: Tag](implicit instance: T, pos: CodePositionMaterializer): BindDSLAfterFrom[T] = {
    val ref = _registered(new SingletonRef(Bindings.binding(instance)))
    _bindDSLAfterFrom(ref)
  }

  final protected[this] def _make[T: Tag](provider: ProviderMagnet[T])(implicit pos: CodePositionMaterializer): BindDSL[T] = {
    val ref = _registered(new SingletonRef(Bindings.provider[T](provider)))
    _bindDSL[T](ref)
  }
}

object AbstractBindingDefDSL {

  trait BindingRef {
    def interpret: collection.Seq[Binding]
  }

  final class SingletonRef(initial: SingletonBinding[DIKey.TypeKey], ops: mutable.Queue[SingletonInstruction] = mutable.Queue.empty) extends BindingRef {
    override def interpret: collection.Seq[ImplBinding] = {
      var b: SingletonBinding[DIKey.BasicKey] = initial
      var refs: List[SingletonBinding[DIKey.BasicKey]] = Nil

      ops.foreach {
        case SetImpl(implDef) =>
          b = b.withImplDef(implDef)
        case AddTags(tags) =>
          b = b.addTags(tags)
        case SetId(id, idContract) =>
          val key = DIKey.IdKey(b.key.tpe, id)(idContract)
          b = b.withTarget(key)
        case SetIdFromImplName() =>
          // b.key.tpe is the same b.implementation.tpe because `SetIdFromImplName` comes before `SetImpl`...
          b = b.withTarget(DIKey.IdKey(b.key.tpe, b.key.tpe.tag.longName.toString.toLowerCase))
        case AliasTo(key, pos) =>
          // it's ok to retrieve `tags`, `implType` & `key` from `b` because all changes to
          // `b` properties must come before first `aliased` call
          // after first `aliased` no more changes are possible
          val newRef = SingletonBinding(key, ImplDef.ReferenceImpl(b.implementation.implType, b.key, weak = false), b.tags, pos)
          refs = newRef :: refs
      }

      b :: refs.reverse
    }

    def key: DIKey.TypeKey = initial.key

    def append(op: SingletonInstruction): SingletonRef = {
      ops += op
      this
    }
  }

  final class SetRef(initial: EmptySetBinding[DIKey.TypeKey]) extends BindingRef {
    private[this] val setOps: mutable.Queue[SetInstruction] = mutable.Queue.empty
    private[this] val elems: mutable.Queue[SetElementRef] = mutable.Queue.empty
    private[this] val multiElems: mutable.Queue[MultiSetElementRef] = mutable.Queue.empty

    override def interpret: collection.Seq[Binding] = {
      val emptySetBinding = setOps.foldLeft(initial: EmptySetBinding[DIKey.BasicKey]) {
        (b, instr) =>
          instr match {
            case AddTagsAll(tags) => b.addTags(tags)
            case SetIdAll(id, idContract) => b.withTarget(DIKey.IdKey(b.key.tpe, id)(idContract))
          }
      }

      val finalKey = emptySetBinding.key

      val elemBindings = elems.map(_.interpret(finalKey))
      val multiSetBindings = multiElems.flatMap(_.interpret(finalKey))

      emptySetBinding +: elemBindings ++: multiSetBindings
    }

    def appendElem(op: SetElementRef): SetRef = {
      elems += op
      this
    }

    def appendOp(op: SetInstruction): SetRef = {
      setOps += op
      this
    }

    def appendMultiElem(op: MultiSetElementRef): SetRef = {
      multiElems += op
      this
    }
  }

  final class SetElementRef(implDef: ImplDef, pos: SourceFilePosition, ops: mutable.Queue[SetElementInstruction] = mutable.Queue.empty) {
    def interpret(setKey: DIKey.BasicKey): SetElementBinding = {
      val implKey = DIKey.TypeKey(implDef.implType)
      val elKey = DIKey.SetElementKey(setKey, implKey, Some(implDef))

      ops.foldLeft(SetElementBinding(elKey, implDef, Set.empty, pos)) {
        (b, instr) =>
          instr match {
            case ElementAddTags(tags) => b.addTags(tags)
          }
      }
    }

    def append(op: SetElementInstruction): SetElementRef = {
      ops += op
      this
    }
  }

  final class MultiSetElementRef(implDef: ImplDef, pos: SourceFilePosition) {
    private[this] val ops: mutable.Queue[MultiSetElementInstruction] = mutable.Queue.empty

    def interpret(setKey: DIKey.BasicKey): Seq[Binding] = {
      val valueProxyKey = DIKey.IdKey(implDef.implType, DIKey.MultiSetImplId(setKey, implDef))
      val valueProxyBinding = SingletonBinding(valueProxyKey, implDef, Set.empty, pos)

      val elementKey = DIKey.SetElementKey(setKey, valueProxyKey, Some(implDef))
      val refBind0 = SetElementBinding(elementKey, ImplDef.ReferenceImpl(valueProxyBinding.key.tpe, valueProxyBinding.key, weak = false), Set.empty, pos)

      val refBind = ops.foldLeft(refBind0) {
        (b, op) =>
          op match {
            case MultiSetElementInstruction.MultiAddTags(tags) => b.addTags(tags)
          }
      }

      Seq(valueProxyBinding, refBind)
    }

    def append(op: MultiSetElementInstruction): MultiSetElementRef = {
      ops += op
      this
    }
  }

  sealed trait SingletonInstruction
  object SingletonInstruction {
    final case class SetImpl(implDef: ImplDef) extends SingletonInstruction
    final case class AddTags(tags: Set[BindingTag]) extends SingletonInstruction
    final case class SetId[I](id: I, idContract: IdContract[I]) extends SingletonInstruction
    final case class SetIdFromImplName() extends SingletonInstruction
    final case class AliasTo(key: DIKey.BasicKey, pos: SourceFilePosition) extends SingletonInstruction
  }

  sealed trait SetInstruction
  object SetInstruction {
    final case class AddTagsAll(tags: Set[BindingTag]) extends SetInstruction
    final case class SetIdAll[I](id: I, idContract: IdContract[I]) extends SetInstruction
  }

  sealed trait SetElementInstruction
  object SetElementInstruction {
    final case class ElementAddTags(tags: Set[BindingTag]) extends SetElementInstruction
  }

  sealed trait MultiSetElementInstruction
  object MultiSetElementInstruction {
    final case class MultiAddTags(tags: Set[BindingTag]) extends MultiSetElementInstruction
  }

}
