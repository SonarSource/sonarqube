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

package org.sonar.batch.protocol;

import com.google.common.io.Resources;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Iterator;

import static org.assertj.core.api.Assertions.assertThat;

public class FileStreamTest {

  File file;

  FileStream sut;

  @Before
  public void setUp() throws Exception {
    file = new File(Resources.getResource(getClass(), "FileStreamTest/file.txt").getFile());
  }

  @After
  public void tearDown() throws Exception {
    if (sut != null) {
      sut.close();
    }
  }

  @Test
  public void read_lines() throws Exception {
    sut = new FileStream(file);

    Iterator<String> lines = sut.iterator();
    assertThat(lines.next()).isEqualTo("line1");
    assertThat(lines.next()).isEqualTo("line2");
    assertThat(lines.next()).isEqualTo("line3");
  }

  @Test(expected = IllegalStateException.class)
  public void fail_to_get_iterator_twice() throws Exception {
    sut = new FileStream(file);
    sut.iterator();

    // Fail !
    sut.iterator();
  }

  @Test
  public void not_fail_when_close_without_calling_iterator() throws Exception {
    sut = new FileStream(file);
    sut.close();
  }
}
