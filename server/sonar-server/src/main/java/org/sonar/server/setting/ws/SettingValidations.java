/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

package org.sonar.server.setting.ws;

import java.util.Locale;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.i18n.I18n;
import org.sonar.db.component.ComponentDto;

import static org.sonar.server.ws.WsUtils.checkRequest;

public class SettingValidations {
  private final PropertyDefinitions definitions;
  private final I18n i18n;

  public SettingValidations(PropertyDefinitions definitions, I18n i18n) {
    this.definitions = definitions;
    this.i18n = i18n;
  }

  public SettingValidation scope() {
    return data -> {
      PropertyDefinition definition = definitions.get(data.key);
      checkRequest(data.component != null || definition == null || definition.global() || isGlobal(definition),
        "Setting '%s' cannot be global", data.key);
    };
  }

  public SettingValidation qualifier() {
    return data -> {
      String qualifier = data.component == null ? "" : data.component.qualifier();
      PropertyDefinition definition = definitions.get(data.key);
      checkRequest(data.component == null || definition == null || definition.qualifiers().contains(data.component.qualifier()),
        "Setting '%s' cannot be set on a %s", data.key, i18n.message(Locale.ENGLISH, "qualifier." + qualifier, null));
    };
  }

  private static boolean isGlobal(PropertyDefinition definition) {
    return !definition.global() && definition.qualifiers().isEmpty();
  }

  @FunctionalInterface
  public interface SettingValidation {
    void validate(SettingData data);
  }

  public static class SettingData {
    private final String key;
    @CheckForNull
    private final ComponentDto component;

    public SettingData(String key, @Nullable ComponentDto component) {
      this.key = key;
      this.component = component;
    }

  }
}
