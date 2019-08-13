/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.ui;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonar.api.utils.MessageException;
import org.sonar.api.web.WebAnalytics;

public class WebAnalyticsLoaderImpl implements WebAnalyticsLoader {

  @Nullable
  private final WebAnalytics analytics;

  public WebAnalyticsLoaderImpl(WebAnalytics[] analytics) {
    if (analytics.length > 1) {
      List<String> classes = Arrays.stream(analytics).map(a -> a.getClass().getName()).collect(Collectors.toList());
      throw MessageException.of("Limited to only one web analytics plugin. Found multiple implementations: " + classes);
    }
    this.analytics = analytics.length == 1 ? analytics[0] : null;
  }

  public WebAnalyticsLoaderImpl() {
    this.analytics = null;
  }

  @Override
  public Optional<String> getUrlPathToJs() {
    return Optional.ofNullable(analytics)
      .map(WebAnalytics::getUrlPathToJs)
      .filter(path -> !path.startsWith("/") && !path.contains("..") && !path.contains("://"))
      .map(path -> "/" + path);
  }
}
