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

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import jakarta.validation.Validation;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.sonar.api.internal.MetadataLoader;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.Version;
import org.sonar.server.v2.common.RestResponseEntityExceptionHandler;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.MediaType;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.SpringValidatorAdapter;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.util.UrlPathHelper;

@Configuration
@EnableWebMvc
@ComponentScan(basePackages = {"org.springdoc"})
@PropertySource("classpath:springdoc.properties")
public class CommonWebConfig implements WebMvcConfigurer {

  @Override
  public void configurePathMatch(PathMatchConfigurer configurer) {
    UrlPathHelper urlPathHelper = new UrlPathHelper();
    urlPathHelper.setUrlDecode(false);
    configurer.setUrlPathHelper(urlPathHelper).setUseTrailingSlashMatch(true);
  }

  @Override
  public Validator getValidator() {
    // This validator gets returned from the
    // WebMvcConfigurationSupport#mvcValidator bean factory method.
    // We can create a new one each time here and an instance will be cached
    // in the Spring context.
    //
    // One reason we override the validator is to avoid a dependency
    // on an expression language implementation like expressly.
    //
    // This same validator must also be configured in ControllerTester,
    // otherwise unit test behavior will not match production behavior.
    //
    // The validator errors are formatted in RestResponseEntityExceptionHandler.
    var jakartaValidator = Validation.byDefaultProvider()
      .configure()
      .messageInterpolator(new ParameterMessageInterpolator())
      .buildValidatorFactory()
      .getValidator();
    return new SpringValidatorAdapter(jakartaValidator);
  }

  @Bean
  public RestResponseEntityExceptionHandler restResponseEntityExceptionHandler() {
    return new RestResponseEntityExceptionHandler();
  }

  @Bean
  public OpenAPI customOpenAPI() {
    Version sqVersion = MetadataLoader.loadSQVersion(System2.INSTANCE);
    return new OpenAPI()
      .info(new Info()
        .title("SonarQube Web API v2")
        .description("""
          The SonarQube API v2 is a REST API which enables you to interact with SonarQube programmatically.
          While not all endpoints of the former Web API are available yet, the ones available are stable and can be used in production environments.
          """)
        .version(sqVersion.toString()));
  }

  @Bean
  public BeanFactoryPostProcessor beanFactoryPostProcessor1(SpringDocConfigProperties springDocConfigProperties) {
    return beanFactory -> springDocConfigProperties.setDefaultProducesMediaType(MediaType.APPLICATION_JSON_VALUE);
  }

  @Override
  public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
    configurer.defaultContentType(MediaType.APPLICATION_JSON, MediaType.ALL);
  }
}
