/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.issue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.issue.action.Action;

import static org.fest.assertions.Assertions.assertThat;

public class DefaultActionsTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private DefaultActions actions;

  @Before
  public void before(){
    actions = new DefaultActions();
  }

  @Test
  public void should_add_action() {
    Action action = Action.builder("link-to-jira").build();

    actions.addAction(action);

    assertThat(actions.getActions()).hasSize(1);
  }

  @Test
  public void should_get_action() {
    Action action = Action.builder("link-to-jira").build();

    actions.addAction(action);

    assertThat(actions.getAction("link-to-jira")).isEqualTo(action);
    assertThat(actions.getAction("not-found")).isNull();
  }

}
