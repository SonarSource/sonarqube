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
package org.sonar.ce.task.projectanalysis.source;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.ce.task.projectanalysis.component.Component;

import static org.mockito.Mockito.mock;
import static org.sonar.ce.task.projectanalysis.component.ReportComponent.builder;

public class SourceLinesHashImplTest {
  private static final String FILE_UUID = "FILE_UUID";
  private static final String FILE_KEY = "FILE_KEY";

  @Rule
  public SourceLinesRepositoryRule sourceLinesRepository = new SourceLinesRepositoryRule();
  public SignificantCodeRepository significantCodeRepository = mock(SignificantCodeRepository.class);
  public SourceLinesHashCache cache = mock(SourceLinesHashCache.class);
  public DbLineHashVersion dbLineHashVersion = mock(DbLineHashVersion.class);

  private SourceLinesHashRepositoryImpl underTest = new SourceLinesHashRepositoryImpl(sourceLinesRepository, significantCodeRepository, cache, dbLineHashVersion);

  @Test
  public void should_generate_correct_version_of_line_hashes() {
    Component component = createComponent(1);

    underTest.getLineHashesMatchingDBVersion(component);

  }

  private static Component createComponent(int ref) {
    return builder(Component.Type.FILE, ref)
      .setKey(FILE_KEY)
      .setUuid(FILE_UUID)
      .build();
  }
}
