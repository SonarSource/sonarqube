/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.batch.scan.filesystem;

import org.sonar.api.scan.filesystem.InputFile;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.scan.filesystem.FileQuery;
import org.sonar.api.scan.filesystem.internal.InputFileFilter;
import org.sonar.batch.bootstrap.AnalysisMode;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FileQueryFilterTest {

  private AnalysisMode mode;

  @Before
  public void before() {
    mode = mock(AnalysisMode.class);
  }

  @Test
  public void wrap_query_on_attributes() throws Exception {
    FileQuery query = FileQuery.onSource();
    FileQueryFilter filter = new FileQueryFilter(mode, query);

    assertThat(filter.filters()).hasSize(1);
    InputFileFilter typeFilter = filter.filters().get(0);
    assertThat(typeFilter).isInstanceOf(AttributeFilter.class);
    assertThat(((AttributeFilter) typeFilter).key()).isEqualTo(InputFile.ATTRIBUTE_TYPE);
    assertThat(((AttributeFilter) typeFilter).values()).containsOnly(InputFile.TYPE_MAIN);
  }

  @Test
  public void wrap_query_on_inclusions() throws Exception {
    FileQuery query = FileQuery.on().withInclusions("Foo*.java");
    FileQueryFilter filter = new FileQueryFilter(mode, query);

    assertThat(filter.filters()).hasSize(1);
    InputFileFilter inclusionFilter = filter.filters().get(0);
    assertThat(inclusionFilter).isInstanceOf(InclusionFilter.class);
    assertThat(inclusionFilter.toString()).isEqualTo("Includes: Foo*.java");
  }

  @Test
  public void wrap_query_on_exclusions() throws Exception {
    FileQuery query = FileQuery.on().withExclusions("Foo*.java");
    FileQueryFilter filter = new FileQueryFilter(mode, query);

    assertThat(filter.filters()).hasSize(1);
    InputFileFilter exclusionFilter = filter.filters().get(0);
    assertThat(exclusionFilter).isInstanceOf(ExclusionFilter.class);
    assertThat(exclusionFilter.toString()).isEqualTo("Excludes: Foo*.java");
  }

  @Test
  public void all_files_by_default() throws Exception {
    FileQuery query = FileQuery.on();
    FileQueryFilter filter = new FileQueryFilter(mode, query);
    assertThat(filter.filters()).isEmpty();
  }

  @Test
  public void only_changed_files_by_default_if_incremental_mode() throws Exception {
    when(mode.isIncremental()).thenReturn(true);

    FileQuery query = FileQuery.on();
    FileQueryFilter filter = new FileQueryFilter(mode, query);

    assertThat(filter.filters()).hasSize(1);
    AttributeFilter statusFilter = (AttributeFilter) filter.filters().get(0);
    assertThat(statusFilter.key()).isEqualTo(InputFile.ATTRIBUTE_STATUS);
    assertThat(statusFilter.values()).containsOnly(InputFile.STATUS_ADDED, InputFile.STATUS_CHANGED);
  }

  @Test
  public void get_all_files_even_if_incremental_mode() throws Exception {
    when(mode.isIncremental()).thenReturn(true);

    FileQuery query = FileQuery.on().on(InputFile.ATTRIBUTE_STATUS, InputFile.STATUS_SAME);
    FileQueryFilter filter = new FileQueryFilter(mode, query);

    assertThat(filter.filters()).hasSize(1);
    AttributeFilter statusFilter = (AttributeFilter) filter.filters().get(0);
    assertThat(statusFilter.key()).isEqualTo(InputFile.ATTRIBUTE_STATUS);
    assertThat(statusFilter.values()).containsOnly(InputFile.STATUS_SAME);
  }
}
