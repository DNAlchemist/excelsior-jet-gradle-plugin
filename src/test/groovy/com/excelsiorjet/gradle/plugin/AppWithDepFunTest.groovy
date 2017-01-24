package com.excelsiorjet.gradle.plugin

import org.gradle.testkit.runner.TaskOutcome

class AppWithDepFunTest extends BaseFunTest {

    def "jetBuild task builds simple application with dependency"() {
        setup:
        File prj = new File(basedir, "build/jet/build/AppWithDep.prj")
        File dep = new File(basedir, "build/jet/build/AppWithDep_jetpdb/tmpres/commons-io-1.3.2__1.jar")

        when:
        def result = runGradle('jetBuild')

        then:
        buildExeFile.exists()
        appExeFile.exists()
        zipFile.exists()
        dep.exists()
        def prjText = toUnixLineSeparators(prj.text)
        prjText.contains("""
!classpathentry lib/commons-io-1.3.2.jar
  -optimize=all
  -protect=all
!end""")

        checkStdOutContains("HelloWorld", appExeFile)

        result.task(":jetBuild").outcome == TaskOutcome.SUCCESS
    }


    @Override
    protected String testProjectDir() {
        return "02-withdependency"
    }

    @Override
    protected String projectName() {
        return "AppWithDep"
    }

    @Override
    protected String projectVersion() {
        return "1.0-SNAPSHOT"
    }
}
