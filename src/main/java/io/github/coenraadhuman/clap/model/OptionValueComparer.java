package io.github.coenraadhuman.clap.model;

public record OptionValueComparer(
    OptionValue optionValue,
    int largestShortInput
) {
}
