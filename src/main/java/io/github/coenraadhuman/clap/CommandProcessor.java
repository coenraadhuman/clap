package io.github.coenraadhuman.clap;

public interface CommandProcessor<T> {

  void process(T commandArgument);


}
