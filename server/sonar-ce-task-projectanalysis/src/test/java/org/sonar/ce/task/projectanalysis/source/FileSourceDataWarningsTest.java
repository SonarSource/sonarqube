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
package org.sonar.ce.task.projectanalysis.source;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.Arrays;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.api.utils.System2;
import org.sonar.ce.task.log.CeTaskMessages;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.ReportComponent;
import org.sonar.ce.task.projectanalysis.source.linereader.LineReader;

import static org.apache.commons.lang3.RandomStringUtils.secure;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.ce.task.projectanalysis.source.linereader.LineReader.Data.HIGHLIGHTING;
import static org.sonar.ce.task.projectanalysis.source.linereader.LineReader.Data.SYMBOLS;

@RunWith(DataProviderRunner.class)
public class FileSourceDataWarningsTest {

  private CeTaskMessages taskMessages = mock(CeTaskMessages.class);
  private System2 system2 = mock(System2.class);
  private Random random = new Random();
  private int line = 1 + new Random().nextInt(200);
  private long timeStamp = 9_887L + new Random().nextInt(300);
  private String path = secure().nextAlphabetic(50);

  private FileSourceDataWarnings underTest = new FileSourceDataWarnings(taskMessages, system2);

  @Test
  public void addWarning_fails_with_NPE_if_file_is_null() {
    LineReader.ReadError readError = new LineReader.ReadError(HIGHLIGHTING, 2);

    assertThatThrownBy(() -> underTest.addWarning(null, readError))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("file can't be null");
  }

  @Test
  public void addWarning_fails_with_NPE_if_readError_is_null() {
    Component component = mock(Component.class);

    assertThatThrownBy(() -> underTest.addWarning(component, null))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("readError can't be null");
  }

  @Test
  public void addWarnings_fails_with_ISE_if_called_after_commitWarnings() {
    underTest.commitWarnings();

    assertThatThrownBy(() -> underTest.addWarning(null /*doesn't matter*/, null /*doesn't matter*/))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("warnings already commit");
  }

  @Test
  public void commitWarnings_fails_with_ISE_if_called_after_commitWarnings() {
    underTest.commitWarnings();

    assertThatThrownBy(() -> underTest.commitWarnings())
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("warnings already commit");
  }

  @Test
  public void create_highlighting_warning_when_one_file_HIGHLIGHT_read_error() {
    ReportComponent file = ReportComponent.builder(Component.Type.FILE, 1)
      .setUuid("uuid")
      .setName(path)
      .build();
    LineReader.ReadError readError = new LineReader.ReadError(HIGHLIGHTING, line);
    when(system2.now()).thenReturn(timeStamp);

    underTest.addWarning(file, readError);

    verifyNoInteractions(taskMessages);

    underTest.commitWarnings();

    verify(taskMessages, times(1))
      .add(new CeTaskMessages.Message("Inconsistent highlighting data detected on file '" + path + "'. " +
        "File source may have been modified while analysis was running.", timeStamp));
  }

  @Test
  public void create_highlighting_warning_when_any_number_of_read_error_for_one_file() {
    ReportComponent file = ReportComponent.builder(Component.Type.FILE, 1)
      .setUuid("uuid")
      .setName(path)
      .build();
    LineReader.ReadError[] readErrors = IntStream.range(0, 1 + random.nextInt(10))
      .mapToObj(i -> new LineReader.ReadError(HIGHLIGHTING, line + i))
      .toArray(LineReader.ReadError[]::new);
    when(system2.now()).thenReturn(timeStamp);

    Arrays.stream(readErrors).forEach(readError -> underTest.addWarning(file, readError));

    verifyNoInteractions(taskMessages);

    underTest.commitWarnings();

    verify(taskMessages, times(1))
      .add(new CeTaskMessages.Message("Inconsistent highlighting data detected on file '" + path + "'. " +
        "File source may have been modified while analysis was running.", timeStamp));
  }

  @Test
  public void create_highlighting_warning_when_any_number_of_read_error_for_less_than_5_files() {
    int fileCount = 2 + random.nextInt(3);
    Component[] files = IntStream.range(0, fileCount)
      .mapToObj(i -> ReportComponent.builder(Component.Type.FILE, i)
        .setUuid("uuid_" + i)
        .setName(path + "_" + i)
        .build())
      .toArray(Component[]::new);
    when(system2.now()).thenReturn(timeStamp);

    Arrays.stream(files).forEach(file -> IntStream.range(0, 1 + random.nextInt(10))
      .forEach(i -> underTest.addWarning(file, new LineReader.ReadError(HIGHLIGHTING, line + i))));

    verifyNoInteractions(taskMessages);

    underTest.commitWarnings();

    String expectedMessage = "Inconsistent highlighting data detected on some files (" + fileCount + " in total). " +
      "File source may have been modified while analysis was running." +
      Arrays.stream(files).map(Component::getName).collect(Collectors.joining("\n   ° ", "\n   ° ", ""));
    verify(taskMessages, times(1))
      .add(new CeTaskMessages.Message(expectedMessage, timeStamp));
  }

