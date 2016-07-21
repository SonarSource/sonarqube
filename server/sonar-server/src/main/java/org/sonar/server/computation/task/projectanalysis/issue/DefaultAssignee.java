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
package org.sonar.server.computation.task.projectanalysis.issue;

import com.google.common.base.Strings;
import javax.annotation.CheckForNull;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.server.computation.task.projectanalysis.component.SettingsRepository;
import org.sonar.server.computation.task.projectanalysis.component.TreeRootHolder;
import org.sonar.server.user.index.UserDoc;
import org.sonar.server.user.index.UserIndex;

import static org.sonar.api.CoreProperties.DEFAULT_ISSUE_ASSIGNEE;

/**
 * The user who is optionally declared as being the assignee
 * of all the issues which SCM author is not associated with any SonarQube user.
 */
public class DefaultAssignee {

  private static final Logger LOG = Loggers.get(DefaultAssignee.class);

  private final TreeRootHolder treeRootHolder;
  private final UserIndex userIndex;
  private final SettingsRepository settingsRepository;

  private boolean loaded = false;
  private String login = null;

  public DefaultAssignee(TreeRootHolder treeRootHolder, UserIndex userIndex, SettingsRepository settingsRepository) {
    this.treeRootHolder = treeRootHolder;
    this.userIndex = userIndex;
    this.settingsRepository = settingsRepository;
  }

  @CheckForNull
  public String getLogin() {
    if (!loaded) {
      String configuredLogin = settingsRepository.getSettings(treeRootHolder.getRoot()).getString(DEFAULT_ISSUE_ASSIGNEE);
      if (!Strings.isNullOrEmpty(configuredLogin) && isValidLogin(configuredLogin)) {
        this.login = configuredLogin;
      }
      loaded = true;
    }
    return login;
  }

  private boolean isValidLogin(String s) {
    UserDoc user = userIndex.getNullableByLogin(s);
    if (user == null || !user.active()) {
      LOG.info("Property {} is set with an unknown login: {}", DEFAULT_ISSUE_ASSIGNEE, s);
      return false;
    }
    return true;
  }
}
