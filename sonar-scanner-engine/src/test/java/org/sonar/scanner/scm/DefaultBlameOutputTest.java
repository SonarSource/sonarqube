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
package org.sonar.scanner.scm;

import java.util.Arrays;
import java.util.Date;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.scm.BlameLine;

public class DefaultBlameOutputTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void shouldNotFailIfNotSameNumberOfLines() {
    InputFile file = new TestInputFileBuilder("foo", "src/main/java/Foo.java").setLines(10).build();

    new DefaultBlameOutput(null, Arrays.asList(file)).blameResult(file, Arrays.asList(new BlameLine().revision("1").author("guy")));
  }

  @Test
  public void shouldFailIfNotExpectedFile() {
    InputFile file = new TestInputFileBuilder("foo", "src/main/java/Foo.java").build();

    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("It was not expected to blame file src/main/java/Foo.java");

    new DefaultBlameOutput(null, Arrays.<InputFile>asList(new TestInputFileBuilder("foo", "src/main/java/Foo2.java").build()))
      .blameResult(file, Arrays.asList(new BlameLine().revision("1").author("guy")));
  }

  @Test
  public void shouldFailIfNullDate() {
    InputFile file = new TestInputFileBuilder("foo", "src/main/java/Foo.java").setLines(1).build();

    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Blame date is null for file src/main/java/Foo.java at line 1");

    new DefaultBlameOutput(null, Arrays.<InputFile>asList(file))
      .blameResult(file, Arrays.asList(new BlameLine().revision("1").author("guy")));
  }

  @Test
  public void shouldFailIfNullRevision() {
    InputFile file = new TestInputFileBuilder("foo", "src/main/java/Foo.java").setLines(1).build();

    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Blame revision is blank for file src/main/java/Foo.java at line 1");

    new DefaultBlameOutput(null, Arrays.<InputFile>asList(file))
      .blameResult(file, Arrays.asList(new BlameLine().date(new Date()).author("guy")));
  }

}
