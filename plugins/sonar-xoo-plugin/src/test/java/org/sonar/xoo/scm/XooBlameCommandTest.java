/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.xoo.scm;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.function.Predicate;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.scm.BlameCommand.BlameInput;
import org.sonar.api.batch.scm.BlameCommand.BlameOutput;
import org.sonar.api.batch.scm.BlameLine;
import org.sonar.api.utils.DateUtils;
import org.sonar.xoo.Xoo;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class XooBlameCommandTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private DefaultFileSystem fs;
  private File baseDir;
  private BlameInput input;

  @Before
  public void prepare() throws IOException {
    baseDir = temp.newFolder();
    fs = new DefaultFileSystem(baseDir.toPath());
    input = mock(BlameInput.class);
    when(input.fileSystem()).thenReturn(fs);
  }

  @Test
  public void testBlame() throws IOException {
    File source = new File(baseDir, "src/foo.xoo");
    FileUtils.write(source, "sample content");
    File scm = new File(baseDir, "src/foo.xoo.scm");
    FileUtils.write(scm, "123,julien,2014-12-12\n234,julien,2014-12-24");
    DefaultInputFile inputFile = new TestInputFileBuilder("foo", "src/foo.xoo")
      .setLanguage(Xoo.KEY)
      .setModuleBaseDir(baseDir.toPath())
      .build();
    fs.add(inputFile);
    BlameOutput result = mock(BlameOutput.class);
    when(input.filesToBlame()).thenReturn(Arrays.asList(inputFile));

    new XooBlameCommand().blame(input, result);

    verify(result).blameResult(inputFile, Arrays.asList(
      new BlameLine().revision("123").author("julien").date(DateUtils.parseDate("2014-12-12")),
      new BlameLine().revision("234").author("julien").date(DateUtils.parseDate("2014-12-24"))));
  }

  @Test
  public void testBlameWithRelativeDate() throws IOException {
    File source = new File(baseDir, "src/foo.xoo");
    FileUtils.write(source, "sample content");
    File scm = new File(baseDir, "src/foo.xoo.scm");
    FileUtils.write(scm, "123,julien,-10\n234,julien,-10");
    DefaultInputFile inputFile = new TestInputFileBuilder("foo", "src/foo.xoo")
      .setLanguage(Xoo.KEY)
      .setModuleBaseDir(baseDir.toPath())
      .build();
    fs.add(inputFile);
    BlameOutput result = mock(BlameOutput.class);
    when(input.filesToBlame()).thenReturn(Arrays.asList(inputFile));

    new XooBlameCommand().blame(input, result);

    Predicate<Date> datePredicate = argument -> {
      Date approximate = DateUtils.addDays(new Date(), -10);
      return argument.getTime() > approximate.getTime() - 5000 && argument.getTime() < approximate.getTime() + 5000;
    };
    ArgumentCaptor<List<BlameLine>> blameLinesCaptor = ArgumentCaptor.forClass(List.class);
    verify(result).blameResult(eq(inputFile), blameLinesCaptor.capture());
    assertThat(blameLinesCaptor.getValue())
      .extracting(BlameLine::date)
      .allMatch(datePredicate);
  }

  @Test
  public void blame_containing_author_with_comma() throws IOException {
    File source = new File(baseDir, "src/foo.xoo");
    FileUtils.write(source, "sample content");
    File scm = new File(baseDir, "src/foo.xoo.scm");
    FileUtils.write(scm, "\"123\",\"john,doe\",\"2019-01-22\"");
    DefaultInputFile inputFile = new TestInputFileBuilder("foo", "src/foo.xoo")
      .setLanguage(Xoo.KEY)
      .setModuleBaseDir(baseDir.toPath())
      .build();
    fs.add(inputFile);
    BlameOutput result = mock(BlameOutput.class);
    when(input.filesToBlame()).thenReturn(Arrays.asList(inputFile));

    new XooBlameCommand().blame(input, result);

    verify(result).blameResult(inputFile, singletonList(
      new BlameLine().revision("123").author("john,doe").date(DateUtils.parseDate("2019-01-22"))));
  }
}
