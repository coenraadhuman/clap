package io.github.coenraadhuman.clap.annotation.processor;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import io.github.coenraadhuman.clap.ClapApplication;
import io.github.coenraadhuman.clap.Command;
import io.github.coenraadhuman.clap.CommandArgument;
import io.github.coenraadhuman.clap.Option;
import io.github.coenraadhuman.clap.model.CommandArgumentClassContainer;
import io.github.coenraadhuman.clap.model.CommandArgumentContainer;
import io.github.coenraadhuman.clap.model.CommandContainer;
import io.github.coenraadhuman.clap.model.OptionContainer;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@AutoService(Processor.class)
public class ClapProcessor extends AbstractProcessor {

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

  private void processCommandArguments(
      Map<String, CommandArgumentClassContainer> commandArguments, RoundEnvironment roundEnvironment
  ) {
    for (var annotatedElement : roundEnvironment.getElementsAnnotatedWith(CommandArgument.class)) {
      // Todo: check that we extends CommandArgumentProcessor:
      if (annotatedElement.getKind() != ElementKind.INTERFACE) {
        error("Only an interface can be annotated with CommandArgument", annotatedElement);
        throw new RuntimeException("Only an interface can be annotated with CommandArgument");
      }

      var key = getKey(annotatedElement);

      commandArguments.put(
          key,
          new CommandArgumentClassContainer(
              new CommandArgumentContainer(annotatedElement, annotatedElement.getAnnotation(CommandArgument.class)),
              new ArrayList<>()
          )
      );

      for (var enclosedElement : annotatedElement.getEnclosedElements()) {
        if (enclosedElement.getKind() == ElementKind.METHOD) {
          var option = enclosedElement.getAnnotation(Option.class);
          if (option != null) {
            commandArguments.get(key).options().add(new OptionContainer(enclosedElement, option));
          }
        }
      }
    }
  }

  private void processCommands(
      List<CommandContainer> commands, RoundEnvironment roundEnvironment
  ) {
    for (var annotatedElement : roundEnvironment.getElementsAnnotatedWith(Command.class)) {
      // Todo: check if correct interface is implemented
      if (annotatedElement.getKind() != ElementKind.CLASS) {
        error("Only a class can be annotated with Command", annotatedElement);
        throw new RuntimeException("Only a class can be annotated with Command");
      }

      commands.add(
          new CommandContainer(
              annotatedElement.getAnnotation(Command.class),
              annotatedElement
          )
      );
    }
  }

  private void createCommandArguments(Map<String, CommandArgumentClassContainer> commandArguments) {
    try {
      commandArguments.forEach((key, commandArgument) -> {
            var typeElement = (TypeElement) commandArgument.commandArgument().element();
            var className = ClassName.get(typeElement);

            commandArgument
                .options()
                .sort(Comparator.comparing(optionContainer -> optionContainer.annotation().shortInput()));

            var builder = new ArgumentImplBuilder(
                filer,
                className.simpleName(),
                className.packageName(),
                typeElement.asType(),
                commandArgument
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

  private void createMapper(Map<String, CommandArgumentClassContainer> commandArguments, String projectPackage) {
    var mapper = new CommandMapperBuilder(
        filer,
        String.format("%s.mapper", projectPackage),
        commandArguments
    );

    try {
      mapper.generate();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
    var projectPackage = "io.github.coenraadhuman.clap";

    var annotations = roundEnvironment.getElementsAnnotatedWith(ClapApplication.class);

    for (var foundAnnotation : annotations) {
      projectPackage = elements.getPackageOf(foundAnnotation).getQualifiedName().toString();
    }

    var commandArguments = new HashMap<String, CommandArgumentClassContainer>();
    var commands = new ArrayList<CommandContainer>();

    processCommandArguments(commandArguments, roundEnvironment);

    processCommands(commands, roundEnvironment);

    if (commandArguments.size() > 0) {
      createCommandArguments(commandArguments);
      createMapper(commandArguments, projectPackage);
    }

    if (commands.size() > 0) {
      var runner = new CommandRunnerBuilder(
          filer,
          projectPackage,
          commandArguments,
          commands
      );

      try {
        runner.generate();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    return false;
  }

  private String getKey(Element element) {
    var typeElement = (TypeElement) element;
    var className = ClassName.get(typeElement);
    return className.canonicalName();
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
