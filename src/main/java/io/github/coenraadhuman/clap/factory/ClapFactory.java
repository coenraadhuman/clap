package io.github.coenraadhuman.clap.factory;

import io.github.coenraadhuman.clap.service.InformationService;
import io.github.coenraadhuman.clap.service.impl.InformationServiceImpl;
import lombok.RequiredArgsConstructor;

import javax.lang.model.util.Elements;
import java.util.function.Supplier;

@RequiredArgsConstructor
public class ClapFactory {

  private final Elements elementUtils;

  public InformationService getInformationService() {
    final Supplier<InformationService> informationServiceSupplier = () -> new InformationServiceImpl(elementUtils);
    return informationServiceSupplier.get();
  }

}
