package io.github.coenraadhuman.clap.model;

import java.util.List;

public record ProjectInformation(
    String projectPackage,
    String projectDescription,
    List<CommandInformation> commands
) {

}
