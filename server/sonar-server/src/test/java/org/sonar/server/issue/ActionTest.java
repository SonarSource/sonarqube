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
package org.sonar.server.issue;

import java.util.Collection;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.server.user.UserSession;

public class ActionTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void key_should_not_be_empty() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Action key must be set");

    new FakeAction("");
  }

  @Test
  public void key_should_not_be_null() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Action key must be set");

    new FakeAction(null);
  }

  private static class FakeAction extends Action {

    FakeAction(String key) {
      super(key);
    }

    @Override
    public boolean verify(Map<String, Object> properties, Collection<DefaultIssue> issues, UserSession userSession) {
      return false;
    }

    @Override
    public boolean execute(Map<String, Object> properties, Context context) {
      return false;
    }

    @Override
    public boolean shouldRefreshMeasures() {
      return false;
    }
  }
}
