package io.github.coenraadhuman.clap.model;

import java.util.List;

public record CommandInformation(
    CommandElement command,
    CommandArgumentElement argument,
    List<OptionElement> options,
    String argumentClassSimpleName
) {

}
