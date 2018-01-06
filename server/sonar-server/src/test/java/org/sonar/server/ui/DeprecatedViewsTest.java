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
package org.sonar.server.ui;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.api.web.Page;
import org.sonar.api.web.View;
import org.sonar.api.web.Widget;

import static org.assertj.core.api.Assertions.assertThat;

public class DeprecatedViewsTest {

  @Rule
  public LogTester logger = new LogTester();

  private DeprecatedViews underTest;

  @Test
  public void no_log_when_no_views() {
    underTest = new DeprecatedViews();
    underTest.start();

    assertThat(logger.logs()).isEmpty();
  }

  @Test
  public void log_one_warning_by_view() {
    underTest = new DeprecatedViews(new View[] {
      new FakePage("governance/my_page", "My Page"),
      new FakeWidget("governance/my_widget", "My Widget")});
    assertThat(logger.logs()).isEmpty();

    underTest.start();

    assertThat(logger.logs()).hasSize(2);
    assertThat(logger.logs(LoggerLevel.WARN)).containsExactly(
      "Page 'My Page' (governance/my_page) is ignored. See org.sonar.api.web.page.PageDefinition to define pages.",
      "Widget 'My Widget' (governance/my_widget) is ignored. See org.sonar.api.web.page.PageDefinition to define pages.");
  }

  private static final class FakeWidget implements Widget {
    private final String id;
    private final String name;

    private FakeWidget(String id, String name) {
      this.id = id;
      this.name = name;
    }

    @Override
    public String getId() {
      return id;
    }

    @Override
    public String getTitle() {
      return name;
    }
  }

  private static class FakePage implements Page {
    private final String id;
    private final String name;

    FakePage(String id, String name) {
      this.id = id;
      this.name = name;
    }

    @Override
    public String getId() {
      return id;
    }

    @Override
    public String getTitle() {
      return name;
    }
  }
}
