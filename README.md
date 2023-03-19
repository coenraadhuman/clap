# clap

Command Line Argument Parser for Java 17

Note: This is a initial release of `clap`.

### Getting Started

Add to your pom.xml the dependency and reference to the annotation processor on the compiler.

```xml

<dependencies>
    <dependency>
        <groupId>io.github.coenraadhuman</groupId>
        <artifactId>clap</artifactId>
        <version>0.1.0</version>
    </dependency>
</dependencies>

<build>
<plugins>
    <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-compiler-plugin</artifactId>
        <configuration>
            <source>${maven.compiler.source}</source>
            <target>${maven.compiler.target}</target>
            <parameters>true</parameters>
            <annotationProcessorPaths>
                <path>
                    <groupId>org.springframework</groupId>
                    <artifactId>spring-context-indexer</artifactId>
                    <version>${spring-framework.version}</version>
                </path>
            </annotationProcessorPaths>
        </configuration>
    </plugin>
</plugins>
</build>

```

Now to create your command-line argument and command it's respective command:

#### Argument:

```java

@CommandArgument(
    input = "version" // This is the command for the application.
)
// Must extend CommandArgumentProcessor, generated source relies on it.
public interface VersionCommandArgument extends CommandArgumentProcessor {

    @Option(
        info = "Specify the path of the git project.", // Description of option.
        shortInput = "-pa", // Short option to be used by end-user.
        longInput = "--path", // Long option to be used by end-user.
        providesValue = true // Indicates whether option is a flag or provides a value on next argument provided to application.
    )
    String path();

    @Option(
        info = "Bump current project version with a major increment.",
        shortInput = "-m",
        longInput = "--major",
        providesValue = false
    )
    boolean major();

    @Option(
        info = "Bump current project version with a minor increment.",
        shortInput = "-mi",
        longInput = "--minor",
        providesValue = false
    )
    boolean minor();

    @Option(
        info = "Bump current project version with a patch increment.",
        shortInput = "-p",
        longInput = "--patch",
        providesValue = false
    )
    boolean patch();

    @Option(
        info = "Mechanism to provide the latest commit made to be included in project version calculation.",
        shortInput = "-c",
        longInput = "--git-hook-commit",
        providesValue = true
    )
    String gitHookCommit();

}
```

#### Command with Spring DI:

```java

@Component // Still need to mark the command as a Spring component.
@RequiredArgsConstructor
@Command(
    argument = VersionCommandArgument.class, // Class of the argument the command uses.
    componentModel = "spring" // Indicates if Spring injection should be used.
)
// Must extend CommandProcessor, generated source relies on it.
public final class VersionCommand implements CommandProcessor<VersionCommandArgument> {

    private final SpringComponent springComponent;

    @Override
    public void process(VersionCommandArgument commandArgument) {
        System.out.println(commandArgument);
    }

}
```

#### Runner with Spring DI:

```java

@Slf4j
@Component
@RequiredArgsConstructor
public class ArgumentRunner implements CommandLineRunner {

    private final CommandRunner runner;

    @Override
    public void run(String[] arguments) {
        // Will map and execute your selected command otherwise it will print out a help menu.
        runner.execute(arguments);
    }

}
```

#### Command without dependency injection:

```java

@Command(
    argument = VersionCommandArgument.class // Class of the argument the command uses.
)
// Must extend CommandProcessor, generated source relies on it.
public final class VersionCommand implements CommandProcessor<VersionCommandArgument> {

    // Can't have RequiredArgsConstructor.

    @Override
    public void process(VersionCommandArgument commandArgument) {
        System.out.println(commandArgument);
    }

}
```

#### Runner without dependency injection:

```java

@Slf4j
public class ArgumentRunner implements CommandLineRunner {

    private final CommandRunner runner = new ClapCommandRunner();

    @Override
    public void run(String[] arguments) {
        runner.execute(arguments);
    }

}
```