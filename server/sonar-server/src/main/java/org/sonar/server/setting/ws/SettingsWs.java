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
package org.sonar.server.setting.ws;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import org.sonar.api.server.ws.WebService;

import static org.sonar.core.config.CorePropertyDefinitions.LEAK_PERIOD;

public class SettingsWs implements WebService {

  public static final Set<String> SETTING_ON_BRANCHES = ImmutableSet.of(LEAK_PERIOD);

  private final SettingsWsAction[] actions;

  public SettingsWs(SettingsWsAction... actions) {
    this.actions = actions;
  }

  @Override
  public void define(Context context) {
    NewController controller = context.createController("api/settings")
      .setDescription("Manage settings.")
      .setSince("6.1");
    for (SettingsWsAction action : actions) {
      action.define(controller);
    }
    controller.done();
  }
}
