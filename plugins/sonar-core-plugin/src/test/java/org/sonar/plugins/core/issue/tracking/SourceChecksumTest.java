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
package org.sonar.plugins.core.issue.tracking;

import org.junit.Test;
import org.sonar.plugins.core.issue.tracking.SourceChecksum;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class SourceChecksumTest {
  @Test
  public void shouldGetChecksumForLine() {
    List<String> checksums = SourceChecksum.lineChecksumsOfFile("line");
    assertThat(SourceChecksum.getChecksumForLine(checksums, null)).isNull();
    assertThat(SourceChecksum.getChecksumForLine(checksums, 0)).isNull();
    assertThat(SourceChecksum.getChecksumForLine(checksums, 1)).isNotNull();
    assertThat(SourceChecksum.getChecksumForLine(checksums, 2)).isNull();
  }

  /**
   * See http://jira.codehaus.org/browse/SONAR-2358
   */
  @Test
  public void shouldGenerateCorrectChecksums() {
    List<String> encoding = SourceChecksum.lineChecksumsOfFile("Привет Мир");
    assertThat(encoding).hasSize(1);
    assertThat(encoding.get(0)).isEqualTo("5ba3a45e1299ede07f56e5531351be52");
  }

  @Test
  public void shouldSplitLinesAndIgnoreSpaces() {
    List<String> crlf = SourceChecksum.lineChecksumsOfFile("Hello\r\nWorld");
    List<String> lf = SourceChecksum.lineChecksumsOfFile("Hello\nWorld");
    List<String> cr = SourceChecksum.lineChecksumsOfFile("Hello\rWorld");
    assertThat(crlf).hasSize(2);
    assertThat(crlf.get(0)).isNotEqualTo(crlf.get(1));
    assertThat(lf).isEqualTo(crlf);
    assertThat(cr).isEqualTo(crlf);

    assertThat(SourceChecksum.lineChecksum("\tvoid  method()  {\n")).isEqualTo(SourceChecksum.lineChecksum("  void method() {"));
  }
}
