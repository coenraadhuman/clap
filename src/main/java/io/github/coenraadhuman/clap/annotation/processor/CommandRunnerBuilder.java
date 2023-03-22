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
  private final String projectDescription;
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

    executeMethod.beginControlFlow("try");
    executeMethod.addStatement(String.format("var mapper = new %s.mapper.ClapArgumentMapper()", packageName));
    executeMethod.addStatement("var argumentResult = mapper.map(args)");
    executeMethod.addStatement("var executed = false");

    executeMethod.beginControlFlow("if (argumentResult == null)")
        .addStatement("System.out.println(toString())")
        .addStatement("throw new RuntimeException(\"Arguments mapper result was null\")")
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
          .addStatement("throw e")
          .endControlFlow()
          .endControlFlow();
    });

    executeMethod.beginControlFlow("if (!executed)")
        .addStatement("System.out.println(toString())")
        .addStatement("throw new RuntimeException(\"Did not execute any command\")")
        .endControlFlow();

    if (isSpring) {
      clapCommandMapper.addMethod(sprintConstructor.build());
    }

    executeMethod.nextControlFlow("catch (Exception e)")
        .addStatement("System.out.println(String.format(\"\\nError: %s\", e.getMessage()))")
        .addStatement("System.exit(1)")
        .endControlFlow();

    clapCommandMapper.addMethod(executeMethod.build());

    var helpMethod = createToString();

    clapCommandMapper.addMethod(helpMethod.build());

    var javaFile = JavaFile.builder(String.format("%s.runner", packageName), clapCommandMapper.build()).build();


    javaFile.writeTo(filer);
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

  private MethodSpec.Builder createToString() {
    var methodBuilder = MethodSpec.methodBuilder("toString")
                            .addModifiers(Modifier.PUBLIC)
                            .addAnnotation(Override.class)
                            .addStatement("var help = new StringBuilder()");

    if (projectDescription.length() > 0) {
      methodBuilder.addStatement(String.format("help.append(\"%s\\n\\n\")", projectDescription));
    }

    methodBuilder.addStatement("help.append(\"Commands:\\n\")");

    commandArguments.forEach((key, commandArgument) -> {
      var argumentInterface = ClassName.get((TypeElement) commandArgument.commandArgument().element()).canonicalName();
      var command = findCommand(argumentInterface);

      // Todo: make this dynamic and calculate width that would fit given information.
      methodBuilder.addStatement(
          "help.append(String.format(\"  %-25s %s\", \"" + commandArgument.commandArgument().annotation().input()
              + "\",\"" + command.command().description() + "\\n\"))"
      );
    });

    methodBuilder
        .addStatement("return help.toString()")
        .returns(String.class);

    return methodBuilder;
  }

}
