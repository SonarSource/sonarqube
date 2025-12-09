/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.api.batch.sensor.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.data.MapEntry.entry;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.Test;

public class InMemorySensorStorageTest {

  InMemorySensorStorage underTest = new InMemorySensorStorage();

  @Test
  public void test_storeProperty() {
    assertThat(underTest.contextProperties).isEmpty();

    underTest.storeProperty("foo", "bar");
    assertThat(underTest.contextProperties).containsOnly(entry("foo", "bar"));
  }

  @Test
  public void storeProperty_throws_IAE_if_key_is_null() {
    assertThatThrownBy(() -> underTest.storeProperty(null, "bar"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Key of context property must not be null");
  }

  @Test
  public void storeProperty_throws_IAE_if_value_is_null() {
    assertThatThrownBy(() -> underTest.storeProperty("foo", null))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Value of context property must not be null");
  }

  @Test
  public void test_storeAnalysisData() {
    // Given
    String key = "analysisKey";
    String mimeType = "mimeType";
    String dataString = "analysisData";
    ByteArrayInputStream dataStream = new ByteArrayInputStream(dataString.getBytes(StandardCharsets.UTF_8));

    // When
    underTest.storeAnalysisData(key, mimeType, dataStream);

    // Then
    assertThat(underTest.analysisDataEntries).containsKey(key);
    assertThat(new String(underTest.analysisDataEntries.get(key).data(), StandardCharsets.UTF_8)).isEqualTo(dataString);
  }

  @Test
  public void storeAnalysisData_throws_UOE_if_operation_not_supported() {
    underTest.storeAnalysisData("unsupportedKey", "mimeType", new ByteArrayInputStream("dummyData".getBytes(StandardCharsets.UTF_8)));
    ByteArrayInputStream dataStream = new ByteArrayInputStream("newData".getBytes(StandardCharsets.UTF_8));

    assertThatThrownBy(() -> underTest.storeAnalysisData("unsupportedKey", "mimeType", dataStream))
      .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  public void storeAnalysisData_throws_IOE_on_data_handling_error() {
    InputStream faultyStream = new InputStream() {
      @Override
      public int read() throws IOException {
        throw new IOException("Simulated IO Exception");
      }
    };

    assertThatThrownBy(() -> underTest.storeAnalysisData("validKey", "mimeType", faultyStream))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("Failed to read data from InputStream");
  }
  
  
}
