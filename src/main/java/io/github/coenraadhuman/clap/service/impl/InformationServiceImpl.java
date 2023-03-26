package io.github.coenraadhuman.clap.service.impl;

import com.squareup.javapoet.ClassName;
import io.github.coenraadhuman.clap.ClapApplication;
import io.github.coenraadhuman.clap.Command;
import io.github.coenraadhuman.clap.CommandArgument;
import io.github.coenraadhuman.clap.Option;
import io.github.coenraadhuman.clap.model.CommandArgumentClass;
import io.github.coenraadhuman.clap.model.CommandArgumentElement;
import io.github.coenraadhuman.clap.model.CommandElement;
import io.github.coenraadhuman.clap.model.CommandInformation;
import io.github.coenraadhuman.clap.model.OptionElement;
import io.github.coenraadhuman.clap.model.ProjectInformation;
import io.github.coenraadhuman.clap.service.InformationService;
import lombok.RequiredArgsConstructor;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class InformationServiceImpl implements InformationService {

  private static final String PACKAGE_KEY = "project-package";
  private static final String DESCRIPTION_KEY = "project-description";

  private final Elements elementUtils;

  @Override
  public ProjectInformation retrieve(final RoundEnvironment roundEnvironment) {
    var mainInformation = getMainInformation(roundEnvironment);
    var commandInformation = getCommandElements(roundEnvironment);
    var commandArgumentInformation = getCommandArgumentInformation(roundEnvironment);
    var commands = combineCommandAndArgumentInformation(commandInformation, commandArgumentInformation);

    return new ProjectInformation(
        mainInformation.get(PACKAGE_KEY),
        mainInformation.get(DESCRIPTION_KEY),
        commands
    );
  }

  private List<CommandInformation> combineCommandAndArgumentInformation(
      final List<CommandElement> commandInformation, final Map<String, CommandArgumentClass> commandArgumentInformation
  ) {
    var commands = new ArrayList<CommandInformation>();

    commandArgumentInformation.forEach((key, commandArgument) -> {
      var commandArgumentCanonicalName = ClassName.get((TypeElement) commandArgument.commandArgument().element())
                                             .canonicalName();

      for (var command : commandInformation) {
        command.element()
            .getAnnotationMirrors()
            .forEach(annotationMirror ->
                         annotationMirror
                             .getElementValues()
                             .forEach((executableElement, annotationValue) -> {
                               if (executableElement.toString().equals("argument()")
                                       && annotationValue.toString()
                                              .replace(".class", "").equals(
                                       commandArgumentCanonicalName)) {
                                 commands.add(
                                     new CommandInformation(
                                         command,
                                         commandArgument.commandArgument(),
                                         commandArgument.options(),
                                         commandArgumentCanonicalName
                                     )
                                 );
                               }
                             })
            );
      }
    });

    if (commands.size() != commandInformation.size()) {
      throw new RuntimeException("All commands should have an argument");
    }

    return commands;
  }


  private Map<String, String> getMainInformation(final RoundEnvironment roundEnvironment) {
    var information = new HashMap<String, String>();

    var annotations = roundEnvironment.getElementsAnnotatedWith(ClapApplication.class);

    for (var foundAnnotation : annotations) {
      information.put(PACKAGE_KEY, elementUtils.getPackageOf(foundAnnotation).getQualifiedName().toString());
      information.put(DESCRIPTION_KEY, foundAnnotation.getAnnotation(ClapApplication.class).description());

      if (information.get(PACKAGE_KEY) != null && information.get(DESCRIPTION_KEY) != null) {
        break;
      }
    }

    return information;
  }

  private List<CommandElement> getCommandElements(RoundEnvironment roundEnvironment) {
    var commands = new ArrayList<CommandElement>();

    for (var annotatedElement : roundEnvironment.getElementsAnnotatedWith(Command.class)) {
      // Todo: check if correct interface is implemented
      if (annotatedElement.getKind() != ElementKind.CLASS) {
        // Todo: get better logging
        // error("Only a class can be annotated with Command", annotatedElement);
        throw new RuntimeException("Only a class can be annotated with Command");
      }

      commands.add(
          new CommandElement(
              annotatedElement,
              annotatedElement.getAnnotation(Command.class)
          )
      );
    }

    return commands;
  }

  private Map<String, CommandArgumentClass> getCommandArgumentInformation(RoundEnvironment roundEnvironment) {
    var arguments = new HashMap<String, CommandArgumentClass>();

    for (var annotatedElement : roundEnvironment.getElementsAnnotatedWith(CommandArgument.class)) {
      // Todo: check that we extends CommandArgumentProcessor:
      if (annotatedElement.getKind() != ElementKind.INTERFACE) {
        // Todo: get better logging
        // error("Only an interface can be annotated with CommandArgument", annotatedElement);
        throw new RuntimeException("Only an interface can be annotated with CommandArgument");
      }

      var typeElement = (TypeElement) annotatedElement;
      var className = ClassName.get(typeElement);
      var key = className.canonicalName();

      arguments.put(
          key,
          new CommandArgumentClass(
              new CommandArgumentElement(annotatedElement, annotatedElement.getAnnotation(CommandArgument.class)),
              new ArrayList<>()
          )
      );

      for (var enclosedElement : annotatedElement.getEnclosedElements()) {
        if (enclosedElement.getKind() == ElementKind.METHOD) {
          var option = enclosedElement.getAnnotation(Option.class);
          if (option != null) {
            arguments.get(key).options().add(new OptionElement(enclosedElement, option));
          }
        }
      }
    }

    return arguments;
  }

}
