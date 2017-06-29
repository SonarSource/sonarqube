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
package org.sonar.scanner.bootstrap;

import java.util.HashMap;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.CoreProperties;

import static org.assertj.core.api.Assertions.assertThat;

public class GlobalModeTest {
  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void testModeNotSupported() {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("[preview, publish, issues]");

    createMode(CoreProperties.ANALYSIS_MODE, "invalid");
  }

  @Test
  public void testOtherProperty() {
    GlobalMode mode = createMode(CoreProperties.ANALYSIS_MODE, CoreProperties.ANALYSIS_MODE_PUBLISH);
    assertThat(mode.isPreview()).isFalse();
    assertThat(mode.isIssues()).isFalse();
    assertThat(mode.isPublish()).isTrue();
  }

  @Test
  public void testIssuesMode() {
    GlobalMode mode = createMode(CoreProperties.ANALYSIS_MODE, CoreProperties.ANALYSIS_MODE_ISSUES);
    assertThat(mode.isPreview()).isFalse();
    assertThat(mode.isIssues()).isTrue();
    assertThat(mode.isPublish()).isFalse();
  }

  @Test
  public void preview_mode_fallback_issues() {
    GlobalMode mode = createMode(CoreProperties.ANALYSIS_MODE, CoreProperties.ANALYSIS_MODE_PREVIEW);

    assertThat(mode.isIssues()).isTrue();
    assertThat(mode.isPreview()).isFalse();
  }

  @Test
  public void testDefault() {
    GlobalMode mode = createMode(null, null);
    assertThat(mode.isPreview()).isFalse();
    assertThat(mode.isIssues()).isFalse();
    assertThat(mode.isPublish()).isTrue();
  }

  @Test(expected = IllegalStateException.class)
  public void testInvalidMode() {
    createMode(CoreProperties.ANALYSIS_MODE, CoreProperties.ANALYSIS_MODE_ANALYSIS);
  }

  private GlobalMode createMode(String key, String value) {
    Map<String, String> map = new HashMap<>();
    if (key != null) {
      map.put(key, value);
    }
    GlobalProperties props = new GlobalProperties(map);
    return new GlobalMode(props);
  }
}
