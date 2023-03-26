package io.github.coenraadhuman.clap.annotation.processor;

import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import io.github.coenraadhuman.clap.model.CommandInformation;
import io.github.coenraadhuman.clap.model.StringOption;
import lombok.RequiredArgsConstructor;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.TypeMirror;
import java.io.IOException;

@RequiredArgsConstructor
public class ArgumentImplBuilder {

  private final Filer filer;
  private final String className;
  private final String packageName;

  private final TypeMirror implement;
  private final CommandInformation command;

  void generate() throws IOException {
    var commandArgumentClass = TypeSpec.classBuilder(String.format("%sImpl", className))
                                   .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                                   .addSuperinterface(implement);

    for (var option : command.options()) {
      FieldSpec.Builder field;
      MethodSpec.Builder method = MethodSpec.methodBuilder(option.element().getSimpleName().toString())
                                      .addModifiers(Modifier.PUBLIC)
                                      .addAnnotation(Override.class);

      if (option.annotation().providesValue()) {
        // Todo: avoid direct access temp work around:
        method.addStatement(String.format(
                "return %s.value()", option.element().getSimpleName().toString()
            ))
            .returns(String.class);
        field = FieldSpec.builder(StringOption.class, option.element().getSimpleName().toString(),
            Modifier.PUBLIC);
      } else {
        field = FieldSpec.builder(TypeName.BOOLEAN, option.element().getSimpleName().toString(), Modifier.PUBLIC);
        method.addStatement(String.format(
                "return %s", option.element().getSimpleName().toString()
            ))
            .returns(TypeName.BOOLEAN);
      }

      commandArgumentClass.addField(field.build());
      commandArgumentClass.addMethod(method.build());
    }

    var toStringMethod = createToString();
    commandArgumentClass.addMethod(toStringMethod.build());

    var javaFile = JavaFile.builder(packageName, commandArgumentClass.build()).build();

    javaFile.writeTo(filer);
  }

  private MethodSpec.Builder createToString() {
    var methodBuilder = MethodSpec.methodBuilder("toString")
                            .addModifiers(Modifier.PUBLIC)
                            .addStatement("var help = new StringBuilder()")
                            .addStatement(
                                "help.append(\"Actions for " + command.argument().annotation().input()
                                    + "\\n\\n\")");

    methodBuilder.addStatement("help.append(\"Options:\\n\")");

    for (var option : command.options()) {
      // Todo: make this dynamic and calculate width that would fit given information.
      methodBuilder.addStatement(
          "help.append(String.format(\"  %-5s%-20s %s\", \"" + option.annotation().shortInput() + ",\",\""
              + option.annotation().longInput()
              + "\",\"" + option.annotation().description() + "\\n\"))"
      );
    }

    methodBuilder
        .addStatement("return help.toString()")
        .returns(String.class);

    return methodBuilder;
  }

}
