/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.issue;

import java.util.Collection;
import java.util.Map;
import org.junit.Test;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.server.user.UserSession;

import static org.assertj.core.api.Assertions.assertThat;

public class ActionTest {

  @Test
  public void key_should_not_be_empty() {
    try {
      new Action("") {
        @Override
        public boolean verify(Map<String, Object> properties, Collection<DefaultIssue> issues, UserSession userSession) {
          return false;
        }

        @Override
        public boolean execute(Map<String, Object> properties, Context context) {
          return false;
        }
      };
    } catch (Exception e) {
      assertThat(e).hasMessage("Action key must be set").isInstanceOf(IllegalArgumentException.class);
    }
  }

  @Test
  public void key_should_not_be_null() {
    try {
      new Action(null) {
        @Override
        public boolean verify(Map<String, Object> properties, Collection<DefaultIssue> issues, UserSession userSession) {
          return false;
        }

        @Override
        public boolean execute(Map<String, Object> properties, Context context) {
          return false;
        }
      };
    } catch (Exception e) {
      assertThat(e).hasMessage("Action key must be set").isInstanceOf(IllegalArgumentException.class);
    }
  }
}
