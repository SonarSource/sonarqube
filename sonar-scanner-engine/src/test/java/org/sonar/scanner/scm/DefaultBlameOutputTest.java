/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.scm.BlameLine;
import org.sonar.scanner.index.BatchComponent;
import org.sonar.scanner.index.BatchComponentCache;
import org.sonar.scanner.scm.DefaultBlameOutput;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DefaultBlameOutputTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private BatchComponentCache componentCache;

  @Before
  public void prepare() {
    componentCache = mock(BatchComponentCache.class);
    BatchComponent component = mock(BatchComponent.class);
    when(component.batchId()).thenReturn(1);
    when(componentCache.get(any(InputComponent.class))).thenReturn(component);
  }

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

  @Test
  public void shouldFailIfNullDate() {
    InputFile file = new DefaultInputFile("foo", "src/main/java/Foo.java").setLines(1);

    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Blame date is null for file src/main/java/Foo.java at line 1");

    new DefaultBlameOutput(null, componentCache, Arrays.<InputFile>asList(file))
      .blameResult(file, Arrays.asList(new BlameLine().revision("1").author("guy")));
  }

  @Test
  public void shouldFailIfNullRevision() {
    InputFile file = new DefaultInputFile("foo", "src/main/java/Foo.java").setLines(1);

    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Blame revision is blank for file src/main/java/Foo.java at line 1");

    new DefaultBlameOutput(null, componentCache, Arrays.<InputFile>asList(file))
      .blameResult(file, Arrays.asList(new BlameLine().date(new Date()).author("guy")));
  }

}
