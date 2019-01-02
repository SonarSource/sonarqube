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
package org.sonar.ce.task.projectanalysis.filemove;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.Arrays;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.ce.task.projectanalysis.component.Component;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(DataProviderRunner.class)
public class AddedFileRepositoryImplTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private AnalysisMetadataHolder analysisMetadataHolder = mock(AnalysisMetadataHolder.class);
  private AddedFileRepositoryImpl underTest = new AddedFileRepositoryImpl(analysisMetadataHolder);

  @Test
  public void isAdded_fails_with_NPE_if_component_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("component can't be null");

    underTest.isAdded(null);
  }

  @Test
  public void isAdded_returns_true_for_any_component_type_on_first_analysis() {
    when(analysisMetadataHolder.isFirstAnalysis()).thenReturn(true);

    Arrays.stream(Component.Type.values()).forEach(type -> {
      Component component = newComponent(type);

      assertThat(underTest.isAdded(component)).isTrue();
    });
  }

  @Test
  public void isAdded_returns_false_for_unregistered_component_type_when_not_on_first_analysis() {
    when(analysisMetadataHolder.isFirstAnalysis()).thenReturn(false);

    Arrays.stream(Component.Type.values()).forEach(type -> {
      Component component = newComponent(type);

      assertThat(underTest.isAdded(component)).isFalse();
    });
  }

  @Test
  public void isAdded_returns_true_for_registered_file_when_not_on_first_analysis() {
    when(analysisMetadataHolder.isFirstAnalysis()).thenReturn(false);
    Component file1 = newComponent(Component.Type.FILE);
    Component file2 = newComponent(Component.Type.FILE);
    underTest.register(file1);

    assertThat(underTest.isAdded(file1)).isTrue();
    assertThat(underTest.isAdded(file2)).isFalse();
  }

  @Test
  public void register_fails_with_NPE_if_component_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("component can't be null");

    underTest.register(null);
  }

  @Test
  @UseDataProvider("anyTypeButFile")
  public void register_fails_with_IAE_if_component_is_not_a_file(Component.Type anyTypeButFile) {
    Component component = newComponent(anyTypeButFile);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("component must be a file");

    underTest.register(component);
  }

  @DataProvider
  public static Object[][] anyTypeButFile() {
    return Arrays.stream(Component.Type.values())
      .filter(t -> t != Component.Type.FILE)
      .map(t -> new Object[] {t})
      .toArray(Object[][]::new);
  }

  @Test
  public void register_fails_with_ISE_if_called_on_first_analysis() {
    when(analysisMetadataHolder.isFirstAnalysis()).thenReturn(true);
    Component component = newComponent(Component.Type.FILE);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("No file can be registered on first analysis");

    underTest.register(component);
  }

  private static Component newComponent(Component.Type type) {
    Component component = mock(Component.class);
    when(component.getType()).thenReturn(type);
    return component;
  }
}
