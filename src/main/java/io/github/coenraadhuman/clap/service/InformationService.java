package io.github.coenraadhuman.clap.service;

import io.github.coenraadhuman.clap.model.ProjectInformation;

import javax.annotation.processing.RoundEnvironment;

public interface InformationService {

  ProjectInformation retrieve(RoundEnvironment roundEnvironment);

}
