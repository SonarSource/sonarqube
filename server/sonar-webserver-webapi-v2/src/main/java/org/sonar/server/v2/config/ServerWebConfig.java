/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import org.sonar.api.internal.MetadataLoader;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.Version;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
@ComponentScan(basePackages = { "org.springdoc", "org.sonar.server.v2.common" })
@PropertySource("classpath:springdoc.properties")
@EnableWebMvc
public class ServerWebConfig extends CommonWebConfig {

  @Bean
  public OpenAPI customOpenAPI() {
    Version sqVersion = MetadataLoader.loadSQVersion(System2.INSTANCE);
    return new OpenAPI()
        .info(new Info()
            .title("SonarQube Web API v2")
            .description(
                """
                    The SonarQube API v2 is a REST API which enables you to interact with SonarQube programmatically.
                    While not all endpoints of the former Web API are available yet, the ones available are stable and can be used in production environments.
                    """)
            .version(sqVersion.toString()));
  }

  @Bean
  public BeanFactoryPostProcessor beanFactoryPostProcessor1(SpringDocConfigProperties springDocConfigProperties) {
    return beanFactory -> springDocConfigProperties.setDefaultProducesMediaType(MediaType.APPLICATION_JSON_VALUE);
  }
}
