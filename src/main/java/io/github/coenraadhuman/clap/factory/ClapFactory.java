package io.github.coenraadhuman.clap.factory;

import io.github.coenraadhuman.clap.annotation.processor.information.reader.ProjectInformationReader;
import io.github.coenraadhuman.clap.annotation.processor.information.reader.impl.ProjectInformationReaderImpl;
import lombok.RequiredArgsConstructor;

import javax.lang.model.util.Elements;
import java.util.function.Supplier;

@RequiredArgsConstructor
public class ClapFactory {

  private final Elements elementUtils;

  public ProjectInformationReader getInformationService() {
    final Supplier<ProjectInformationReader> informationServiceSupplier =
        () -> new ProjectInformationReaderImpl(elementUtils);
    return informationServiceSupplier.get();
  }

}
