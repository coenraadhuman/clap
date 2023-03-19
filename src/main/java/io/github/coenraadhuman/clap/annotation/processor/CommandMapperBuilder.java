package io.github.coenraadhuman.clap.annotation.processor;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import io.github.coenraadhuman.clap.CommandArgumentProcessor;
import io.github.coenraadhuman.clap.CommandMapper;
import io.github.coenraadhuman.clap.mapper.CommandMapperBase;
import io.github.coenraadhuman.clap.model.CommandArgumentClassContainer;
import lombok.RequiredArgsConstructor;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import java.io.IOException;
import java.util.Map;

@RequiredArgsConstructor
public class CommandMapperBuilder {

  private final Filer filer;
  private final String packageName;
  private final Map<String, CommandArgumentClassContainer> commandArguments;

  void generate() throws IOException {
    var mapMethod = MethodSpec.methodBuilder("map")
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(String[].class, "args")
                        .addAnnotation(Override.class)
                        .returns(CommandArgumentProcessor.class);

    mapMethod.beginControlFlow("if (args.length == 0)")
        .addStatement("return null")
        .endControlFlow();

    commandArguments.forEach((key, commandArgument) -> {
      mapMethod.beginControlFlow(
          String.format("if (\"%s\".equals(args[0]))", commandArgument.commandArgument().annotation().input())
      );

      mapMethod.addStatement(String.format("var command = new %sImpl()",
          ClassName.get((TypeElement) commandArgument.commandArgument().element()).canonicalName()));

      for (var option : commandArgument.options()) {
        if (option.annotation().providesValue()) {
          mapMethod.addStatement(String.format("command.%s = findArgumentProvidedValue(args, \"%s\", \"%s\")",
              option.element().getSimpleName().toString(),
              option.annotation().shortInput(),
              option.annotation().longInput()
          ));
        } else {
          mapMethod.addStatement(String.format("command.%s = findArgument(args, \"%s\", \"%s\")",
              option.element().getSimpleName().toString(),
              option.annotation().shortInput(),
              option.annotation().longInput()
          ));
        }
      }

      mapMethod.addStatement("return command")
          .endControlFlow();

    });

    mapMethod.addStatement("return null");

    var clapCommandMapper = TypeSpec.classBuilder("ClapArgumentMapper")
                                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                                .superclass(CommandMapperBase.class)
                                .addSuperinterface(CommandMapper.class)
                                .addMethod(mapMethod.build());


    var javaFile = JavaFile.builder(packageName, clapCommandMapper.build()).build();

    try {
      javaFile.writeTo(filer);
    } catch (Exception e) {
      // Todo: for some reason this is invoked twice?
    }
  }

}
