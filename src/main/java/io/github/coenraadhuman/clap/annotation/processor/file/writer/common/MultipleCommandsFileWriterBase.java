package io.github.coenraadhuman.clap.annotation.processor.file.writer.common;

import io.github.coenraadhuman.clap.annotation.processor.file.writer.FileWriter;
import io.github.coenraadhuman.clap.model.ProjectInformation;

import java.io.IOException;

public abstract class MultipleCommandsFileWriterBase implements FileWriter {

  @Override
  public void generate(ProjectInformation projectInformation) {
    try {
      process(projectInformation);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  protected abstract void process(ProjectInformation projectInformation) throws IOException;

}
