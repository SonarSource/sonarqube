/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.v2.api.dashboards.config;

import org.sonarsource.reporting.dashboards.api.config.EnumConverterConfiguration;
import org.sonarsource.reporting.dashboards.api.model.DashboardResourceType;
import org.sonarsource.reporting.dashboards.server.BuiltInDashboardService;
import org.junit.jupiter.api.Test;
import org.sonar.server.v2.api.dashboards.controller.BuiltInDashboardsController;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.support.DefaultFormattingConversionService;

import static org.assertj.core.api.Assertions.assertThat;

class BuiltInDashboardsWebConfigurationTest {

  @Test
  void importsOnlyWebComponents() {
    assertThat(BuiltInDashboardsWebConfiguration.class.getAnnotation(Import.class).value())
      .containsExactly(BuiltInDashboardsController.class, EnumConverterConfiguration.class, BuiltInDashboardService.class);
  }

  @Test
  void addFormatters_registersLowercaseDashboardResourceTypeConverter() {
    try (var context = new AnnotationConfigApplicationContext(EnumConverterConfiguration.class)) {
      @SuppressWarnings("unchecked")
      Converter<String, DashboardResourceType> converter = context.getBean(Converter.class);
      var conversionService = new DefaultFormattingConversionService();

      new BuiltInDashboardsWebConfiguration(converter).addFormatters(conversionService);

      assertThat(conversionService.convert("project", DashboardResourceType.class)).isEqualTo(DashboardResourceType.PROJECT);
    }
  }
}
