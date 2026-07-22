# Closure Template (Soy) Toolbox

This repository contains various tools for use with Google's [Closure Templates](https://github.com/google/closure-templates). It provide the following things;

* [Localization (L10n) support](#localization-l10n-support)
* [Toggles (feature flags) support](#toggles-feature-flags-support)
* [Maven Plugins](#maven-plugins)
  * [Generate Bytecode from Soy](#soy-to-bytecode-goal)
  * [Extract Soy translation messages](#soy-to-icu-properties-goal)

## Localization (L10n) support

### Why this exists

Closure Templates has excellent i18n primitives (`{msg}`, `{plural}`, `{select}`, etc.), but the open-source release ships without a usable translation pipeline for them:

* Google's internal message formats (XMB / XTB) and their Translation Console integration were never open-sourced (see [closure-templates#146](https://github.com/google/closure-templates/issues/146)).
* The only `SoyMsgPlugin` bundled with Closure Templates is XLIFF 1.2, and its generator throws a `RuntimeException` ("Xliff doesn't support plurals or genders") as soon as a message contains a plural, select or gender.

So out of the box there is no way to get a `{plural}` or `{select}` message extracted, translated and rendered server-side. This toolbox fills that gap.

### What this toolbox provides

* `PropertiesIcuMsgPlugin` is a `SoyMsgPlugin` which writes and reads Java properties files in which each key is the Soy message ID (the stable content fingerprint computed by the Soy compiler) and each value is an [ICU MessageFormat](https://unicode-org.github.io/icu/userguide/format_parse/messages/) string. Plural, select and (nested) gender messages are fully supported. Using the same embedded ICU representation Soy uses internally.
* The [`soy-to-icu-properties`](#soy-to-icu-properties-goal) Maven goal, which runs Soy's `SoyMsgExtractor` with this plugin at build time.
* Runtime helpers for the rendering side:
  * `SoyUtil.loadIcuMsgBundle(url, locale, tag, failOnError)` loads a translated properties file into a `SoyMsgBundle`. The locale is required for messages with plurals, because Soy selects the CLDR plural category (`zero` / `one` / `two` / `few` / `many` / `other`) based on the bundle's locale.
  * `CombinedSoyMsgBundle` merges multiple bundles of the same locale into one.
  * `VerboseSoyMsgBundle` wraps a bundle and logs (or fails on) message lookups which have no translation. Useful to detect drift between the templates and the translation files.

### Workflow

1. At build time, extract the messages from the templates into a source-locale properties file using the `soy-to-icu-properties` Maven goal.
2. Import that file into your translation management system as ICU messages. We use [Tolgee](https://tolgee.io); inside plural blocks the plural variable is emitted as ICU `#`, so that Tolgee's machine translation expands the number into concrete values and picks the correct plural category per locale.
3. Export a translated properties file per locale.
4. At runtime, load each file with `SoyUtil.loadIcuMsgBundle(...)` and pass the bundle matching the request's locale to the renderer via `.setMsgBundle(bundle)`. This works with both SoySauce and Tofu.

Example of an extracted file:

```properties
# Asset vendor
9612312914948721=Vendor: {ASSET_VENDOR}

# Asset quantity
1619791968919851692={QUANTITY_1,plural,=1{# asset}other{# assets}}
```

### Things to be aware of

* Message IDs are content fingerprints, not hand-picked keys. Changing a message's text, placeholders or `meaning` attribute changes its ID, which orphans the existing translation (translation memory in your translation management system, such as Tolgee, might recover it). Changing only the `desc` attribute does *not* change the ID. Soy's `alternateId` msg attribute can be used to keep serving the old translation while the new one is being translated.
* The ICU plural `offset:` feature (Soy's `{plural $n offset="..."}` with `remainder()`) is not supported.
* Properties files carry no locale metadata, while Soy needs a locale to apply plural rules. Therefor `SoyUtil.loadIcuMsgBundle` injects it via a `# SOY-BUNDLE-LOCALE <tag>` comment at the top of the file.

## Toggles (feature flags) support

### Why this exists

Closure Templates has a first-class feature flag primitive called a _toggle_. A `.toggles` file is a plain newline-delimited list of flag names, which a template imports and then uses as a `bool`:

```soy
{namespace nl.serviceplanet.app}

import {NEW_CHECKOUT} from 'nl/serviceplanet/app/feature.toggles';

{template checkout}
    {if NEW_CHECKOUT}New checkout{else}Old checkout{/if}
{/template}
```

The compiler only knows about a toggles file if it is passed via the `--togglesFiles` flag. On the rendering side toggles are evaluated through the renderer's _active mod selector_. Which works on both SoySauce and Tofu.

### What this toolbox provides

* The `togglesFiles` and `togglesFilesBasePath` parameters on all Maven goals, which pass the toggles files to the Soy compiler.
* `ToggleFile` reads a `.toggles` file. It also builds the registry needed to compile templates with toggle imports programmatically (`SoyUtil.compileSoyFile(filename, toggleFile)`), which the compiler otherwise only accepts via the CLI flag.
* `SoyToggles` builds the `Predicate<String>` the renderer wants, checked against the toggles the templates actually declare.

### Workflow

1. Put a `.toggles` file next to your templates and import the flags you need.
2. Point the Maven goals at it, so the compiler can resolve the imports:

```xml
<configuration>
    <workdir>${soy.root}</workdir>
    <soySourcesBasePath>.</soySourcesBasePath>
    <togglesFilesBasePath>.</togglesFilesBasePath>
</configuration>
```

3. At runtime, tell the renderer which flags are on:

```java
ToggleFile featureToggles = ToggleFile.fromClasspath("nl/serviceplanet/app/feature.toggles");

SoyToggles toggles = SoyToggles.builder()
        .declareFrom(featureToggles)
        .enable("NEW_CHECKOUT")
        .build();

sauce.renderTemplate(templateName)
        .setData(data)
        .setActiveModSelector(toggles.activeModSelector())
        .renderHtml();
```

### Things to be aware of

* Toggles and mods share one namespace.
* A toggles file supports no syntax beyond one name per line. It does *not* support comments for example.
* Toggles are only usable on the Java backends.
  * The JS/incremental-DOM backend emits a `goog.require` for a module name matching Google's internal `ts_toggle_lib` build rule, which does not exist outside Google. The Python backend rejects toggle imports outright.

## Maven Plugins

Maven plugins to make Google's [Closure Templates](https://github.com/google/closure-templates) easier to use with Maven.

All goals share the parameters of the Soy compiler they wrap, such as `soySources` / `soySourcesBasePath`, `workdir`, the proto and header dependency parameters, and `togglesFiles` / `togglesFilesBasePath` (see [Toggles support](#toggles-feature-flags-support)).


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

Extracts all `{msg}` messages from the given Soy templates into a Java properties file with ICU MessageFormat values, keyed by Soy message ID. This is the extraction half of the translation pipeline described in [Localization (L10n) support](#localization-l10n-support).

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
