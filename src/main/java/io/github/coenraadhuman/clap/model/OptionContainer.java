package io.github.coenraadhuman.clap.model;

import io.github.coenraadhuman.clap.Option;

import javax.lang.model.element.Element;

public record OptionContainer(Element element, Option annotation) {

}
