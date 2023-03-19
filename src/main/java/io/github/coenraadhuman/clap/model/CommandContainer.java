package io.github.coenraadhuman.clap.model;

import io.github.coenraadhuman.clap.Command;

import javax.lang.model.element.Element;

public record CommandContainer(Command command, Element element) {

}
