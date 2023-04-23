package io.github.coenraadhuman.clap.model;

import java.util.function.Function;

public record OptionValue(
    String description,
    String shortInput,
    String longInput,
    boolean providesValue
) implements Comparable<OptionValueComparer> {

  @Override
  public int compareTo(OptionValueComparer optionValueComparer) {
    if (optionValueComparer == null) {
      return -1;
    } else if (optionValueComparer.optionValue() == null) {
      return -1;
    } else {
      var thisShortInput = this.shortInput.replace("-", "");
      var otherShortInput = optionValueComparer.optionValue().shortInput().replace("-", "");

      Function<OptionValue, String> countOptionValue = (optionValue) -> {
        var shortInput = optionValue.shortInput().replace("-", "");
        var value = new StringBuilder();

        if (shortInput.length() == optionValueComparer.largestShortInput()) {
          return shortInput;
        }

        for (int i = 0; i < optionValueComparer.largestShortInput(); i++) {
          if (i < shortInput.chars().count()) {
            value.append(shortInput).charAt(i);
          } else {
            value.append('a');
          }
        }
        return value.toString();
      };

      var thisOptionShortInput = countOptionValue.apply(this);
      var otherOptionShortInput = countOptionValue.apply(optionValueComparer.optionValue());

      System.out.println(String.format("Comparing: this: %s (%s) to %s (%s)", thisOptionShortInput, thisShortInput,
          otherOptionShortInput, otherShortInput));

      if (thisOptionShortInput.compareTo(otherOptionShortInput) != 0) {
        return thisOptionShortInput.compareTo(otherOptionShortInput);
      }

      return this.shortInput.chars().count() < optionValueComparer.optionValue().shortInput.chars().count()
          ? -1
          : 1;
    }
  }

  public OptionValueComparer toOptionValueComparer(int largestShortInput) {
    return new OptionValueComparer(this, largestShortInput);
  }

}