  @Test
  public void create_highlighting_warning_when_any_number_of_read_error_for_more_than_5_files_only_the_5_first_by_ref() {
    int fileCount = 6 + random.nextInt(4);
    Component[] files = IntStream.range(0, fileCount)
      .mapToObj(i -> ReportComponent.builder(Component.Type.FILE, i)
        .setUuid("uuid_" + i)
        .setName(path + "_" + i)
        .build())
      .toArray(Component[]::new);
    when(system2.now()).thenReturn(timeStamp);

    Arrays.stream(files).forEach(file -> IntStream.range(0, 1 + random.nextInt(10))
      .forEach(i -> underTest.addWarning(file, new LineReader.ReadError(HIGHLIGHTING, line + i))));

    verifyNoInteractions(taskMessages);

    underTest.commitWarnings();

    String expectedMessage = "Inconsistent highlighting data detected on some files (" + fileCount + " in total). " +
      "File source may have been modified while analysis was running." +
      Arrays.stream(files).limit(5).map(Component::getName).collect(Collectors.joining("\n   ° ", "\n   ° ", ""));
    verify(taskMessages, times(1))
      .add(new CeTaskMessages.Message(expectedMessage, timeStamp));
  }

  @Test
  public void create_symbol_warning_when_one_file_HIGHLIGHT_read_error() {
    ReportComponent file = ReportComponent.builder(Component.Type.FILE, 1)
      .setUuid("uuid")
      .setName(path)
      .build();
    LineReader.ReadError readError = new LineReader.ReadError(SYMBOLS, line);
    when(system2.now()).thenReturn(timeStamp);

    underTest.addWarning(file, readError);

    verifyNoInteractions(taskMessages);

    underTest.commitWarnings();

    verify(taskMessages, times(1))
      .add(new CeTaskMessages.Message("Inconsistent symbol data detected on file '" + path + "'. " +
        "File source may have been modified while analysis was running.", timeStamp));
  }

  @Test
  public void create_symbol_warning_when_any_number_of_read_error_for_one_file() {
    ReportComponent file = ReportComponent.builder(Component.Type.FILE, 1)
      .setUuid("uuid")
      .setName(path)
      .build();
    LineReader.ReadError[] readErrors = IntStream.range(0, 1 + random.nextInt(10))
      .mapToObj(i -> new LineReader.ReadError(SYMBOLS, line + i))
      .toArray(LineReader.ReadError[]::new);
    when(system2.now()).thenReturn(timeStamp);

    Arrays.stream(readErrors).forEach(readError -> underTest.addWarning(file, readError));

    verifyNoInteractions(taskMessages);

    underTest.commitWarnings();

    verify(taskMessages, times(1))
      .add(new CeTaskMessages.Message("Inconsistent symbol data detected on file '" + path + "'. " +
        "File source may have been modified while analysis was running.", timeStamp));
  }

  @Test
  public void create_symbol_warning_when_any_number_of_read_error_for_less_than_5_files() {
    int fileCount = 2 + random.nextInt(3);
    Component[] files = IntStream.range(0, fileCount)
      .mapToObj(i -> ReportComponent.builder(Component.Type.FILE, i)
        .setUuid("uuid_" + i)
        .setName(path + "_" + i)
        .build())
      .toArray(Component[]::new);
    when(system2.now()).thenReturn(timeStamp);

    Arrays.stream(files).forEach(file -> IntStream.range(0, 1 + random.nextInt(10))
      .forEach(i -> underTest.addWarning(file, new LineReader.ReadError(SYMBOLS, line + i))));

    verifyNoInteractions(taskMessages);

    underTest.commitWarnings();

    String expectedMessage = "Inconsistent symbol data detected on some files (" + fileCount + " in total). " +
      "File source may have been modified while analysis was running." +
      Arrays.stream(files).map(Component::getName).collect(Collectors.joining("\n   ° ", "\n   ° ", ""));
    verify(taskMessages, times(1))
      .add(new CeTaskMessages.Message(expectedMessage, timeStamp));
  }

  @Test
  public void create_symbol_warning_when_any_number_of_read_error_for_more_than_5_files_only_the_5_first_by_ref() {
    int fileCount = 6 + random.nextInt(4);
    Component[] files = IntStream.range(0, fileCount)
      .mapToObj(i -> ReportComponent.builder(Component.Type.FILE, i)
        .setUuid("uuid_" + i)
        .setName(path + "_" + i)
        .build())
      .toArray(Component[]::new);
    when(system2.now()).thenReturn(timeStamp);

    Arrays.stream(files).forEach(file -> IntStream.range(0, 1 + random.nextInt(10))
      .forEach(i -> underTest.addWarning(file, new LineReader.ReadError(SYMBOLS, line + i))));

    verifyNoInteractions(taskMessages);

    underTest.commitWarnings();

    String expectedMessage = "Inconsistent symbol data detected on some files (" + fileCount + " in total). " +
      "File source may have been modified while analysis was running." +
      Arrays.stream(files).limit(5).map(Component::getName).collect(Collectors.joining("\n   ° ", "\n   ° ", ""));
    verify(taskMessages, times(1))
      .add(new CeTaskMessages.Message(expectedMessage, timeStamp));
  }

  @Test
  @UseDataProvider("anyDataButHighlightAndSymbols")
  public void creates_no_warning_when_read_error_for_anything_but_highlighting_and_symbols(LineReader.Data data) {
    ReportComponent file = ReportComponent.builder(Component.Type.FILE, 1)
      .setUuid("uuid")
      .setName(path)
      .build();
    LineReader.ReadError readError = new LineReader.ReadError(data, line);
    when(system2.now()).thenReturn(timeStamp);

    underTest.addWarning(file, readError);

    verifyNoInteractions(taskMessages);

    underTest.commitWarnings();

    verifyNoInteractions(taskMessages);
  }

  @DataProvider
  public static Object[][] anyDataButHighlightAndSymbols() {
    return Arrays.stream(LineReader.Data.values())
      .filter(t -> t != HIGHLIGHTING && t != SYMBOLS)
      .map(t -> new Object[] {t})
      .toArray(Object[][]::new);
  }
}
