package io.github.coenraadhuman.clap.model;

import java.util.List;

public record CommandArgumentClass(
    CommandArgumentElement commandArgument,
    List<OptionElement> options
) {

}