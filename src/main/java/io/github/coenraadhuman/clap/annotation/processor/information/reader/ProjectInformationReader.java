package io.github.coenraadhuman.clap.annotation.processor.information.reader;

import io.github.coenraadhuman.clap.model.ProjectInformation;

import javax.annotation.processing.RoundEnvironment;

public interface ProjectInformationReader {

  ProjectInformation retrieve(RoundEnvironment roundEnvironment);

}
