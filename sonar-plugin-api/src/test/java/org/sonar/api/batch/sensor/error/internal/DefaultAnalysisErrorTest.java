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
package org.sonar.api.batch.sensor.error.internal;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextPointer;
import org.sonar.api.batch.fs.internal.DefaultTextPointer;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.sensor.error.NewAnalysisError;
import org.sonar.api.batch.sensor.internal.SensorStorage;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;

public class DefaultAnalysisErrorTest {
  private InputFile inputFile;
  private SensorStorage storage;
  private TextPointer textPointer;

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Before
  public void setUp() {
    inputFile = new TestInputFileBuilder("module1", "src/File.java").build();
    textPointer = new DefaultTextPointer(5, 2);
    storage = mock(SensorStorage.class);
  }

  @Test
  public void test_analysis_error() {
    DefaultAnalysisError analysisError = new DefaultAnalysisError(storage);
    analysisError.onFile(inputFile)
      .at(textPointer)
      .message("msg");

    assertThat(analysisError.location()).isEqualTo(textPointer);
    assertThat(analysisError.message()).isEqualTo("msg");
    assertThat(analysisError.inputFile()).isEqualTo(inputFile);
  }

  @Test
  public void test_save() {
    DefaultAnalysisError analysisError = new DefaultAnalysisError(storage);
    analysisError.onFile(inputFile).save();

    verify(storage).store(analysisError);
    verifyNoMoreInteractions(storage);
  }

  @Test
  public void test_no_storage() {
    exception.expect(NullPointerException.class);
    DefaultAnalysisError analysisError = new DefaultAnalysisError();
    analysisError.onFile(inputFile).save();
  }

  @Test
  public void test_validation() {
    try {
      new DefaultAnalysisError(storage).onFile(null);
      fail("Expected exception");
    } catch (IllegalArgumentException e) {
      // expected
    }

    NewAnalysisError error = new DefaultAnalysisError(storage).onFile(inputFile);
    try {
      error.onFile(inputFile);
      fail("Expected exception");
    } catch (IllegalStateException e) {
      // expected
    }

    error = new DefaultAnalysisError(storage).at(textPointer);
    try {
      error.at(textPointer);
      fail("Expected exception");
    } catch (IllegalStateException e) {
      // expected
    }

    try {
      new DefaultAnalysisError(storage).save();
      fail("Expected exception");
    } catch (NullPointerException e) {
      // expected
    }
  }
}
