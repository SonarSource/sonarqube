/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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
package org.sonar.batch.bootstrap;

import org.sonar.api.CoreProperties;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class GlobalModeTest {
  @Test
  public void testPreview() {
    GlobalMode mode = createMode(CoreProperties.ANALYSIS_MODE, CoreProperties.ANALYSIS_MODE_PREVIEW);
    assertThat(mode.isPreview()).isTrue();
    assertThat(mode.isIssues()).isFalse();
    assertThat(mode.isPublish()).isFalse();
  }

  @Test
  public void testOtherProperty() {
    GlobalMode mode = createMode(CoreProperties.ANALYSIS_MODE, CoreProperties.ANALYSIS_MODE_PUBLISH);
    assertThat(mode.isPreview()).isFalse();
    assertThat(mode.isIssues()).isFalse();
    assertThat(mode.isPublish()).isTrue();
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
    BootstrapProperties props = new BootstrapProperties(map);
    return new GlobalMode(props);
  }
}

