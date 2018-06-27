package com.github.pshirshov.izumi.distage.config

import com.github.pshirshov.izumi.distage.config.annotations._
import com.github.pshirshov.izumi.distage.config.codec.RuntimeConfigReader
import com.github.pshirshov.izumi.distage.config.model.AppConfig
import com.github.pshirshov.izumi.distage.model.exceptions.DIException
import com.github.pshirshov.izumi.distage.model.plan.ExecutableOp.ImportDependency
import com.github.pshirshov.izumi.distage.model.plan.{ExecutableOp, FinalPlan, FinalPlanImmutableImpl}
import com.github.pshirshov.izumi.distage.model.planning.PlanningHook
import com.github.pshirshov.izumi.distage.model.reflection.universe.RuntimeDIUniverse
import com.github.pshirshov.izumi.distage.model.reflection.universe.RuntimeDIUniverse._
import com.typesafe.config.Config

import scala.util.Try
import scala.util.control.NonFatal

class ConfigProvider(config: AppConfig, reader: RuntimeConfigReader) extends PlanningHook {

  import ConfigProvider._

  override def hookFinal(plan: FinalPlan): FinalPlan = {
    val updatedSteps = plan.steps
      .map {
        case ConfigImport(ci) =>
          try {
            val requirement = toRequirement(ci)
            translate(ci.imp, requirement)
          } catch {
            case NonFatal(t) =>
              TranslationResult.Failure(ci.imp, t)
          }

        case s =>
          TranslationResult.Success(s)
      }

    val errors = updatedSteps.collect({ case t: TranslationFailure => t })

    if (errors.nonEmpty) {
      // TODO: instead of throwing exception we may just print a warning and leave import in place. It would fail on provisioning anyway
      throw new DIException(s"Cannot resolve config:\n - ${errors.mkString("\n - ")}", null)
    }

    val ops = updatedSteps.collect({ case TranslationResult.Success(op) => op })
    val newPlan = FinalPlanImmutableImpl(plan.definition, ops)
    newPlan
  }

  private def translate(op: ExecutableOp, step: RequiredConfigEntry): TranslationResult = {
    val results = step.paths.map(p => Try((p.toPath, config.config.getConfig(p.toPath))))
    val loaded = results.collect({ case scala.util.Success(value) => value })

    if (loaded.isEmpty) {
      return TranslationResult.MissingConfigValue(op, step.paths)
    }

    val section = loaded.head
    try {
      val product = reader.readConfig(section._2, step.targetType)
      TranslationResult.Success(ExecutableOp.WiringOp.ReferenceInstance(step.target, Wiring.UnaryWiring.Instance(step.target.tpe, product)))
    } catch {
      case NonFatal(t) =>
        TranslationResult.ExtractionFailure(op, step.targetType, section._1, section._2, t)
    }
  }

  implicit class TypeExt(t: TypeFull) {
    def name: String = t.tpe.typeSymbol.asClass.fullName
  }

  case class DepType(fqName: Seq[String], qualifier: Seq[String]) {
    def name: Seq[String] = Seq(fqName.last)
  }

  case class DepUsage(fqName: Seq[String], qualifier: Seq[String]) {
    def name: Seq[String] = Seq(fqName.last)
  }

  case class DependencyContext(dep: DepType, usage: DepUsage)

  private def toRequirement(op: ConfigImport): RequiredConfigEntry = {
    val paths = op.id match {
      case p: ConfPathId =>
        Seq(
          ConfigPath(p.pathOverride.split('.'))
        )

      case _: AutomaticConfId =>
        toRequirementAuto(op)
    }

    RequiredConfigEntry(paths, op.imp.target.tpe, op.imp.target)

  }

