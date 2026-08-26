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

import com.sonarsource.reporting.dashboards.api.config.EnumConverterConfiguration;
import com.sonarsource.reporting.dashboards.api.model.DashboardResourceType;
import com.sonarsource.reporting.dashboards.server.BuiltInDashboardService;
import com.sonarsource.reporting.dashboards.server.db.DashboardsDbClient;
import com.sonarsource.reporting.dashboards.server.db.repository.DashboardsRepository;
import org.sonar.server.v2.api.dashboards.controller.BuiltInDashboardsController;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Registers the built-in dashboard endpoints available in every edition. */
@Configuration
@Import({
  BuiltInDashboardsController.class,
  EnumConverterConfiguration.class,
  DashboardsDbClient.class,
  DashboardsRepository.class,
  BuiltInDashboardService.class
})
public class BuiltInDashboardsWebConfiguration implements WebMvcConfigurer {
  private final Converter<String, DashboardResourceType> converter;

  public BuiltInDashboardsWebConfiguration(Converter<String, DashboardResourceType> converter) {
    this.converter = converter;
  }

  @Override
  public void addFormatters(FormatterRegistry registry) {
    registry.addConverter(converter);
  }
}
