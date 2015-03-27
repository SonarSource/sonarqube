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
package org.sonar.batch.scm;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.scm.BlameLine;

import java.util.Arrays;

public class DefaultBlameOutputTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void shouldNotFailIfNotSameNumberOfLines() {
    InputFile file = new DefaultInputFile("foo", "src/main/java/Foo.java").setLines(10);

    new DefaultBlameOutput(null, null, Arrays.asList(file)).blameResult(file, Arrays.asList(new BlameLine().revision("1").author("guy")));
  }

  @Test
  public void shouldFailIfNotExpectedFile() {
    InputFile file = new DefaultInputFile("foo", "src/main/java/Foo.java").setLines(1);

    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("It was not expected to blame file src/main/java/Foo.java");

    new DefaultBlameOutput(null, null, Arrays.<InputFile>asList(new DefaultInputFile("foo", "src/main/java/Foo2.java")))
      .blameResult(file, Arrays.asList(new BlameLine().revision("1").author("guy")));
  }

}
