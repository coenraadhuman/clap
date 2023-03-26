package io.github.coenraadhuman.clap.annotation.processor;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import io.github.coenraadhuman.clap.ClapApplication;
import io.github.coenraadhuman.clap.Command;
import io.github.coenraadhuman.clap.CommandArgument;
import io.github.coenraadhuman.clap.Option;
import io.github.coenraadhuman.clap.annotation.processor.file.writer.ArgumentFileWriter;
import io.github.coenraadhuman.clap.annotation.processor.file.writer.CommandMapperFileWriter;
import io.github.coenraadhuman.clap.annotation.processor.file.writer.CommandRunnerFileWriter;
import io.github.coenraadhuman.clap.factory.ClapFactory;
import io.github.coenraadhuman.clap.model.ProjectInformation;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;

@AutoService(Processor.class)
public class ClapAnnotationProcessor extends AbstractProcessor {

  private Filer filer;
  private Messager messager;
  private Elements elements;
  private Types types;

  @Override
  public synchronized void init(ProcessingEnvironment processingEnv) {
    super.init(processingEnv);
    filer = processingEnv.getFiler();
    messager = processingEnv.getMessager();
    types = processingEnv.getTypeUtils();
    elements = processingEnv.getElementUtils();
  }


  private void createCommandArguments(final ProjectInformation projectInformation) {
    try {
      projectInformation.commands().forEach(command -> {
            var typeElement = (TypeElement) command.argument().element();
            var className = ClassName.get(typeElement);

            command
                .options()
                .sort(Comparator.comparing(optionElement -> optionElement.annotation().shortInput()));

            var builder = new ArgumentFileWriter(
                filer,
                className.simpleName(),
                className.packageName(),
                typeElement.asType(),
                command
            );

            try {
              builder.generate();
            } catch (IOException e) {
              throw new RuntimeException(e);
            }
          }
      );
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void createMapper(final ProjectInformation projectInformation) {
    var mapper = new CommandMapperFileWriter(
        filer,
        String.format("%s.mapper", projectInformation.projectPackage()),
        projectInformation.commands()
    );

    try {
      mapper.generate();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
    // Create factory:
    var factory = new ClapFactory(elements);
    var informationService = factory.getInformationService();

    // Get project data to process:
    var projectInformation = informationService.retrieve(roundEnvironment);

    // Create source files:
    if (projectInformation.commands().size() > 0) {
      createCommandArguments(projectInformation);
      createMapper(projectInformation);

      var runner = new CommandRunnerFileWriter(
          filer,
          projectInformation.projectPackage(),
          projectInformation.projectDescription(),
          projectInformation.commands()
      );

      try {
        runner.generate();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    return false;
  }


  @Override
  public Set<String> getSupportedAnnotationTypes() {
    Set<String> annotations = new LinkedHashSet<>();
    annotations.add(Command.class.getCanonicalName());
    annotations.add(CommandArgument.class.getCanonicalName());
    annotations.add(Option.class.getCanonicalName());
    annotations.add(ClapApplication.class.getCanonicalName());
    return annotations;
  }

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latestSupported();
  }

  private void error(final String message, final Element element) {
    messager.printMessage(Diagnostic.Kind.ERROR, message, element);
  }

  private void error(final String message) {
    messager.printMessage(Diagnostic.Kind.ERROR, message);
  }

  private void note(final String message) {
    messager.printMessage(Diagnostic.Kind.NOTE, message);
  }

}
