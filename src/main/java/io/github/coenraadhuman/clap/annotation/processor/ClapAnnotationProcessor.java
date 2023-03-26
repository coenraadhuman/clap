package io.github.coenraadhuman.clap.annotation.processor;

import com.google.auto.service.AutoService;
import io.github.coenraadhuman.clap.ClapApplication;
import io.github.coenraadhuman.clap.Command;
import io.github.coenraadhuman.clap.CommandArgument;
import io.github.coenraadhuman.clap.Option;
import io.github.coenraadhuman.clap.factory.ClapFactory;

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

  @Override
  public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
    var factory = new ClapFactory(elements, filer);

    var projectInformation = factory.getInformationService()
                                 .retrieve(roundEnvironment);

    if (projectInformation.commands().size() > 0) {
      factory.getArgumentFileWriter()
          .generate(projectInformation);

      factory.getCommandMapperFileWriter()
          .generate(projectInformation);

      factory.getCommandRunnerFileWriter()
          .generate(projectInformation);
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