  private def toRequirementAuto(op: ConfigImport): Seq[ConfigPath] = {
    val dc = DependencyContext(structInfo(op), usageInfo(op))

    Seq(
      ConfigPath(dc.usage.fqName ++ dc.usage.qualifier ++ dc.dep.fqName ++ dc.dep.qualifier)
      , ConfigPath(dc.usage.fqName ++ dc.usage.qualifier ++ dc.dep.name ++ dc.dep.qualifier)
      , ConfigPath(dc.usage.name ++ dc.usage.qualifier ++ dc.dep.fqName ++ dc.dep.qualifier)
      , ConfigPath(dc.usage.name ++ dc.usage.qualifier ++ dc.dep.name ++ dc.dep.qualifier)

      , ConfigPath(dc.usage.fqName ++ dc.usage.qualifier ++ dc.dep.fqName)
      , ConfigPath(dc.usage.fqName ++ dc.usage.qualifier ++ dc.dep.name)
      , ConfigPath(dc.usage.name ++ dc.usage.qualifier ++ dc.dep.fqName)
      , ConfigPath(dc.usage.name ++ dc.usage.qualifier ++ dc.dep.name)
    ).distinct
  }

  private def structInfo(op: ConfigImport) = {
    val qualifier = op.id match {
      case id: AutoConfId =>
        id.parameter.name
      case id: ConfId =>
        id.parameter.name
      case _ =>
        throw new IllegalArgumentException(s"Unexpected op: $op")
    }


    val structFqName = op.imp.target.tpe.name
    val structFqParts = structFqName.split('.').toSeq
    DepType(structFqParts, Seq(qualifier))
  }

  private def usageInfo(op: ConfigImport) = {
    /* we may get set type the following way:

     case id: AutomaticConfId =>
      id.binding match {
        case b: RuntimeDIUniverse.DIKey.SetElementKey =>
          b.set.tpe.tpe.typeArgs.head.typeSymbol.name.decodedName.toString

     Though in that case we need to disambiguate set members somehow
     */

    val usageKeyFqName = op.id match {
      case id: AutoConfId =>
        id.binding.tpe.name
      case id: ConfId =>
        id.nameOverride
      case _ =>
        throw new IllegalArgumentException(s"Unexpected op: $op")
    }

    val usageKeyParts: Seq[String] = usageKeyFqName.split('.').toSeq

    val usageKeyQualifier = op.id match {
      case id: AutoConfId =>
        id.binding match {
          case k: DIKey.IdKey[_] =>
            Some(k.idContract.repr(k.id))

          case _ =>
            None
        }

      case _ =>
        None
    }
    val usageQualifier = usageKeyQualifier.toSeq
    DepUsage(usageKeyParts, usageQualifier)
  }

}

object ConfigProvider {

  private case class RequiredConfigEntry(paths: Seq[ConfigPath], targetType: TypeFull, target: DIKey) {
    override def toString: String = {
      val allPaths = paths.map(_.toPath).mkString("\n  ")

      s"""type: $targetType, target: $target
         |$allPaths""".stripMargin
    }
  }


  private case class ConfigPath(parts: Seq[String]) {
    def toPath: String = parts.mkString(".")

    override def toString: String = s"cfg:$toPath"
  }

  private sealed trait TranslationResult
  private sealed trait TranslationFailure extends TranslationResult {
    def op: ExecutableOp
    def target: RuntimeDIUniverse.DIKey = op.target
  }

  private object TranslationResult {

    final case class Success(op: ExecutableOp) extends TranslationResult

    final case class MissingConfigValue(op: ExecutableOp, paths: Seq[ConfigPath]) extends TranslationFailure {
      override def toString: String = {
        val tried = paths.mkString("{", "|", "}")
        s"$target: missing config value, tried paths: $tried"
      }
    }

    final case class ExtractionFailure(op: ExecutableOp, tpe: TypeFull, path: String, config: Config, f: Throwable) extends TranslationFailure {
      override def toString: String = s"$target: cannot read $tpe out of $path ==> $config: ${f.getMessage}"
    }

    final case class Failure(op: ExecutableOp, f: Throwable) extends TranslationFailure {
      override def toString: String = s"$target: unexpected exception: ${f.getMessage}"
    }

  }

  private case class ConfigImport(id: AbstractConfId, imp: ImportDependency)

  private object ConfigImport {
    def unapply(op: ExecutableOp): Option[ConfigImport] = {
      op match {
        case i: ImportDependency =>
          unapply(op.target).map(id => ConfigImport(id, i))
        case _ =>
          None
      }
    }

    private def unapply(arg: DIKey): Option[AbstractConfId] = {
      arg match {
        case k: DIKey.IdKey[_] =>
          k.id match {
            case id: AbstractConfId =>
              Some(id)
            case _ =>
              None
          }

        case _ =>
          None
      }
    }
  }

}
