= Closure Templates (Soy) Maven Plugin

Plugin to make Google's [Closure Templates](https://github.com/google/closure-templates) easier to use with Maven. 

For example by default the `SoyToJbcSrcCompiler` outputs a JAR file which is hard to use with an IDE (such as IntelliJ). It also requires manual intervention to get the JAR into the deliverable (for example via the Maven shade plugin). The `soy-to-bytecode` goal makes the class files of the `SoyToJbcSrcCompiler` directly available under `target/classes`.

Example usage:

```xml
<plugin>
    <groupId>nl.serviceplanet.maven</groupId>
    <artifactId>closure-templates-maven-plugin</artifactId>
    <version>1.0.0-SNAPSHOT</version>
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
