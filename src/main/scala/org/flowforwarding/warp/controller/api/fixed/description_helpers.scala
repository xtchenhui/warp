package org.flowforwarding.warp.controller.api.fixed

import scala.util.{Success, Failure, Try}
import scala.reflect.ClassTag

import com.typesafe.config.Config
import org.flowforwarding.warp.controller.api.dynamic.{DynamicBuilderInput, DynamicStructure, DynamicStructureBuilder, DynamicDriver}
import org.flowforwarding.warp.controller.api.fixed.util._
import org.flowforwarding.warp.controller.api.fixed.text_view.{StructureTextView, StructureWithTextView, BITextViewSupport, BITextView}

trait StructuresDescriptionHelper{
  self: DynamicStructureBuilder[_ <: DynamicStructure] =>

  implicit private[fixed] val namesConfig: Config

  private[fixed] abstract class OfpStructure[T: ClassTag](protected val underlyingStructure: DynamicStructure) extends ConcreteStructure with StructureWithTextView {
    apiToImplement: T =>
    val namesConfig = self.namesConfig
    val correspondingClass = implicitly[ClassTag[T]].runtimeClass.asInstanceOf[Class[T]]

    override def textView = StructureTextView.reflect(this, implicitly[ClassTag[T]].runtimeClass)
  }

  private[fixed] trait OfpStructureBuilder[Input <: BuilderInput] extends ConcreteStructureBuilder[Input] with BITextViewSupport[Input] {
    protected val api: StructuresDescriptionHelper with DynamicStructureBuilder[_] = self
  }

  private[fixed] implicit class DynamicStructureExt(s: DynamicStructure){
    def ofType[T: ClassTag] = s.isTypeOf(namesConfig.getTypeName(implicitly[ClassTag[T]].runtimeClass))
  }

  protected def builderClasses: List[Class[_ <: OfpStructureBuilder[_]]]

  private[fixed] def selectBuilder(predicate: Class[_] => Boolean) =
    builderClasses.collectFirst { case b if predicate(b) => b }
      .get
      .getConstructors
      .head
      .newInstance(this)
      .asInstanceOf[OfpStructureBuilder[_]]

  def parseTextView(input: BITextView): Try[BuilderInput] = Try {
    val b = selectBuilder { b => firstGenericParameter(b).map { s => val StructureName(n) = s; n } == Some(input.structureName)}
    b.inputFromTextView(input).asInstanceOf[BuilderInput]
  }

  def buildFixed[T <: BuilderInput: ClassTag](input: BITextView): Try[T] =
    parseTextView(input) map implicitly[ClassTag[T]].runtimeClass.asInstanceOf[Class[T]].cast

  def buildDynamic(input: BITextView): Try[DynamicStructure] = {
    val structureBuilder = selectBuilder { b => firstGenericParameter(b).map { s => val StructureName(n) = s; n } == Some(input.structureName)}
    self.build(structureBuilder toDynamicInput input)
  }

  def buildDynamic(input: BuilderInput): Try[DynamicStructure] = {
    val structureBuilder = selectBuilder { b => firstGenericParameter(b) == Some(input.getClass) } .asInstanceOf[OfpStructureBuilder[BuilderInput]]
    self.build(structureBuilder toDynamicInput input)
  }
}

trait StructureDescription{
  apiProvider: StructuresDescriptionHelper =>

  protected def builderClasses: List[Class[_ <: OfpStructureBuilder[_]]] = List()
}

trait MessagesDescriptionHelper[T <: SpecificVersionMessageHandlers[_, _]] extends StructuresDescriptionHelper{
  driver: DynamicStructureBuilder[_ <: DynamicStructure] =>

  protected def messageClasses: List[Class[_]]

  type MessageInput <: BuilderInput
  type OfpMessage[_] <: StructureWithTextView
  protected type OfpMessageBuilder[_ <: MessageInput] <: OfpStructureBuilder[_]

  def toConcreteMessage(dynamic: DynamicStructure): Try[OfpMessage[_]] = Try {
    messageClasses collectFirst {
      case c if dynamic.isTypeOf(namesConfig.getTypeName(firstGenericParameter(c).get)) =>
        // Every XXXStructure type is inner class, so its constructor must get reference to outer class
        c.getConstructors.head.newInstance(this, dynamic)
    } match {
      case Some(structure) => structure.asInstanceOf[OfpMessage[_]]
      case None => throw new RuntimeException("Undefined type of structure.")
    }
  }
}

trait MessageDescription extends StructureDescription{
  apiProvider: MessagesDescriptionHelper[_] =>

  protected def messageClasses: List[Class[_]] = List()
}