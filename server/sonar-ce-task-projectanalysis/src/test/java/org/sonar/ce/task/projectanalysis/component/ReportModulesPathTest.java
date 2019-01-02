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
package org.sonar.ce.task.projectanalysis.component;

import java.util.Arrays;
import java.util.Map;
import javax.annotation.Nullable;
import org.junit.Test;
import org.sonar.ce.task.projectanalysis.batch.BatchReportReader;
import org.sonar.scanner.protocol.output.ScannerReport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class ReportModulesPathTest {
  private BatchReportReader reader = mock(BatchReportReader.class);
  private ReportModulesPath reportModulesPath = new ReportModulesPath(reader);
  private ScannerReport.Component root = addComponent(1, "project", ScannerReport.Component.ComponentType.PROJECT, null, 2);

  @Test
  public void should_not_read_hierarchy_if_metadata_available() {
    when(reader.readMetadata()).thenReturn(ScannerReport.Metadata.newBuilder()
      .putModulesProjectRelativePathByKey("module1", "path1")
      .setRootComponentRef(1)
      .build());
    Map<String, String> pathByModuleKey = reportModulesPath.get();

    assertThat(pathByModuleKey).containsExactly(entry("module1", "path1"));
    verify(reader).readMetadata();
    verifyNoMoreInteractions(reader);
  }

  @Test
  public void should_read_hierarchy_if_metadata_not_available() {
    when(reader.readMetadata()).thenReturn(ScannerReport.Metadata.newBuilder().setRootComponentRef(1).build());
    addComponent(2, "project:module1", ScannerReport.Component.ComponentType.MODULE, "path1", 3);
    addComponent(3, "project:module1:module2", ScannerReport.Component.ComponentType.MODULE, "path1/path2", 4);
    addComponent(4, "project:module1:module2:dir", ScannerReport.Component.ComponentType.DIRECTORY, "path1/path2/dir");

    Map<String, String> pathByModuleKey = reportModulesPath.get();

    assertThat(pathByModuleKey).containsOnly(
      entry("project:module1", "path1"),
      entry("project:module1:module2", "path1/path2"));
    verify(reader).readMetadata();
    verify(reader).readComponent(1);
    verify(reader).readComponent(2);
    verify(reader).readComponent(3);
    verify(reader).readComponent(4);

    verifyNoMoreInteractions(reader);
  }

  private ScannerReport.Component addComponent(int ref, String key, ScannerReport.Component.ComponentType type, @Nullable String path, Integer... children) {
    ScannerReport.Component.Builder builder = ScannerReport.Component.newBuilder()
      .setRef(ref)
      .setKey(key)
      .addAllChildRef(Arrays.asList(children))
      .setType(type);

    if (path != null) {
      builder.setProjectRelativePath(path);
    }
    ScannerReport.Component component = builder.build();
    when(reader.readComponent(ref)).thenReturn(component);
    return component;
  }
}
