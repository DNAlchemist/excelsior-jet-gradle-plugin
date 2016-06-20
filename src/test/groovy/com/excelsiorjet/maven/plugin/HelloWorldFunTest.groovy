package com.excelsiorjet.maven.plugin

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome

class HelloWorldFunTest extends BaseFunTest {

    def "jetBuild task builds simple application"() {
        given:
        Utils.copyDirectoryContents(originalProjectDir, basedir.toPath())

        when:
        def result = GradleRunner.create()
                .withProjectDir(basedir)
                .withArguments("-DexcelsiorJetPluginVersion=" + pluginVersion, 'jetBuild')
                .withDebug(true)
                .build()

        then:
        exeFile.exists()
        zipFile.exists()
        checkStdOutContains(exeFile, "Hello World")
        result.task(":jetBuild").outcome == TaskOutcome.SUCCESS
    }


    @Override
    protected String testProjectDir() {
        return "01-helloworld"
    }

    @Override
    protected String projectName() {
        return "HelloWorld"
    }
}
