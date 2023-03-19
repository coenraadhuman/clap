package io.github.coenraadhuman.clap;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
public @interface Option {

  String info();

  String shortInput();

  String longInput();

  boolean providesValue();

}
