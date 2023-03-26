package io.github.coenraadhuman.clap.model;

import io.github.coenraadhuman.clap.Command;

import javax.lang.model.element.Element;

public record CommandElement(
    Element element,
    Command command
) {

}
