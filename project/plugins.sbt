resolvers += Resolver.url("HMRC-open-artefacts-ivy2", url("https://open.artefacts.tax.service.gov.uk/ivy2"))(
  Resolver.ivyStylePatterns
)
resolvers += Resolver.typesafeRepo("releases")
libraryDependencySchemes += "org.typelevel" %% "cats-core"             % VersionScheme.Always
libraryDependencySchemes ++= Seq("org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always)

addSbtPlugin("uk.gov.hmrc"                   % "sbt-auto-build"        % "3.20.0")
addSbtPlugin("uk.gov.hmrc"                   % "sbt-distributables"    % "2.5.0")
addSbtPlugin("org.playframework"             % "sbt-plugin"            % "3.0.1")
addSbtPlugin("org.scoverage"                 % "sbt-scoverage"         % "2.0.10")
addSbtPlugin("com.github.sbt"                % "sbt-gzip"              % "2.0.0")
addSbtPlugin("io.github.irundaia"            % "sbt-sassify"           % "1.5.2")
addSbtPlugin("org.scalastyle"               %% "scalastyle-sbt-plugin" % "1.0.0" exclude ("org.scala-lang.modules", "scala-xml_2.12"))
addSbtPlugin("org.scalameta"                 % "sbt-scalafmt"          % "2.5.2")
addSbtPlugin("com.github.sbt"                % "sbt-concat"            % "1.0.0")
addSbtPlugin("com.typesafe.sbt"              % "sbt-uglify"            % "2.0.0")
addSbtPlugin("com.github.sbt"                % "sbt-digest"            % "2.0.0")
