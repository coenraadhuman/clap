package io.github.coenraadhuman.clap.annotation.processor.file.writer.impl;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import io.github.coenraadhuman.clap.CommandArgumentProcessor;
import io.github.coenraadhuman.clap.CommandMapper;
import io.github.coenraadhuman.clap.annotation.processor.file.writer.common.MultipleCommandsFileWriterBase;
import io.github.coenraadhuman.clap.mapper.CommandMapperBase;
import io.github.coenraadhuman.clap.model.ProjectInformation;
import lombok.RequiredArgsConstructor;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import java.io.IOException;

@RequiredArgsConstructor
public class CommandMapperFileWriterImpl extends MultipleCommandsFileWriterBase {

  private final Filer filer;

  @Override
  protected void process(ProjectInformation projectInformation) throws IOException {
    var mapMethod = MethodSpec.methodBuilder("map")
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(String[].class, "args")
                        .addAnnotation(Override.class)
                        .returns(CommandArgumentProcessor.class);

    mapMethod.beginControlFlow("if (args.length == 0)")
        .addStatement("return null")
        .endControlFlow();

    projectInformation.commands().forEach(command -> {
      mapMethod.beginControlFlow(
          String.format("if (\"%s\".equals(args[0]))", command.argument().annotation().input())
      );

      mapMethod.addStatement(String.format("var command = new %sImpl()",
          ClassName.get((TypeElement) command.argument().element()).canonicalName()));

      for (var option : command.options()) {
        if (option.annotation().providesValue()) {
          mapMethod.addStatement(String.format("command.%s = findOptionProvidedValue(args, \"%s\", \"%s\")",
              option.element().getSimpleName().toString(),
              option.annotation().shortInput(),
              option.annotation().longInput()
          ));
          mapMethod.beginControlFlow(
                  String.format("if (command.%s.provided() && command.%s.value() == null)",
                      option.element().getSimpleName().toString(),
                      option.element().getSimpleName().toString()
                  ))
              .addStatement("System.out.println(command)")
              .addStatement(String.format("throw new RuntimeException(\"Value not provided for option: %s / %s\")",
                  option.annotation().shortInput(),
                  option.annotation().longInput()
              ))
              .endControlFlow();
        } else {
          mapMethod.addStatement(String.format("command.%s = findOption(args, \"%s\", \"%s\")",
              option.element().getSimpleName().toString(),
              option.annotation().shortInput(),
              option.annotation().longInput()
          ));
        }
      }

      mapMethod.addStatement("command.help = findOption(args, \"-h\", \"--help\")");

      mapMethod.addStatement("return command")
          .endControlFlow();

    });

    mapMethod.addStatement("return null");

    var clapCommandMapper = TypeSpec.classBuilder("ClapArgumentMapper")
                                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                                .superclass(CommandMapperBase.class)
                                .addSuperinterface(CommandMapper.class)
                                .addMethod(mapMethod.build());


    var javaFile = JavaFile.builder(
        String.format("%s.mapper", projectInformation.projectPackage()),
        clapCommandMapper.build()
    ).build();

    javaFile.writeTo(filer);
  }

}
