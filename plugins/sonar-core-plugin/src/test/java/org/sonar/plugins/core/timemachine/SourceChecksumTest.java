/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.core.timemachine;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.junit.Test;

public class SourceChecksumTest {
  @Test
  public void shouldGetChecksumForLine() {
    List<String> checksums = SourceChecksum.lineChecksumsOfFile("line");
    assertThat(SourceChecksum.getChecksumForLine(checksums, null), nullValue());
    assertThat(SourceChecksum.getChecksumForLine(checksums, 0), nullValue());
    assertThat(SourceChecksum.getChecksumForLine(checksums, 1), notNullValue());
    assertThat(SourceChecksum.getChecksumForLine(checksums, 2), nullValue());
  }

  /**
   * See http://jira.codehaus.org/browse/SONAR-2358
   */
  @Test
  public void shouldGenerateCorrectChecksums() {
    List<String> encoding = SourceChecksum.lineChecksumsOfFile("Привет Мир");
    assertThat(encoding.size(), is(1));
    assertThat(encoding.get(0), is("5ba3a45e1299ede07f56e5531351be52"));
  }

  @Test
  public void shouldSplitLinesAndIgnoreSpaces() {
    List<String> crlf = SourceChecksum.lineChecksumsOfFile("Hello\r\nWorld");
    List<String> lf = SourceChecksum.lineChecksumsOfFile("Hello\nWorld");
    List<String> cr = SourceChecksum.lineChecksumsOfFile("Hello\rWorld");
    assertThat(crlf.size(), is(2));
    assertThat(crlf.get(0), not(equalTo(crlf.get(1))));
    assertThat(lf, equalTo(crlf));
    assertThat(cr, equalTo(crlf));

    assertThat(SourceChecksum.lineChecksum("\tvoid  method()  {\n"),
        equalTo(SourceChecksum.lineChecksum("  void method() {")));
  }
}
