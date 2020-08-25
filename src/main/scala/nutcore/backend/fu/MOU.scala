/**************************************************************************************
* Copyright (c) 2020 Institute of Computing Technology, CAS
* Copyright (c) 2020 University of Chinese Academy of Sciences
* 
* NutShell is licensed under Mulan PSL v2.
* You can use this software according to the terms and conditions of the Mulan PSL v2. 
* You may obtain a copy of Mulan PSL v2 at:
*             http://license.coscl.org.cn/MulanPSL2 
* 
* THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER 
* EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR 
* FIT FOR A PARTICULAR PURPOSE.  
*
* See the Mulan PSL v2 for more details.  
***************************************************************************************/

package nutcore

import chisel3._
import chisel3.util._
import chisel3.util.experimental.BoringUtils

import utils._

// memory order unit
object MOUOpType {
  def fence  = "b00".U
  def fencei = "b01".U
  def sfence_vma = "b10".U
}

class MOUIO extends FunctionUnitIO {
  val cfIn = Flipped(new CtrlFlowIO)
  val ctrlIn = Flipped(new CtrlSignalIO)
  val redirect = new RedirectIO
}

class MOU extends NutCoreModule {
  val io = IO(new MOUIO)

  val (valid, src1, src2, func) = (io.in.valid, io.in.bits.src1, io.in.bits.src2, io.in.bits.func)
  def access(valid: Bool, src1: UInt, src2: UInt, func: UInt): UInt = {
    this.valid := valid
    this.src1 := src1
    this.src2 := src2
    this.func := func
    io.out.bits
  }

  io.redirect.target := io.cfIn.pc + 4.U
  io.redirect.valid := valid
  io.redirect.rtype := 0.U
  val flushICache = valid && (func === MOUOpType.fencei)
  BoringUtils.addSource(flushICache, "MOUFlushICache")
  Debug(){
    when(flushICache){
      printf("%d: [MOU] Flush I$ at %x\n", GTimer(), io.cfIn.pc)
    }
  }

  val flushTLB = valid && (func === MOUOpType.sfence_vma)
  val sfence = Wire(new SfenceBundle)
  sfence.valid := flushTLB
  sfence.bits.rs1 := io.ctrlIn.rfSrc1===0.U
  sfence.bits.rs2 := io.ctrlIn.rfSrc2===0.U
  sfence.bits.addr := src1
  BoringUtils.addSource(sfence, "SfenceBundle")
  Debug() {
    when (flushTLB) {
      printf("%d: [MOU] Flush TLB at %x\n", GTimer(), io.cfIn.pc)
    }
  }

  io.out.bits := 0.U
  io.in.ready := true.B
  io.out.valid := valid
}
