// adds repos for gradle plugin resolution and ensures github creds are provided to the build
apply {
  from("https://raw.githubusercontent.com/VEuPathDB/lib-gradle-container-utils/v4.8.3/includes/common.settings.gradle.kts")
}

val core = file("../lib-jaxrs-container-core");
if (core.exists()) {
  include(":core")
  project(":core").projectDir = core
}

val edaCommon = file("../EdaCommon");
if (edaCommon.exists()) {
  include(":edaCommon")
  project(":edaCommon").projectDir = edaCommon
}

val libSubsetting = file("../lib-subsetting-build")
if (libSubsetting.exists()) {
  include(":libSubsetting")
  project(":libSubsetting").projectDir = libSubsetting
}
