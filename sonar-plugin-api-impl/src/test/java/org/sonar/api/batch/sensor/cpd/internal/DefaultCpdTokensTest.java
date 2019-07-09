/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.api.batch.sensor.cpd.internal;

import org.junit.Test;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.cpd.internal.DefaultCpdTokens;
import org.sonar.api.batch.sensor.internal.SensorStorage;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

public class DefaultCpdTokensTest {
  private final SensorStorage sensorStorage = mock(SensorStorage.class);

  private final DefaultInputFile inputFile = new TestInputFileBuilder("foo", "src/Foo.java")
    .setLines(2)
    .setOriginalLineStartOffsets(new int[] {0, 50})
    .setOriginalLineEndOffsets(new int[] {49, 100})
    .setLastValidOffset(101)
    .build();

  @Test
  public void save_no_tokens() {
    DefaultCpdTokens tokens = new DefaultCpdTokens(sensorStorage)
      .onFile(inputFile);

    tokens.save();

    verify(sensorStorage).store(tokens);

    assertThat(tokens.inputFile()).isEqualTo(inputFile);
  }

  @Test
  public void save_one_token() {
    DefaultCpdTokens tokens = new DefaultCpdTokens(sensorStorage)
      .onFile(inputFile)
      .addToken(inputFile.newRange(1, 2, 1, 5), "foo");

    tokens.save();

    verify(sensorStorage).store(tokens);

    assertThat(tokens.getTokenLines()).extracting("value", "startLine", "hashCode", "startUnit", "endUnit").containsExactly(tuple("foo", 1, "foo".hashCode(), 1, 1));
  }

  @Test
  public void handle_exclusions() {
    inputFile.setExcludedForDuplication(true);
    DefaultCpdTokens tokens = new DefaultCpdTokens(sensorStorage)
      .onFile(inputFile)
      .addToken(inputFile.newRange(1, 2, 1, 5), "foo");

    tokens.save();

    verifyZeroInteractions(sensorStorage);

    assertThat(tokens.getTokenLines()).isEmpty();
  }

  @Test
  public void dont_save_for_test_files() {
    DefaultInputFile testInputFile = new TestInputFileBuilder("foo", "src/Foo.java")
      .setLines(2)
      .setOriginalLineStartOffsets(new int[] {0, 50})
      .setOriginalLineEndOffsets(new int[] {49, 100})
      .setLastValidOffset(101)
      .setType(InputFile.Type.TEST)
      .build();

    DefaultCpdTokens tokens = new DefaultCpdTokens(sensorStorage)
      .onFile(testInputFile)
      .addToken(testInputFile.newRange(1, 2, 1, 5), "foo");

    tokens.save();
    verifyZeroInteractions(sensorStorage);
    assertThat(tokens.getTokenLines()).isEmpty();
  }

  @Test
  public void save_many_tokens() {
    DefaultCpdTokens tokens = new DefaultCpdTokens(sensorStorage)
      .onFile(inputFile)
      .addToken(inputFile.newRange(1, 2, 1, 5), "foo")
      .addToken(inputFile.newRange(1, 6, 1, 10), "bar")
      .addToken(inputFile.newRange(1, 20, 1, 25), "biz")
      .addToken(inputFile.newRange(2, 1, 2, 10), "next");

    tokens.save();

    verify(sensorStorage).store(tokens);

    assertThat(tokens.getTokenLines())
      .extracting("value", "startLine", "hashCode", "startUnit", "endUnit")
      .containsExactly(
        tuple("foobarbiz", 1, "foobarbiz".hashCode(), 1, 3),
        tuple("next", 2, "next".hashCode(), 4, 4));
  }

  @Test
  public void basic_validation() {
    SensorStorage sensorStorage = mock(SensorStorage.class);
    DefaultCpdTokens tokens = new DefaultCpdTokens(sensorStorage);
    try {
      tokens.save();
      fail("Expected exception");
    } catch (Exception e) {
      assertThat(e).hasMessage("Call onFile() first");
    }
    try {
      tokens.addToken(inputFile.newRange(1, 2, 1, 5), "foo");
      fail("Expected exception");
    } catch (Exception e) {
      assertThat(e).hasMessage("Call onFile() first");
    }
    try {
      tokens.addToken(null, "foo");
      fail("Expected exception");
    } catch (Exception e) {
      assertThat(e).hasMessage("Range should not be null");
    }
    try {
      tokens.addToken(inputFile.newRange(1, 2, 1, 5), null);
      fail("Expected exception");
    } catch (Exception e) {
      assertThat(e).hasMessage("Image should not be null");
    }
  }

  @Test
  public void validate_tokens_order() {
    SensorStorage sensorStorage = mock(SensorStorage.class);
    DefaultCpdTokens tokens = new DefaultCpdTokens(sensorStorage)
      .onFile(inputFile)
      .addToken(inputFile.newRange(1, 6, 1, 10), "bar");

    try {
      tokens.addToken(inputFile.newRange(1, 2, 1, 5), "foo");
      fail("Expected exception");
    } catch (Exception e) {
      assertThat(e).hasMessage("Tokens of file src/Foo.java should be provided in order.\n" +
        "Previous token: Range[from [line=1, lineOffset=6] to [line=1, lineOffset=10]]\n" +
        "Last token: Range[from [line=1, lineOffset=2] to [line=1, lineOffset=5]]");
    }
  }

}
