package io.github.coenraadhuman.clap;


public interface CommandMapper {

  CommandArgumentProcessor map(String[] args);

}
