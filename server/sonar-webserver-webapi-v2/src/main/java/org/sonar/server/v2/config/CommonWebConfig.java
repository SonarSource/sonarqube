/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.v2.config;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.sonar.server.v2.common.RestResponseEntityExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.validation.beanvalidation.SpringValidatorAdapter;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Common Spring Web configuration for SonarQube v2 API.
 * This class overrides the external sonar-spring dependency's CommonWebConfig
 * to fix compatibility with Spring Framework 7.x where setUseTrailingSlashMatch was removed.
 */
public class CommonWebConfig implements WebMvcConfigurer {

  @Override
  public org.springframework.validation.Validator getValidator() {
    ValidatorFactory validatorFactory = Validation.byDefaultProvider()
      .configure()
      .messageInterpolator(new ParameterMessageInterpolator())
      .buildValidatorFactory();
    Validator validator = validatorFactory.getValidator();
    return new SpringValidatorAdapter(validator);
  }

  @Bean
  public RestResponseEntityExceptionHandler restResponseEntityExceptionHandler() {
    return new RestResponseEntityExceptionHandler();
  }

  @Override
  public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
    configurer.defaultContentType(MediaType.APPLICATION_JSON, MediaType.ALL);
  }
}
