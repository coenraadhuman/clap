package io.github.coenraadhuman.clap.annotation.processor.file.writer;

import io.github.coenraadhuman.clap.model.ProjectInformation;

public interface FileWriter {

  void generate(ProjectInformation projectInformation);

}
