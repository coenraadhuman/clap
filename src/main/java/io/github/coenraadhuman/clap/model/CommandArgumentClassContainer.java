package io.github.coenraadhuman.clap.model;

import java.util.List;

public record CommandArgumentClassContainer(CommandArgumentContainer commandArgument, List<OptionContainer> options) {

}