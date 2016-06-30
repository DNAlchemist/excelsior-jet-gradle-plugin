[![Maven Central](https://img.shields.io/maven-central/v/com.excelsiorjet/excelsior-jet-gradle-plugin.svg)](https://maven-badges.herokuapp.com/maven-central/com.excelsiorjet/excelsior-jet-gradle-plugin)
Excelsior JET Gradle Plugin
=====

*Excelsior JET Gradle Plugin* provides Gradle users with an easy way to compile their applications
down to optimized native Windows, OS X, or Linux executables with [Excelsior JET](http://excelsiorjet.com).
Such precompiled applications start and often work faster, do not depend on the JRE,
and are as difficult to reverse engineer as if they were written in C++.

### Prerequisites
Before using this plugin, you need to install Excelsior JET.
You may find a fully functional evaluation version of Excelsior JET [here](http://www.excelsiorjet.com/evaluate).
It is free for evaluation purposes and the only limitation it has is that it expires 90 days
after installation, along with all compiled applications.

**Note:** Excelsior JET does not yet support cross-compilation, so you need to build your application on each target platform
separately. The supported platforms are Windows (32- and 64-bit), Linux (32- and 64-bit), and OS X (64-bit).


### Overview

This plugin will transform your application into an optimized native executable for the platform
on which you run Gradle, and place it into a separate directory together with all required
Excelsior JET runtime files. In addition, it can either pack that directory into a zip archive
(all platforms), create an Excelsior Installer setup (Windows and Linux only)
or an OS X application bundle/installer.

The current version of the plugin can handle two types of applications:

* **Plain Java SE applications**, i.e. applications that have a main class
and have all their dependencies explicitly listed in the JVM classpath at launch time, and

In other words, if your application can be launched using a command line
of the following form:

```
java -cp [dependencies-list] [main class]
```

and loads classes mostly from jars that are present in the `dependencies-list`, then you can use this plugin.

Excelsior JET can also compile Eclipse RCP and Tomcat Web applications and create dynamic libraries (e.g. Windows DLLs)
callable via the Invocation API.
The plugin does not yet support projects of these types nor some advanced Excelsior JET features.
We plan to cover all that functionality in the future, but if you need the plugin to support
a particular feature sooner rather than later, you can help us prioritize the roadmap
by creating a feature request [here](https://github.com/excelsior-oss/excelsior-jet-gradle-plugin/issues).

### Usage

If your project is a plain Java SE application, you need to copy and paste the following configuration into the `build.gradle` file:

```gradle
buildscript {
    def pluginVersion = '0.3.0'
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath "com.excelsiorjet:excelsior-jet-gradle-plugin:$pluginVersion"
    }
}

apply plugin: "java"
apply plugin: 'excelsiorJet'
excelsiorJet {
    mainClass = ''
}
```

set the `mainClass` parameter, and use the following command line to build the application:

```
gradlew jetBuild
```

### Excelsior JET Installation Directory Lookup

In order to do its job, the plugin needs to locate an Excelsior JET installation.
You have three ways to specify the Excelsior JET installation directory explicitly:

- add the `<jetHome>` parameter to the `<configuration>` section of the plugin
- pass the `jet.home` system property on the Gradle command line as follows:
```
gradlew jetBuild -Djet.home=[JET-Home]
```
- or set the `JET_HOME` O/S environment variable

If none of above is set, the plugin searches for an Excelsior JET installation along the `PATH`.
So if you only have one copy of Excelsior JET installed, the plugin should be able to find it on Windows right away,
and on Linux and OS X - if you have run the Excelsior JET `setenv` script prior to launching Gradle.

### Build process

The native build is performed in the `jet` subdirectory of the Gradle build directory.
First, the plugin copies the main application jar to the `jet/build` directory,
and copies all its run time dependencies to `jet/build/lib`.
Then it invokes the Excelsior JET AOT compiler to compile all those jars into a native executable.
Upon success, it copies that executable and the required Excelsior JET Runtime files
into the `jet/app` directory, binds the executable to that copy of the Runtime,
and copies the contents of the `<packageFilesDir>` directory recursively
to `jet/app`, if applicable (see "Customizing Package Content" below.)

> Your natively compiled application is ready for distribution at this point: you may copy
> the contents of the `jet/app` directory to another computer that has neither Excelsior JET nor
> the Oracle JRE installed, and the executable should work as expected.

Finally, the plugin packs the contents of the `jet/app` directory into
a zip archive named `<excelsiorJet.artifactName>.zip` so as to aid single file re-distribution.
On Windows and Linux, you can also set the `<packaging>excelsior-installer</packaging>`
configuration parameter to have the plugin create an Excelsior Installer setup instead,
and on OS X, setting `<packaging>osx-app-bundle</packaging>` will result in the creation
of an application bundle and, optionally, a native OS X installer package (`.pkg` file).

### Performing a Test Run

The plugin can run your Java application on the Excelsior JET JVM
using a JIT compiler before pre-compiling it to native code. This so-called Test Run
helps Excelsior JET:

* verify that your application can be executed successfully on the Excelsior JET JVM.
  Usually, if the Test Run completes normally, the natively compiled application also works well.
* detect the optional parts of Excelsior JET Runtime that are used by your application.
  For instance, JavaFX Webkit is not included in the resulting package by default
  due to its size, but if the application used it during a Test Run, it gets included automatically.
* collect profile information to optimize your app more effectively

To perform a Test Run, execute the following Gradle command:

```
gradle testRun
```

The plugin will place the gathered profiles in the `<project.projectDir>/src/main/jetresources` directory.
Incremental changes of application code do not typically invalidate the profiles, so
it is recommended to commit the profiles (`.usg`, `.startup`) to VCS to allow the plugin
to re-use them during automatic application builds without performing a Test Run.

It is recommended to perform a Test Run at least once before building your application.

Note: 64-bit versions of Excelsior JET do not collect `.usg` profiles yet.
      So it is recommended to perform a Test Run on the 32-bit version of Excelsior JET at least once.

The profiles will be used by the Startup Optimizer and the Global Optimizer (see below).

Note: During a Test Run, the application executes in a special profiling mode,
      so disregard its modest start-up time and performance.

### Configurations other than `excelsiorJet.mainClass`

For a complete list of parameters, look into the Javadoc of field declarations
of the
[ExcelsiorJetExtension](https://github.com/excelsior-oss/excelsior-jet-gradle-plugin/blob/master/src/main/groovy/com/excelsiorjet/gradle/plugin/ExcelsiorJetExtension.groovy) class.
Most of them have default values derived from your `build.gradle` project
such as `excelsiorJet.outputName` parameter specifying resulting executable name.

There are also two useful Windows-specific configuration parameters:

`excelsiorJet.hideConsole = true` – hide console

`excelsiorJet.icon = <icon-file>` – set executable icon (in Windows .ico format)

It is recommended to place the executable icon into a VCS, and if you place it to
`<project.projectDir>/src/main/jetresources/icon.ico`, you do not need to explicitly specify it
in the configuration. The plugin uses the location `<project.projectDir>/src/main/jetresources`
for other Excelsior JET-specific resource files (such as the EULA for Excelsior Installer setups).

#### Customizing Package Content

By default, the final package contains just the resulting executable and the necessary Excelsior JET Runtime files.
However, you may want the plugin to add other files to it: README, license, media, help files,
third-party native libraries, and so on. For that, add the following configuration parameter:

`excelsiorJet.packageFilesDir = <extra-package-files-directory>`

referencing a directory with all such extra files that you need added to the package.
The contents of the directory will be copied recursively to the final package.

By default, the plugin assumes that the extra package files reside
in the `src/main/jetresources/packagefiles` subdirectory of your project,
but you may dynamically generate the contents of that directory by means of other Gradle plugins.

#### Excelsior Installer Configurations

The plugin supports the creation of Excelsior Installer setups -
conventional installer GUIs for Windows or self-extracting archives with command-line interface
for Linux.

To create an Excelsior Installer setup, add the following configuration into the `excelsiorJet` plugin
extension:

`packaging = excelsior-installer`

**Note:** if you use the same build.gradle for all three supported platforms (Windows, OS X, and Linux),
it is recommended to use another configuration:

`packaging = native-bundle`

to create Excelsior Installer setups on Windows and Linux and an application bundle and installer on OS X.

Excelsior Installer setup, in turn, has the following configurations:

* `excelsiorInstaller.product = <product-name>` - default is *<project.name>*

* `excelsiorInstaller.vendor = <vendor-name>` -  default is *<project.group>*

* `excelsiorInstaller.version = <product-version>` - default is *<project.version>*

The above parameters are also used by Windows Version Information and OS X bundle configurations.

To further configure the Excelsior Installer setup, you need to add the following configuration section:

```gradle
excelsiorInstaller {
}
```

that has the following configuration parameters:

* `eula = <end-user-license-agreement-file>` - default is *<project.projectDir>/src/main/jetresources/eula.txt*

* `eulaEncoding = <eula-file-encoding>` - default is *autodetect*. Supported encodings are US-ASCII (plain text), UTF16-LE

* `installerSplash = <installer-splash-screen-image>` - default is *<project.projectDir>/src/main/jetresources/installerSplash.bmp*

#### Creating OS X application bundles and installers

The plugin supports the creation of OS X application bundles and installers.

To create an OS X application bundle, add the following configuration into the `excelsiorJet` plugin extension:

`packaging = "osx-app-bundle"`

**Note:** if you use the same build.gradle for all three supported platforms (Windows, OS X, and Linux), it is recommended to use another configuration:

`packaging = "native-bundle"`

to create Excelsior Installer setups on Windows and Linux and an application bundle and installer on OS X.

To configure the OS X application bundle, you need to add the following configuration section:

```gradle
osxBundle {
}
```

The values of most bundle parameters are derived automatically from the other parameters of your `build.gradle`.
The complete list of the parameters can be obtained
[here](https://github.com/excelsior-oss/excelsior-jet-api/blob/master/src/main/java/com/excelsiorjet/api/tasks/config/OSXAppBundleConfig.java).

You still need to tell the plugin where the OS X icon (`.icns` file) for your bundle is located.
Do that using the `osxBundle.icon` parameter, or simply place the icon file at
`<project.projectDir>/src/main/jetresources/icon.icns` to let the plugin pick it up automatically.

By default, the plugin will create an OS X application bundle only,
but to distribute your application to your customers you probably need to sign it and package as an
OS X installer (`.pkg` file).
The plugin enables you to do that using the following parameters under `osxBundle` section:

* `developerId = <developer-identity-certificate>` - "Developer ID Application" or "Mac App Distribution" certificate name for signing resulting OSX app bundle with `codesign` tool.
* `publisherId = <publisher-identity-certificate>` - "Developer ID Installer" or "Mac Installer Distribution"
certificate name for signing the resulting OS X Installer Package (`.pkg` file) with the `productbuild` tool.

If you do not want to expose above parameters via `build.gradle`, you may pass them as system properties
to the `gradlew` command instead, using the arguments `-Dosx.developer.id` and `-Dosx.publisher.id` respectively.
 
**Troubleshooting:** If you would like to test the created installer file on the same OS X system on which
it was built, you need to first remove the OS X application bundle created by the plugin and located
next to the installer. Otherwise, the installer will overwrite that existing OS X application bundle
instead of installing the application into the `Applications` folder.

#### Windows Version-Information Resource Configurations

On Windows, the plugin automatically adds a
[version-information resource](https://msdn.microsoft.com/en-us/library/windows/desktop/ms646981%28v=vs.85%29.aspx)
to the resulting executable. This can be disabled by specifying the following
configuration:

    excelsiorJet.addWindowsVersionInfo = false

By default, the values of version-information resource strings are derived from project settings.
The values of `product` and `vendor` configurations are used verbatim as
`ProductName` and `CompanyName` respectively;
other defaults can be changed using the following configuration parameters:

* `winVIVersion = <version-string>` - version number (both `FileVersion` and `ProductVersion` strings are set to this same value)

    **Notice:** unlike Gradle `<project.version>`, this string must have format `v1.v2.v3.v4`, where vi is a number.
    The plugin would use heuristics to derive a correct version string from the specified value if the latter
    does not meet this requirement, or from `<project.version>` if this configuration is not present.

* `winVICopyright = <legal-copyright> - `LegalCopyright` string, with default value derived from other parameters

* `winVIDescription = <executable-description>` - `FileDescription` string, default is `<project.name>`

#### Multi-app Executables

The plugin may compile more than one application into a single executable and
let you select a particular application at launch time via command line arguments.

The command line syntax of [multi-app executables](http://www.excelsior-usa.com/doc/jet/jetw011.html#0330)
is an extension of the `java` launcher command
line syntax that allows specifying the main class, VM options, Java system properties,
and the arguments of the application:

```
    Exe-name [Properties-and-options] Main-classname [App-arguments]
```

To enable the multi-app mode add the following configuration parameter:

`excelsiorJet.multiApp = true`

<a name="jvmargs"></a>
#### Defining System Properties and JVM Arguments

Unless you opted for multi-app executable generation, the resulting executable interprets
all its command line arguments as arguments of the main class you have specified.
In other words, there is no place on the application command line for an argument
setting a system property or altering JVM defaults, such as `-Dprop=value` or `-Xmx1G` .
To address this, the plugin enables you to hardwire system properties and JVM arguments
into the resulting executable using the following plugin configuration:

```gradle
jvmArgs = ["-Dprop=value", "-jvmArg"]
```

This configuration also affects Test Runs and can be used with multi-app executables as well
(relieving the user from the need to specify those arguments explicitly on the command line).

System property values may contain references to the *actual* root directory of the installed package
in the form of `$(Root)`.
For example, suppose the package contains a subdirectory `AppFiles`. You may define the following property:

`-Dmy.app.files.dir=$(Root)/AppFiles`

Then you prepare a package and install it into a certain directory on the target system.
Upon application startup, the JET Runtime replaces `$(Root)` with the absolute pathname of the installation directory.
Thus, when the installed application retrieves the value of the `my.app.files.dir` property,
it gets the full path to the `AppFiles` directory on the target system.

**Note:** most of the `-XX` options recognized by the Oracle JRE are not supported,
as they are specific to that HotSpot VM. Moreover, certain `-X` options are not supported either,
for example setting `-Xbootclasspath` or `-Xms` (initial Java heap size) makes no effect.

All in all, the JET Runtime recognizes the following standard JVM arguments:

`-ea, -da, -enableassertions, -disableassertions` - assertions control

`-esa, -dsa, -enablesystemassertions, -disablesystemassertions` - system assertions control

`-Xmx` - set maximum heap size

> **Note:** Setting maximum heap size to zero (default) enables adaptive heap sizing.
> Refer to the "Memory Management" section of the "Application Considerations" chapter 
> of the Excelsior JET User's Guide
> and [Knowledge Base Article #25](http://www.excelsiorjet.com/kb/25/)
> for more information.

`-Xss` - set maximum thread stack size

`-Xverify:all` - enable the strict verifier

`-XX:MaxDirectMemorySize` - set maximum memory size for direct buffers

`-javaagent:` - specify a Java Agent (for non-precompiled classes)

`-version` - print version information on startup

`-verbose:gc` - be verbose about garbage collection

The Excelsior JET Runtime also recognizes a handful of system properties controlling
its own behavior, such as `‑Djet.gc.ratio`.
For more information, consult the "Java System Properties / JET Runtime Specific Properties" section
of the "Application Considerations" chapter of the Excelsior JET User's Guide.

#### Startup Accelerator Configurations

The Startup Accelerator improves the startup time of applications compiled with Excelsior JET.
The plugin automatically runs the compiled application immediately after build,
collects the necessary profile information and hard-wires it into the executable just created.
The JET Runtime will then use the information to reduce the application startup time.
The Startup Accelerator is enabled by default, but you may disable it by specifying the following
configuration:

`profileStartup = false`

You may also specify the duration of the profiling session in seconds by specifying the following
configuration:

`profileStartupTimeout = <duration-in-seconds>`

As soon as the specified period elapses, profiling stops and the application is automatically terminated,
so ensure that the timeout value is large enough to capture all actions the application normally carries out
during startup. (It is safe to close the application manually if the profiling period proves to be excessively long.)

#### Global Optimizer

The 32-bit versions of Excelsior JET feature the Global Optimizer - a powerful facility that has several
important advantages over the default compilation mode:

* single component linking yields an executable that does not require the dynamic libraries
  containing the standard Java library classes,
  thus reducing the size of the installation package and the disk footprint of the compiled application
* global optimizations improve application performance and reduce the startup time and memory usage

By default, Excelsior JET uses the *dynamic link model*. It only compiles application classes, 
linking them into an executable that depends on dynamic libraries containing precompiled
Java SE platform classes. These dynamic libraries, found in the JET Runtime, have to be
distributed together with the executable.

The Global Optimizer detects the platform classes that are actually used by the application
and compiles them along with application classes into a single executable.
Even though the resulting binary occupies more disk space compared with the one built
in the default mode, it no longer requires the dynamic libraries with platform classes.
This results, among other benefits, in a considerable reduction of the application
installation package size.

To enable the Global Optimizer, add the following configuration parameter:

`globalOptimizer = true`

**Note:** performing a Test Run is mandatory if the Global Optimizer is enabled.

#### Java Runtime Slim-Down Configurations

The 32-bit versions of Excelsior JET feature Java Runtime Slim-Down, a unique
Java application deployment model delivering a significant reduction
of application download size and disk footprint.

The key idea is to select the components of the Java SE API that are not used by the application,
and exclude them from the installation altogether. Such components are called *detached*.
For example, if your application does not use any of Swing, AWT, CORBA or, say, JNDI API,
Excelsior JET enables you to easily exclude from the main setup package the standard library
classes implementing those APIs and the associated files, placing them in a separate *detached package*.

The detached package should be placed on a Web server so that the JET Runtime could download it
if the deployed application attempts to use any of the detached components via JNI or the Reflection API.

To enable Java Runtime Slim-Down, copy and paste the following plugin configuration:

```gradle
javaRuntimeSlimDown {
    detachedBaseURL = ''
}
```

and specify the base URL of the location where you plan to place the detached package, e.g.
`http://www.example.com/download/myapp/detached/`.

By default, the plugin automatically detects which Java SE APIs your application does not use
and detaches the respective JET Runtime components from the installation package.
Alternatively, you may enforce detaching of particular components using the following parameter
under the `javaRuntimeSlimDown` configuration section:

`detachComponents = <comma-separated list of APIs>`

Available detachable components: `corba, management, xml, jndi, jdbc, awt/java2d, swing, jsound, rmi, jax-ws`

At the end of the build process, the plugin places the detached package in the `jet` subdirectory
of the Gradle target build directory. You may configure its name with the `detachedPackage` parameter
of the `javaRuntimeSlimDown` section (by default the name is `artifactName.pkl`).

Do not forget to upload the detached package to the location specified in `detachedBaseURL`
above before deploying your application to end-users.

**Note:** Enabling Java Runtime Slim-Down automatically enables the Global Optimizer, 
          so performing a Test Run is mandatory for Java Runtime Slim-Down as well.

**Fixed issue:** Java Runtime Slim-Down did not work with the `excelsior-installer` packaging type 
                 due to a bug in Excelsior JET. This issue is fixed in Excelsior JET 11 Maintenance Pack 2.

#### Creating Trial Versions

You can create a trial version of your Java application that will expire in a specified number of days
after the build date of the executable, or on a fixed date.
Once the trial period is over, the application will refuse to start up,
displaying a custom message.

To enable trial version generation, copy and paste into your `build.gradle` file the following plugin configuration:

```gradle
trialVersion {
    expireInDays = 0
    expireMessage = ''
}
```

and specify the number of calendar days after the build date when you want the application
to expire, and the error message that the expired binary should display to the user on a launch attempt.

You can also set a particular, fixed expiration date by using the `expireDate` parameter
instead of `expireInDays`. The format of the `expireDate` parameter value
is *ddMMMyyyy*, for example `15Sep2020`.

**Note:** If you choose the `excelsior-installer` `packaging` type, the generated setup
package will also expire, displaying the same message to the user.

One common usage scenario of this functionality is setting the hard expiration date further into the future,
while using some other mechanism to enforce a (shorter) trial period.
Typically, you would set the hard expiration date somewhat beyond the planned release 
date of the next version of your application. This way, you would ensure that nobody uses
an outdated trial copy for evaluation.

#### Data protection

If you do not wish constant data, such as reflection info, Java string literals, or packed resource files,
to be visible in the resulting executable, enable data protection by specifying the following configuration:

`protectData = true`

For more details on data protection, refer to the "Data Protection" section of
the "Intellectual Property Protection" chapter of the Excelsior JET User's Guide.


## Release Notes

Version 0.3.0 (01-Jul-2016)
* Support of Excelsior Installer setup generation
* Windows Version Information generation
* Support of multi-app executables
* Startup Accelerator supported and enabled by default
* Test Run Task implemented that enables:
   - running an application on the Excelsior JET JVM before pre-compiling it to native code
   - gathering application execution profiles to enable the Startup Optimizer
* `<optRtFiles>` parameter introduced to add optional JET runtime components
* Global Optimizer
* Java Runtime Slim-Down
* Reduced the download size and disk footprint of resulting packages by means of supporting:
* `<packageFilesDir>` parameter introduced to add extra files to the final package
* Trial version generation is supported
* `<jvmArgs>` parameter introduced to define system properties and JVM arguments
* `<protectData>` parameter added to enable data protection
* Mac OS X application bundles and installers support

Version 0.1.0 (24-Jun-2016)
* Initial release supporting compilation of the Gradle Project with all dependencies into native executable
and placing it into a separate directory with required Excelsior JET runtime files.

## Roadmap

Even though we are going to base the plugin development on your feedback in the future, we have our own short-term plan as well.
So the next few releases will add the following features:

* Windows services support.
* Multi-component support: building dependencies into separate native libraries
                           to reuse them across multiple Gradle project builds
                           so as to reduce overall compilation time
* Code signing.

Note that the order of appearance of these features is not fixed and can be adjusted based on your feedback.