package io.github.coenraadhuman.clap.mapper;

public abstract class CommandMapperBase {

  protected String findArgumentAppender(
      final String[] arguments, final String shortInput, final String longInput, final String seperator,
      final String appendingValue
  ) {
    var foundValue = findArgumentProvidedValue(arguments, shortInput, longInput);
    if (foundValue != null) {
      return String.format("%s%s%s", foundValue, seperator, appendingValue);
    }
    return appendingValue;
  }

  protected String findArgumentProvidedValue(final String[] argument, final String shortInput, final String longInput) {
    for (int i = 1; i < argument.length; i++) {
      if (argument[i].equals(shortInput) || argument[i].equals(longInput)) {
        return argument[i + 1];
      }
    }
    return null;
  }

  protected <T> T findArgument(
      final String[] arguments, final String shortInput, final String longInput, final T returnValue, T defaultValue
  ) {
    var foundArgument = findArgument(arguments, shortInput, longInput, returnValue);
    if (foundArgument == null) {
      return defaultValue;
    }
    return foundArgument;
  }

  protected <T> T findArgument(
      final String[] arguments, final String shortInput, final String longInput, final T returnValue
  ) {
    var foundArgument = findArgument(arguments, shortInput, longInput);
    if (foundArgument) {
      return returnValue;
    }
    return null;
  }

  protected boolean findArgument(final String[] argument, final String shortInput, final String longInput) {
    for (int i = 1; i < argument.length; i++) {
      if (argument[i].equals(shortInput) || argument[i].equals(longInput)) {
        return true;
      }
    }
    return false;
  }

}
