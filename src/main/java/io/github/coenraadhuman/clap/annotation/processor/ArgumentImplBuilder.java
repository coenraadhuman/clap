package io.github.coenraadhuman.clap.annotation.processor;

import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import io.github.coenraadhuman.clap.model.CommandArgumentClassContainer;
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
  private final CommandArgumentClassContainer commandArgument;

  void generate() throws IOException {
    var commandArgumentClass = TypeSpec.classBuilder(String.format("%sImpl", className))
                                   .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                                   .addSuperinterface(implement);

    for (var option : commandArgument.options()) {
      FieldSpec.Builder field;
      MethodSpec.Builder method = MethodSpec.methodBuilder(option.element().getSimpleName().toString())
                                      .addModifiers(Modifier.PUBLIC)
                                      .addAnnotation(Override.class)
                                      .addStatement(String.format(
                                          "return %s", option.element().getSimpleName().toString()
                                      ));

      if (option.annotation().providesValue()) {
        // Todo: avoid direct access temp work around:
        field = FieldSpec.builder(String.class, option.element().getSimpleName().toString(), Modifier.PUBLIC);
        method.returns(String.class);
      } else {
        field = FieldSpec.builder(TypeName.BOOLEAN, option.element().getSimpleName().toString(), Modifier.PUBLIC);
        method.returns(TypeName.BOOLEAN);
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
                                "help.append(\"Actions for " + commandArgument.commandArgument().annotation().input()
                                    + "\\n\\n\")");

    // Todo: add usage in here.

    for (var option : commandArgument.options()) {
      // Todo: make this dynamic and calculate width that would fit given information.
      methodBuilder.addStatement(
          "help.append(String.format(\"%-5s%-20s %s\", \"" + option.annotation().shortInput() + ",\",\""
              + option.annotation().longInput()
              + "\",\"" + option.annotation().info() + "\\n\"))"
      );
    }

    methodBuilder
        .addStatement("return help.toString()")
        .returns(String.class);

    return methodBuilder;
  }

}
