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
package org.sonar.server.es.newindex;

import org.sonar.api.config.Configuration;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

public class SettingsConfiguration {
  public static final int MANUAL_REFRESH_INTERVAL = -1;

  private final Configuration configuration;
  private final int defaultNbOfShards;
  private final int refreshInterval;

  private SettingsConfiguration(Builder builder) {
    this.configuration = builder.configuration;
    this.defaultNbOfShards = builder.defaultNbOfShards;
    this.refreshInterval = builder.refreshInterval;
  }

  public static Builder newBuilder(Configuration configuration) {
    return new Builder(configuration);
  }

  public Configuration getConfiguration() {
    return configuration;
  }

  public int getDefaultNbOfShards() {
    return defaultNbOfShards;
  }

  public int getRefreshInterval() {
    return refreshInterval;
  }

  public static class Builder {
    private final Configuration configuration;
    private int defaultNbOfShards = 1;
    private int refreshInterval = 30;

    public Builder(Configuration configuration) {
      this.configuration = requireNonNull(configuration, "configuration can't be null");
    }

    public Builder setDefaultNbOfShards(int defaultNbOfShards) {
      checkArgument(defaultNbOfShards >= 1, "defaultNbOfShards must be >= 1");
      this.defaultNbOfShards = defaultNbOfShards;
      return this;
    }

    public Builder setRefreshInterval(int refreshInterval) {
      checkArgument(refreshInterval == -1 || refreshInterval > 0,
        "refreshInterval must be either -1 or strictly positive");
      this.refreshInterval = refreshInterval;
      return this;
    }

    public SettingsConfiguration build() {
      return new SettingsConfiguration(this);
    }
  }

}
