import com.typesafe.config.ConfigFactory
import scala.util.Try
import scoverage.ScoverageKeys

val appName = "gatekeeper-clients"

val btVersion: String = Try(ConfigFactory.load().getString("version")).getOrElse("0.1.0")

val scoverageSettings = Seq(
  ScoverageKeys.coverageExcludedPackages  := "<empty>;Reverse.*;models/.data/..*;views.*;global.*;common.*;.*(AuthService|BuildInfo|Routes).*",
  ScoverageKeys.coverageMinimum           := 80,
  ScoverageKeys.coverageFailOnMinimum     := false,
  ScoverageKeys.coverageHighlighting      := true
)

lazy val microservice = Project(appName, file("."))
  .enablePlugins(PlayScala)
  .configs(IntegrationTest)
  .settings(scoverageSettings)
  .settings(PlayKeys.playDefaultPort := 5600)
  .settings(inConfig(IntegrationTest)(Defaults.itSettings): _*)
  .settings(
    version                                       :=  btVersion,
    scalaVersion                                  :=  "2.13.5",
    organization                                  :=  "com.cjww-dev.apps",
    resolvers                                     +=  "cjww-dev" at "https://dl.bintray.com/cjww-development/releases",
    resolvers                                     +=  "mvn" at "https://repo1.maven.org/maven2",
    libraryDependencies                           ++= Seq(
      "com.cjww-dev.libs"      % "mongo-connector_2.13"       % "0.2.0",
      "com.cjww-dev.libs"      % "data-defender_2.13"         % "0.5.0",
      "com.cjww-dev.libs"      % "log-encoding_2.13"          % "0.3.0",
      "com.cjww-dev.libs"      % "feature-management_2.13"    % "2.0.1",
      "com.cjww-dev.libs"      % "inbound-outbound_2.13"      % "0.5.0",
      "io.github.nremond"      % "pbkdf2-scala_2.13"          % "0.6.5",
      "com.pauldijou"          % "jwt-core_2.13"              % "5.0.0",
      "com.nimbusds"           % "nimbus-jose-jwt"            % "9.1.2",
      "org.mockito"            % "mockito-core"               % "3.3.3"    % Test,
      "org.scalatestplus"      % "scalatestplus-mockito_2.13" % "1.0.0-M2" % Test,
      "org.scalatestplus.play" % "scalatestplus-play_2.13"    % "5.1.0"    % Test,
      "org.scalatestplus.play" % "scalatestplus-play_2.13"    % "5.1.0"    % IntegrationTest
    ),
    fork                       in IntegrationTest :=  false,
    unmanagedSourceDirectories in IntegrationTest :=  (baseDirectory in IntegrationTest)(base => Seq(base / "it")).value,
    parallelExecution          in IntegrationTest :=  false,
    fork                       in Test            :=  false,
    testForkedParallel         in Test            :=  false,
    parallelExecution          in Test            :=  false,
    logBuffered                in Test            :=  false
  )
      
