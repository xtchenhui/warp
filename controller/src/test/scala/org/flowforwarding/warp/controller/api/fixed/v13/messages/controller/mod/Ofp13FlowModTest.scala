/*
 * © 2013 FlowForwarding.Org
 * All Rights Reserved.  Use is subject to license terms.
 *
 * @author Vitaliy Savkin (Vitaliy_Savkin@epam.com)
 */
package org.flowforwarding.warp.controller.api.fixed.v13.messages.controller.mod

import spire.syntax.literals._
import spire.math.{UShort, ULong, UInt, UByte}

import org.flowforwarding.warp.controller.api.fixed._
import org.flowforwarding.warp.controller.api.fixed.v13.structures._
import org.flowforwarding.warp.controller.api.fixed.v13.messages.controller.{Max, ControllerMaxLength}
import org.flowforwarding.warp.controller.api.fixed.v13.messages.controller.mod.FlowModCommand.FlowModCommand
import org.flowforwarding.warp.controller.api.fixed.v13.messages.{Ofp13MessageInput, Ofp13MessageDescription}
import org.flowforwarding.warp.controller.api.fixed.v13.messages.async.{Error => msgError}
import org.flowforwarding.warp.controller.api.fixed.v13.Ofp13DriverApi
import org.flowforwarding.warp.controller.api.fixed.util.MacAddress
import org.flowforwarding.warp.controller.api.fixed.v13.structures.instructions._
import org.flowforwarding.warp.controller.api.fixed.v13.structures.actions.Action

trait Ofp13FlowModTest extends MessageTestsSet[Ofp13DriverApi]{
  def inputTemplate(instructions: Instruction*) = FlowModInput(
    ul"1",
    ul"0",
    uh"0",
    FlowModCommand.Add,
    ui"1",
    ui"5",
    ui"100",
    ControllerMaxLength(uh"30"),
    PortNumber(ui"333"),
    GroupId(ui"7"),
    FlowModFlags(false, false, true, false, true),
    MatchInput(true, Array()),
    instructions.toArray
  )

  private def noErrorTestTemplate(description: String, instructions: Instruction*): (FlowModInput, TestData) =
    inputTemplate(instructions: _*) -> TestNoError(classOf[msgError], description)

  private def errorResponseTestTemplate(description: String, eType: UShort, eCode: UShort, instructions: Instruction*): (FlowModInput, TestData) =
    inputTemplate(instructions: _*) -> TestResponse({ case e: msgError => e.errorType == eType && e.errorCode == eCode }, description)

  abstract override def tests = super.tests +
    noErrorTestTemplate("Flow mod: empty") +
    noErrorTestTemplate("Flow mod: InstructionGotoTable", InstructionGotoTable(ub"10")) +
    noErrorTestTemplate("Flow mod: InstructionGotoTable", InstructionWriteMetadata(ul"10", ul"10")) +
    errorResponseTestTemplate("Flow mod: InstructionGotoTable", eType = uh"3", eCode = uh"1", InstructionMeter(ui"100")) + /* Unsupported instruction */
    noErrorTestTemplate("Flow mod: InstructionGotoTable", InstructionWriteMetadata(ul"20", ul"20"), InstructionGotoTable(ub"1")) +
/*  InstructionExperimenter causes a crash of LINC-Switch. Should be tested separately from other messages.  */
//  noErrorTestTemplate("Flow mod: InstructionExperimenter", InstructionExperimenter(500)) +
//  noErrorTestTemplate("Flow mod: InstructionExperimenter with data", InstructionExperimenter(100, Array[Byte](1, 2, 3, 4, 5)))
    noErrorTestTemplate("Flow mod: InstructionClearActions",  InstructionClearActions()) +
    noErrorTestTemplate("Flow mod: ActionSetQueue (Apply)",   InstructionApplyActions(Array(Action.setQueue(ui"1")))) +
    noErrorTestTemplate("Flow mod: ActionSetMplsTtl (Apply)", InstructionApplyActions(Array(Action.setMplsTtl(ub"1")))) +
    noErrorTestTemplate("Flow mod: ActionSetNwTtl",           InstructionWriteActions(Array(Action.setNwTtl(ub"1")))) +
    noErrorTestTemplate("Flow mod: ActionPopVlan",            InstructionWriteActions(Array(Action.popVlan))) +
    noErrorTestTemplate("Flow mod: ActionPopMpls",            InstructionWriteActions(Array(Action.popMpls(uh"1")))) +
    noErrorTestTemplate("Flow mod: Array of actions",         InstructionWriteActions(Array(Action.setQueue(ui"1"), Action.setMplsTtl(ub"1"), Action.setNwTtl(ub"1")))) +
    errorResponseTestTemplate("Flow mod: ActionGroup",    eType = uh"2", eCode = uh"9", InstructionWriteActions(Array(Action.group(GroupId(ui"10"))))) +       /* BAD_OUT_GROUP */
    errorResponseTestTemplate("Flow mod: ActionPushVlan", eType = uh"2", eCode = uh"5", InstructionWriteActions(Array(Action.pushVlan(uh"1")))) +  /* BAD_ARGUMENT */
    errorResponseTestTemplate("Flow mod: ActionPushMpls", eType = uh"2", eCode = uh"5", InstructionWriteActions(Array(Action.pushMpls(uh"1")))) +  /* BAD_ARGUMENT */
    errorResponseTestTemplate("Flow mod: ActionOutput",   eType = uh"2", eCode = uh"4", InstructionWriteActions(Array(Action.output(PortNumber(ui"100"), Max)))) /* BAD_OUT_PORT */
    /* TODO: tests for OxmTlv */
    //noErrorTestTemplate("Flow mod: ip_proto (OxmTlv)", InstructionWriteActions(Array(Action.setField(ip_proto(4)))))
    /* These actions cause crashes of LINC-Switch. Should be tested separately from other messages.  */
    //noErrorTestTemplate("Flow mod: ActionPushPbb",      InstructionWriteActions(Array(Action.pushPbb(1)))) +
    //noErrorTestTemplate("Flow mod: ActionPopPbb",       InstructionWriteActions(Array(Action.popPbb))) +
    //noErrorTestTemplate("Flow mod: ActionCopyTtlOut",   InstructionWriteActions(Array(Action.copyTtlOut))) +
    //noErrorTestTemplate("Flow mod: ActionCopyTtlIn",    InstructionWriteActions(Array(Action.copyTtlIn))) +
    //noErrorTestTemplate("Flow mod: ActionExperimenter", InstructionWriteActions(Array(Action.experimenter(10))))
}