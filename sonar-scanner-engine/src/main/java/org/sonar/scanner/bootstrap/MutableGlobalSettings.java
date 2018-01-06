/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.scanner.bootstrap;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.MessageException;

import static java.util.Objects.requireNonNull;

/**
 * @deprecated since 6.5 {@link GlobalConfiguration} used to be mutable, so keep a mutable copy for backward compatibility.
 */
@Deprecated
public class MutableGlobalSettings extends Settings {

  private final GlobalAnalysisMode mode;
  private final Map<String, String> mutableProperties = new HashMap<>();

  public MutableGlobalSettings(GlobalConfiguration globalSettings) {
    super(globalSettings.getDefinitions(), globalSettings.getEncryption());
    this.mutableProperties.putAll(globalSettings.getProperties());
    this.mode = globalSettings.getMode();
  }

  @Override
  protected Optional<String> get(String key) {
    if (mode.isIssues() && key.endsWith(".secured") && !key.contains(".license")) {
      throw MessageException.of("Access to the secured property '" + key
        + "' is not possible in issues mode. The SonarQube plugin which requires this property must be deactivated in issues mode.");
    }
    return Optional.ofNullable(mutableProperties.get(key));
  }

  @Override
  public Map<String, String> getProperties() {
    return mutableProperties;
  }

  @Override
  protected void set(String key, String value) {
    mutableProperties.put(
      requireNonNull(key, "key can't be null"),
      requireNonNull(value, "value can't be null").trim());
  }

  @Override
  protected void remove(String key) {
    mutableProperties.remove(key);
  }
}
