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
package org.sonar.batch.scan.filesystem;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.sonar.api.batch.fs.InputFile;

import static org.fest.assertions.Assertions.assertThat;

public class StatusDetectionTest {
  @Test
  public void detect_status() throws Exception {
    StatusDetection statusDetection = new StatusDetection(ImmutableMap.of(
      "src/Foo.java", "ABCDE",
      "src/Bar.java", "FGHIJ"
    ));

    assertThat(statusDetection.status("src/Foo.java", "ABCDE")).isEqualTo(InputFile.Status.SAME);
    assertThat(statusDetection.status("src/Foo.java", "XXXXX")).isEqualTo(InputFile.Status.CHANGED);
    assertThat(statusDetection.status("src/Other.java", "QWERT")).isEqualTo(InputFile.Status.ADDED);
  }
}
