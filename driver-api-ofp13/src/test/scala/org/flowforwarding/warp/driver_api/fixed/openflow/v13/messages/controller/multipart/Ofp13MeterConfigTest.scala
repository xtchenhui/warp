/*
 * © 2013 FlowForwarding.Org
 * All Rights Reserved.  Use is subject to license terms.
 *
 * @author Vitaliy Savkin (Vitaliy_Savkin@epam.com)
 */
package org.flowforwarding.warp.driver_api.fixed
package openflow.v13
package messages.controller.multipart

import org.flowforwarding.warp.driver_api.fixed.openflow.v13.messages.controller.multipart.data._
import org.flowforwarding.warp.driver_api.fixed.openflow.v13.structures.MeterId

import org.flowforwarding.warp.driver_api.fixed.test.MessageTestsSet

trait Ofp13MeterConfigTest extends MessageTestsSet[Ofp13DriverApi] {
  private val req = MeterConfigRequest(MeterId.AllMeters)

  abstract override def tests = super.tests + {
    MultipartRequestInput(MeterConfigRequestBodyInput(req)) -> TestResponse({
      case r: MultipartReply => r.body.isInstanceOf[MeterConfigReplyBody]
    }, "MeterConfig")
  }
}