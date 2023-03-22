package io.github.coenraadhuman.clap.mapper;

import io.github.coenraadhuman.clap.model.StringOptionContainer;

public abstract class CommandMapperBase {

  protected StringOptionContainer findOptionProvidedValue(
      final String[] argument, final String shortInput, final String longInput
  ) {
    for (int i = 1; i < argument.length; i++) {
      if (argument[i].equals(shortInput) || argument[i].equals(longInput)) {
        if ((i + 1) < argument.length) {
          return new StringOptionContainer(argument[i + 1], true);
        } else {
          return new StringOptionContainer(null, true);
        }
      }
    }
    return new StringOptionContainer(null, false);
  }

  protected boolean findOption(final String[] argument, final String shortInput, final String longInput) {
    for (int i = 1; i < argument.length; i++) {
      if (argument[i].equals(shortInput) || argument[i].equals(longInput)) {
        return true;
      }
    }
    return false;
  }

}
