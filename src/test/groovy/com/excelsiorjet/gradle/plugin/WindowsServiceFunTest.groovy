package com.excelsiorjet.gradle.plugin

import org.gradle.testkit.runner.TaskOutcome
import spock.lang.IgnoreIf

class WindowsServiceFunTest extends BaseFunTest {

    @IgnoreIf({!windowsServicesSupported})
    def "jetBuild task builds windows service"() {
        when:
        def result = runGradle('jetBuild')
        def rspFile = new File(appDir, "SampleService.rsp");
        File installBat = new File(appDir, "install.bat")
        File uninstallBat = new File(appDir, "uninstall.bat")

        then:
        buildExeFile.exists()
        appExeFile.exists()

        rspFile.exists();
        String rspContents = toUnixLineSeparators(rspFile.text);
        rspContents.equals(
        """-install SampleService.exe
-displayname "Sample Service"
-description "Sample Service created with Excelsior JET"
-auto
-dependence Dhcp
-dependence Dnscache
-args
arg
arg with space
"""
        )

        installBat.exists()
        uninstallBat.exists()
        zipFile.exists()

        result.task(":jetBuild").outcome == TaskOutcome.SUCCESS
    }

    public String testProjectDir() {
        return "24-windows-service"
    }

    @Override
    protected String projectName() {
        return "SampleService"
    }

    @Override
    protected String projectVersion() {
        return "1.0-SNAPSHOT"
    }
}
