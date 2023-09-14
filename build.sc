import mill._, scalalib._
import coursier.maven.MavenRepository

object ivys {
  val scala = "2.13.10"
  val chisel3 = ivy"org.chipsalliance::chisel:6.0.0-M3"
  val chisel3Plugin = ivy"org.chipsalliance:::chisel-plugin:6.0.0-M3"
}

trait CommonModule extends ScalaModule {
  override def scalaVersion = ivys.scala

  override def scalacOptions = Seq("-Ymacro-annotations")
}

trait HasChisel3 extends ScalaModule {
  override def repositoriesTask = T.task {
    super.repositoriesTask() ++ Seq(
      MavenRepository("https://oss.sonatype.org/content/repositories/snapshots")
    )
  }
  override def ivyDeps = Agg(ivys.chisel3)
  override def scalacPluginIvyDeps = Agg(ivys.chisel3Plugin)
}

trait HasChiselTests extends SbtModule {
  object test extends SbtModuleTests with TestModule.ScalaTest {
    override def ivyDeps = Agg(ivy"edu.berkeley.cs::chiseltest:0.5.4")
  }
}

trait CommonNS extends SbtModule with CommonModule with HasChisel3

object difftest extends CommonNS {
  override def millSourcePath = os.pwd / "difftest"
}

object chiselModule extends CommonNS with HasChiselTests {
  override def millSourcePath = os.pwd

  override def moduleDeps = super.moduleDeps ++ Seq(
    difftest
  )
}
