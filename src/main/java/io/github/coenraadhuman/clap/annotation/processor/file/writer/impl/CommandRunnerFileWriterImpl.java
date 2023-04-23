package io.github.coenraadhuman.clap.annotation.processor.file.writer.impl;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import io.github.coenraadhuman.clap.CommandRunner;
import io.github.coenraadhuman.clap.annotation.processor.file.writer.common.MultipleCommandsFileWriterBase;
import io.github.coenraadhuman.clap.model.CommandInformation;
import io.github.coenraadhuman.clap.model.OptionValue;
import io.github.coenraadhuman.clap.model.ProjectInformation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static io.github.coenraadhuman.clap.common.SpaceConstants.AFTER_SHORT_INPUT;
import static io.github.coenraadhuman.clap.common.SpaceConstants.BEFORE_DESCRIPTION;

@RequiredArgsConstructor
public class CommandRunnerFileWriterImpl extends MultipleCommandsFileWriterBase {

  private final Filer filer;

  @Override
  protected void process(ProjectInformation projectInformation) throws IOException {
    var isSpring = usingSpring(projectInformation.commands());

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

    executeMethod.beginControlFlow("if (args.length != 0)");
    executeMethod.beginControlFlow("if (!args[0].equals(\"-h\") && !args[0].equals(\"--help\"))");

    executeMethod.addStatement(
        String.format("var mapper = new %s.mapper.ClapArgumentMapper()", projectInformation.projectPackage())
    );

    executeMethod.addStatement("var argumentResult = mapper.map(args)");
    executeMethod.addStatement("var executed = false");

    executeMethod.beginControlFlow("if (argumentResult == null)")
        .addStatement("System.out.println(toString())")
        .addStatement("throw new RuntimeException(\"Arguments mapper result was null\")")
        .endControlFlow();

    var sprintConstructor = MethodSpec.constructorBuilder()
        .addModifiers(Modifier.PUBLIC);

    projectInformation.commands().forEach(command -> {
      var commandVariableName =
          ClassName.get((TypeElement) command.command().element()).simpleName().substring(0, 1).toLowerCase()
              + ClassName.get((TypeElement) command.command().element()).simpleName().substring(1);

      executeMethod.beginControlFlow(
          String.format("if (argumentResult instanceof %s argument)", command.argumentClassSimpleName())
      );

      if (isSpring) {
        sprintConstructor.addParameter(TypeName.get(command.command().element().asType()), commandVariableName)
            .addStatement(String.format("this.%s = %s", commandVariableName, commandVariableName));

        clapCommandMapper.addField(TypeName.get(command.command().element().asType()),
            commandVariableName,
            Modifier.PRIVATE,
            Modifier.FINAL
        );
      } else {
        executeMethod.addStatement(String.format("var %s = new %s()",
            commandVariableName,
            ClassName.get((TypeElement) command.command().element()).canonicalName()
        ));
      }
      executeMethod
          .beginControlFlow("if (argument.help())")
          .addStatement("System.out.println(argument)")
          .addStatement("executed = true")
          .nextControlFlow("else")
          .beginControlFlow("try")
          .addStatement(String.format("%s.process(argument)", commandVariableName))
          .addStatement("executed = true")
          .nextControlFlow("catch (Exception e)")
          .addStatement("System.out.println(argument)")
          .addStatement("throw e")
          .endControlFlow()
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

    executeMethod.nextControlFlow("else")
        .addStatement("System.out.println(toString())")
        .endControlFlow();

    executeMethod.nextControlFlow("else")
        .addStatement("System.out.println(toString())")
        .endControlFlow();

    executeMethod.nextControlFlow("catch (Exception e)")
        .addStatement("System.out.println(String.format(\"\\nError: %s\", e.getMessage()))")
        .addStatement("System.exit(1)")
        .endControlFlow();

    clapCommandMapper.addMethod(executeMethod.build());

    var helpMethod = createToString(projectInformation);

    clapCommandMapper.addMethod(helpMethod.build());

    var javaFile = JavaFile.builder(
        String.format("%s.runner", projectInformation.projectPackage()), clapCommandMapper.build()
    ).build();


    javaFile.writeTo(filer);
  }

  private boolean usingSpring(List<CommandInformation> commands) {
    for (var command : commands) {
      var dependencyInjection = command.command().command().componentModel();
      if ("spring".equals(dependencyInjection)) {
        return true;
      }
    }
    return false;
  }

  private MethodSpec.Builder createToString(ProjectInformation projectInformation) {
    var methodBuilder = MethodSpec.methodBuilder("toString")
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(Override.class)
        .addStatement("var help = new StringBuilder()");

    var largestLongInput = 0;
    var largestShortInput = 0;
    var largestOption = 0;
    var largestCommand = 0;

    if (projectInformation.projectDescription().length() > 0) {
      methodBuilder.addStatement(String.format("help.append(\"%s\\n\\n\")", projectInformation.projectDescription()));
    }

    methodBuilder.addStatement("help.append(\"Commands:\\n\")");

    for (var command : projectInformation.commands()) {
      largestCommand = Math.max(largestCommand, command.argument().annotation().input().length());
    }

    var optionValues = new ArrayList<OptionValue>();

    optionValues.add(
        new OptionValue(
            "Prints help",
            "-h",
            "--help",
            false
        )
    );

    for (var optionValue : optionValues) {
      largestLongInput = Math.max(largestLongInput, optionValue.longInput().length());
      largestShortInput = Math.max(largestShortInput, optionValue.shortInput().replace("-", "").length());
      largestOption = Math.max(
          largestOption,
          (optionValue.shortInput().length() + AFTER_SHORT_INPUT + optionValue.longInput().length())
      );
    }

    largestCommand = Math.max(largestCommand, largestOption);

    for (int i = 0; i < projectInformation.commands().size(); i++) {
      methodBuilder.addStatement(
          "help.append(String.format(\"  %-" + (largestCommand + BEFORE_DESCRIPTION) + "s %s\", \""
              + projectInformation.commands().get(i).argument().annotation().input()
              + "\",\"" + projectInformation.commands().get(i).command().command().description()
              + (i == (projectInformation.commands().size() - 1) ? "\\n\\n\"))" : "\\n\"))")
      );
    }

    final int finalLargestShortInput = largestShortInput;
    optionValues.sort((optionValueA, optionValueB) -> optionValueA.compareTo(
        optionValueB.toOptionValueComparer(finalLargestShortInput))
    );

    methodBuilder.addStatement("help.append(\"Options:\\n\")");

    for (var optionValue : optionValues) {
      methodBuilder.addStatement(
          "help.append(String.format(\"  %-" + (largestShortInput + AFTER_SHORT_INPUT) + "s%-"
              + (largestLongInput + BEFORE_DESCRIPTION) + "s %s\", \""
              + optionValue.shortInput() + ",\",\""
              + optionValue.longInput()
              + "\",\"" + optionValue.description() + "\\n\"))"
      );
    }

    methodBuilder
        .addStatement("return help.toString()")
        .returns(String.class);

    return methodBuilder;
  }

}
