package io.github.coenraadhuman.clap.annotation.processor;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import io.github.coenraadhuman.clap.CommandRunner;
import io.github.coenraadhuman.clap.model.CommandArgumentClassContainer;
import io.github.coenraadhuman.clap.model.CommandContainer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@RequiredArgsConstructor
public class CommandRunnerBuilder {

  private final Filer filer;
  private final String packageName;
  private final Map<String, CommandArgumentClassContainer> commandArguments;
  private final List<CommandContainer> commands;

  void generate() throws IOException {
    var isSpring = usingSpring(commands);

    var clapCommandMapper = TypeSpec.classBuilder("ClapCommandRunner")
                                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                                .addSuperinterface(CommandRunner.class);

    if (isSpring) {
      clapCommandMapper.addAnnotation(Component.class);
    }

    var executeMethod = MethodSpec.methodBuilder("execute")
                            .addModifiers(Modifier.PUBLIC)
                            .addParameter(String[].class, "args")
                            .addAnnotation(Override.class);

    executeMethod.addStatement(String.format("var mapper = new %s.mapper.ClapArgumentMapper()", packageName));
    executeMethod.addStatement("var argumentResult = mapper.map(args)");
    executeMethod.addStatement("var executed = false");

    executeMethod.beginControlFlow("if (argumentResult == null)")
        // Todo: add print function that will print the help menu for the main application.
        .addStatement("System.exit(1)")
        .endControlFlow();

    var sprintConstructor = MethodSpec.constructorBuilder()
                                .addModifiers(Modifier.PUBLIC);

    commandArguments.forEach((key, commandArgument) -> {
      var argumentInterface = ClassName.get((TypeElement) commandArgument.commandArgument().element()).canonicalName();
      var command = findCommand(argumentInterface);

      var commandVariableName =
          ClassName.get((TypeElement) command.element()).simpleName().substring(0, 1).toLowerCase()
              + ClassName.get((TypeElement) command.element()).simpleName().substring(1);

      executeMethod.beginControlFlow(
          String.format("if (argumentResult instanceof %s argument)", argumentInterface)
      );

      if (isSpring) {
        sprintConstructor.addParameter(TypeName.get(command.element().asType()), commandVariableName)
            .addStatement(String.format("this.%s = %s", commandVariableName, commandVariableName));

        clapCommandMapper.addField(TypeName.get(command.element().asType()), commandVariableName, Modifier.PRIVATE,
            Modifier.FINAL);
      } else {
        executeMethod.addStatement(String.format("var %s = new %s()",
            commandVariableName,
            ClassName.get((TypeElement) command.element()).canonicalName()
        ));
      }

      executeMethod.beginControlFlow("try")
          .addStatement(String.format("%s.process(argument)", commandVariableName))
          .addStatement("executed = true")
          .nextControlFlow("catch (Exception e)")
          .addStatement("System.out.println(argument)")
          .addStatement("System.exit(1)")
          .endControlFlow()
          .endControlFlow();
    });

    executeMethod.beginControlFlow("if (!executed)")
        .addStatement("System.exit(1)")
        .endControlFlow();

    if (isSpring) {
      clapCommandMapper.addMethod(sprintConstructor.build());
    }

    clapCommandMapper.addMethod(executeMethod.build());

    var javaFile = JavaFile.builder(String.format("%s.runner", packageName), clapCommandMapper.build()).build();

    try {
      javaFile.writeTo(filer);
    } catch (Exception e) {
      // Todo: for some reason this is invoked twice?
    }
  }

  private boolean usingSpring(List<CommandContainer> commands) {
    for (var command : commands) {
      var dependencyInjection = command.command().componentModel();
      if ("spring".equals(dependencyInjection)) {
        return true;
      }
    }
    return false;
  }


  private CommandContainer findCommand(String commandArgumentCanonicalName) {
    var retrieved = new AtomicReference<CommandContainer>();
    for (var command : commands) {
      command.element().getAnnotationMirrors().forEach(annotationMirror -> {
        annotationMirror.getElementValues().forEach(
            (executableElement, annotationValue) -> {
              if (executableElement.toString().equals("argument()")
                      && annotationValue.toString().replace(".class", "").equals(commandArgumentCanonicalName)) {
                retrieved.set(command);
              }
            }
        );
      });
    }
    if (retrieved.get() != null) {
      return retrieved.get();
    }
    throw new RuntimeException("Argument must have associated command");
  }

}
