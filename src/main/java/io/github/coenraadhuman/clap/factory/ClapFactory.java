package io.github.coenraadhuman.clap.factory;

import io.github.coenraadhuman.clap.annotation.processor.file.writer.FileWriter;
import io.github.coenraadhuman.clap.annotation.processor.file.writer.impl.ArgumentFileWriterImpl;
import io.github.coenraadhuman.clap.annotation.processor.file.writer.impl.CommandMapperFileWriterImpl;
import io.github.coenraadhuman.clap.annotation.processor.file.writer.impl.CommandRunnerFileWriterImpl;
import io.github.coenraadhuman.clap.annotation.processor.information.reader.ProjectInformationReader;
import io.github.coenraadhuman.clap.annotation.processor.information.reader.impl.ProjectInformationReaderImpl;
import lombok.RequiredArgsConstructor;

import javax.annotation.processing.Filer;
import javax.lang.model.util.Elements;
import java.util.function.Supplier;

@RequiredArgsConstructor
public class ClapFactory {

  private final Elements elementUtils;
  private final Filer filer;

  public ProjectInformationReader getInformationService() {
    final Supplier<ProjectInformationReader> supplier = () -> new ProjectInformationReaderImpl(elementUtils);
    return supplier.get();
  }

  public FileWriter getArgumentFileWriter() {
    final Supplier<FileWriter> supplier = () -> new ArgumentFileWriterImpl(filer);
    return supplier.get();
  }

  public FileWriter getCommandMapperFileWriter() {
    final Supplier<FileWriter> supplier = () -> new CommandMapperFileWriterImpl(filer);
    return supplier.get();
  }

  public FileWriter getCommandRunnerFileWriter() {
    final Supplier<FileWriter> supplier = () -> new CommandRunnerFileWriterImpl(filer);
    return supplier.get();
  }

}
