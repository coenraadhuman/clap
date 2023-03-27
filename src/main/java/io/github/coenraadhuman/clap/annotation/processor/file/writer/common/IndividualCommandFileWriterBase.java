package io.github.coenraadhuman.clap.annotation.processor.file.writer.common;

import io.github.coenraadhuman.clap.annotation.processor.file.writer.FileWriter;
import io.github.coenraadhuman.clap.model.CommandInformation;
import io.github.coenraadhuman.clap.model.ProjectInformation;

import java.io.IOException;

public abstract class IndividualCommandFileWriterBase implements FileWriter {

  @Override
  public void generate(ProjectInformation projectInformation) {
    for (var command : projectInformation.commands()) {
      try {
        processItem(projectInformation.projectPackage(), projectInformation.projectDescription(), command);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

  }

  protected abstract void processItem(String projectPackage, String projectDescription, CommandInformation command)
      throws IOException;

}
