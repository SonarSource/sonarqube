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
package org.sonar.application;

import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;

public class SystemChecksTest {

  @Test
  public void checkJavaVersion() throws Exception {
    SystemChecks checks = new SystemChecks();

    // yes, sources are compiled with a supported Java version!
    checks.checkJavaVersion();

    checks.checkJavaVersion("1.6.1_b2");
    try {
      checks.checkJavaVersion("1.5.2");
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Minimal required Java version if 1.6. Got: 1.5.2");
    }
  }
}
