//See LICENSE for license details.
package firesim.midasexamples

import java.io.File
import scala.sys.process.{stringSeqToProcess, ProcessLogger}
import scala.io.Source

import firesim.util.GeneratorArgs

abstract class TutorialSuite(
    val targetName: String, // See GeneratorUtils
    targetConfigs: String = "NoConfig",
    tracelen: Int = 8,
    simulationArgs: Seq[String] = Seq()
  ) extends firesim.TestSuiteCommon with firesim.util.HasFireSimGeneratorUtilities {

  val longName = names.topModuleProject + "." + names.topModuleClass + "." + names.configs
  val backendSimulator = "verilator"

  lazy val generatorArgs = GeneratorArgs(
    midasFlowKind = "midas",
    targetDir = "generated-src",
    topModuleProject = "firesim.midasexamples",
    topModuleClass = targetName,
    targetConfigProject = "firesim.midasexamples",
    targetConfigs = targetConfigs,
    platformConfigProject = "firesim.midasexamples",
    platformConfigs = "HostDebugFeatures_DefaultF1Config")

  val args = Seq(s"+tracelen=$tracelen") ++ simulationArgs
  val commonMakeArgs = Seq(s"TARGET_PROJECT=midasexamples",
                           s"DESIGN=$targetName",
                           s"TARGET_CONFIG=${generatorArgs.targetConfigs}",
                           s"PLATFORM_CONFIG=${generatorArgs.platformConfigs}")
  val targetTuple = generatorArgs.tupleName

  def run(backend: String,
          debug: Boolean = false,
          sample: Option[File] = None,
          logFile: Option[File] = None,
          waveform: Option[File] = None,
          args: Seq[String] = Nil) = {
    val makeArgs = Seq(
      s"run-$backend%s".format(if (debug) "-debug" else ""),
      "SAMPLE=%s".format(sample map toStr getOrElse ""),
      "LOGFILE=%s".format(logFile map toStr getOrElse ""),
      "WAVEFORM=%s".format(waveform map toStr getOrElse ""),
      "ARGS=%s".format(args mkString " "))
    if (isCmdAvailable(backend)) {
      make(makeArgs:_*)
    } else 0
  }


  def runTest(b: String, debug: Boolean = false) {
    behavior of s"$targetName in $b"
    compileMlSimulator(b, debug)
    val sample = Some(new File(outDir, s"$targetName.$b.sample"))
    val testEnv = "MIDAS-level simulation" + { if (debug) " with waves enabled" else "" }
    if (isCmdAvailable(b)) {
      it should s"pass in ${testEnv}" in {
        assert(run(b, debug, sample, args=args) == 0)
      }
    } else {
      ignore should s"pass in ${testEnv}" in { }
    }
  }

  // Checks that the synthesized print log in ${genDir}/${synthPrintLog} matches the
  // printfs from the RTL simulator
  def diffSynthesizedPrints(synthPrintLog: String) {
    behavior of "synthesized print log"
    it should "match the logs produced by the verilated design" in {
      def printLines(filename: File): Seq[String] = {
        val lines = Source.fromFile(filename).getLines.toList
        lines.filter(_.startsWith("SYNTHESIZED_PRINT")).sorted
      }

      val verilatedOutput = printLines(new File(outDir,  s"/${targetName}.${backendSimulator}.out"))
      val synthPrintOutput = printLines(new File(genDir, s"/${synthPrintLog}"))
      assert(verilatedOutput.size == synthPrintOutput.size && verilatedOutput.nonEmpty)
      for ( (vPrint, sPrint) <- verilatedOutput.zip(synthPrintOutput) ) {
        assert(vPrint == sPrint)
      }
    }
  }

  // Checks that the synthesized print log in ${genDir}/${synthPrintLog} matches the
  // printfs from the RTL simulator
  def diffAutoCounterOutput(autocounterOutputLog: String, referenceFile: String) {
    behavior of "AutoCounter output log"
    it should "match the logs commited based on the design intent" in {
      def printLines(filename: File): Seq[String] = {
        val lines = Source.fromFile(filename).getLines.toList
        lines.sorted
      }

      def printVerilatorLines(filename: File): Seq[String] = {
        val lines = Source.fromFile(filename).getLines.toList
        val stripedlines = lines.filter(_.startsWith("AUTOCOUNTER_PRINT")).map(line => line.stripPrefix("AUTOCOUNTER_PRINT").trim.replaceAll(" +", " "))
        stripedlines.sorted
      }

      //val referenceOutput = printLines(new File(outDir,  s"/${referenceFile}"))
      val referenceOutput = printVerilatorLines(new File(outDir,  s"/${targetName}.${backendSimulator}.out"))
      val autocounterOutput = printLines(new File(genDir, s"/${autocounterOutputLog}"))
      assert(referenceOutput.size == autocounterOutput.size && referenceOutput.nonEmpty)
      for ( (rPrint, acPrint) <- referenceOutput.zip(autocounterOutput) ) {
        assert(rPrint == acPrint)
      }
    }
  }

  clean
  mkdirs
  elaborate
  runTest(backendSimulator)
}

//class PointerChaserF1Test extends TutorialSuite(
//  "PointerChaser", "PointerChaserConfig", simulationArgs = Seq("`cat runtime.conf`"))
class GCDF1Test extends TutorialSuite("GCD")
// Hijack Parity to test all of the Midas-level backends
class ParityF1Test extends TutorialSuite("Parity") {
  runTest("verilator", true)
  runTest("vcs", true)
}
class ShiftRegisterF1Test extends TutorialSuite("ShiftRegister")
class ResetShiftRegisterF1Test extends TutorialSuite("ResetShiftRegister")
class EnableShiftRegisterF1Test extends TutorialSuite("EnableShiftRegister")
class StackF1Test extends TutorialSuite("Stack")
class RiscF1Test extends TutorialSuite("Risc")
class RiscSRAMF1Test extends TutorialSuite("RiscSRAM")
class AssertModuleF1Test extends TutorialSuite("AssertModule")
class AutoCounterModuleF1Test extends TutorialSuite("AutoCounterModule",
        simulationArgs = Seq("+autocounter-readrate0=1000", "+autocounter-filename0=AUTOCOUNTERFILE")) {
        diffAutoCounterOutput("AUTOCOUNTERFILE", "AutoCounterModule.autocounter.out")
}
class AutoCounterCoverModuleF1Test extends TutorialSuite("AutoCounterCoverModule",
        simulationArgs = Seq("+autocounter-readrate0=1000", "+autocounter-filename0=AUTOCOUNTERFILE")) {
        diffAutoCounterOutput("AUTOCOUNTERFILE", "AutoCounterCoverModule.autocounter.out")
}
class PrintfModuleF1Test extends TutorialSuite("PrintfModule",
  simulationArgs = Seq("+print-no-cycle-prefix", "+print-file=synthprinttest.out")) {
  diffSynthesizedPrints("synthprinttest.out")
}
class NarrowPrintfModuleF1Test extends TutorialSuite("NarrowPrintfModule",
  simulationArgs = Seq("+print-no-cycle-prefix", "+print-file=synthprinttest.out")) {
  diffSynthesizedPrints("synthprinttest.out")
}
// MIDAS 2.0 compiler tests
class WireInterconnectF1Test extends TutorialSuite("WireInterconnect")
