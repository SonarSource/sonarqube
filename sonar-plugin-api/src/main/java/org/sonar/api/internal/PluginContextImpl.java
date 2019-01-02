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
package org.sonar.api.internal;

import org.sonar.api.Plugin;
import org.sonar.api.SonarRuntime;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.internal.MapSettings;

/**
 * Implementation of {@link Plugin.Context} that plugins could use in their unit tests.
 *
 * Example:
 *
 * <pre>
 *   import org.sonar.api.internal.SonarRuntimeImpl;
 *   import org.sonar.api.config.internal.MapSettings;
 *
 *   ...
 *
 *   SonarRuntime runtime = SonarRuntimeImpl.forSonarQube(Version.create(7, 1), SonarQubeSide.SCANNER);
 *   MapSettings settings = new MapSettings().setProperty("foo", "bar");
 *   Plugin.Context context = new PluginContextImpl.Builder()
 *     .setSonarRuntime(runtime)
 *     .setBootConfiguration(settings.asConfig());
 *     .build();
 * </pre>
 *
 * @since 7.1
 */
public class PluginContextImpl extends Plugin.Context {

  private final Configuration bootConfiguration;

  private PluginContextImpl(Builder builder) {
    super(builder.sonarRuntime);
    this.bootConfiguration = builder.bootConfiguration != null ? builder.bootConfiguration : new MapSettings().asConfig();
  }

  @Override
  public Configuration getBootConfiguration() {
    return bootConfiguration;
  }

  public static class Builder {
    private SonarRuntime sonarRuntime;
    private Configuration bootConfiguration;

    /**
     * Required.
     * @see SonarRuntimeImpl
     * @return this
     */
    public Builder setSonarRuntime(SonarRuntime r) {
      this.sonarRuntime = r;
      return this;
    }

    /**
     * If not set, then an empty configuration is used.
     * @return this
     */
    public Builder setBootConfiguration(Configuration c) {
      this.bootConfiguration = c;
      return this;
    }

    public Plugin.Context build() {
      return new PluginContextImpl(this);
    }
  }
}
