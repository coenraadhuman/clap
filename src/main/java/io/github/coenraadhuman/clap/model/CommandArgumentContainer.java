package io.github.coenraadhuman.clap.model;

import io.github.coenraadhuman.clap.CommandArgument;

import javax.lang.model.element.Element;

public record CommandArgumentContainer(Element element, CommandArgument annotation) {

}
