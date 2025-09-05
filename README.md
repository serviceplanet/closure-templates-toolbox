# Closure Template (Soy) Toolbox

This repository contains various tools for use with Google's [Closure Templates](https://github.com/google/closure-templates).

## Maven Plugin

Maven plugin to make Google's [Closure Templates](https://github.com/google/closure-templates) easier to use with Maven. 

### 'soy-to-bytecode' goal

By default the `SoyToJbcSrcCompiler` outputs a JAR file which is hard to use with an IDE (such as IntelliJ). It also requires manual intervention to get the JAR into the deliverable (for example via the Maven shade plugin). The `soy-to-bytecode` goal makes the class files of the `SoyToJbcSrcCompiler` directly available under `target/classes`.

Example usage:

```xml
<plugin>
    <groupId>nl.serviceplanet.maven</groupId>
    <artifactId>closure-templates-maven-plugin</artifactId>
    <version>[REPLACE WITH VERSION]</version>
    <executions>
        <execution>
            <id>soy-to-bytecode</id>
            <goals>
                <goal>soy-to-bytecode</goal>
            </goals>
            <phase>verify</phase>
            <configuration>
                <soySources>${project.basedir}/src/main/resources/template1.soy,${project.basedir}/src/main/resources/template2.soy</soySources>
            </configuration>
        </execution>
    </executions>
</plugin>
```

### 'soy-to-icu-properties' goal

```xml
<plugin>
    <groupId>nl.serviceplanet.maven</groupId>
    <artifactId>closure-templates-maven-plugin</artifactId>
    <version>[REPLACE WITH VERSION]</version>
    <execution>
        <id>soy-to-icu</id>
        <goals>
            <goal>soy-to-icu-properties</goal>
        </goals>
        <phase>generate-sources</phase>
        <configuration>
            <soySourcesBasePath>${soy.root}</soySourcesBasePath>
            <outputFile>${icu.root}/messages.properties</outputFile>
            <sourceLocaleString>nl-NL</sourceLocaleString>
            <messagePlugin>${soy.plugin}</messagePlugin>
        </configuration>
    </execution>
</plugin>
```

### Implementation

This Maven plugin does some voodoo with reflection and [jnr-posix](https://github.com/jnr/jnr-posix) in order to work;

* The Soy compilers are intended to be called from the CLI. For the most part there is no API tooling (like this plugin) can use. Furthermore all API which we would need to call is locked down. For example a lot of classes and methods are package private, packages are sealed (meaning you can't simply create a `com.google.soy` package yourself).
  * While we can only guess Google probably uses these compilers with Bazel. Where it simply invokes them via the CLI.
* Invoking the main method is not going to work because the Soy compilers make a call to `System.exit`. Meaning the Maven process will exit if this plugin calls the main method of a Soy compiler.
* [jnr-posix](https://github.com/jnr/jnr-posix) Is used because the Soy header compiler needs to be started from a specific working directory because the path passed via the `--srcs` command determines the unique name for the template. In Java you can't change the working directory of the current process. Simply changing the `user.dir` property turned out not to be enough to fool the Soy compiler.

Ideally the Soy compilers would have some form of API for tooling to use. 

An alternative to this implementation would be to start a separate process and JVM with the Soy compiler. The disadvantage of this is that one would need to determine what the correct `java` binary is to start the JVM. In for example a CI container this might be different then a local machine.
