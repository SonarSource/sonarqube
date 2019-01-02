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

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.function.Supplier;
import org.sonar.ce.task.projectanalysis.batch.BatchReportReader;
import org.sonar.scanner.protocol.output.ScannerReport;

public class ReportModulesPath implements Supplier<Map<String, String>> {
  private final BatchReportReader reader;

  public ReportModulesPath(BatchReportReader reader) {
    this.reader = reader;
  }

  public Map<String, String> get() {
    ScannerReport.Metadata metadata = reader.readMetadata();
    Map<String, String> modulesProjectRelativePathByKey = metadata.getModulesProjectRelativePathByKeyMap();
    if (modulesProjectRelativePathByKey.isEmpty()) {
      return collectModulesPathFromHierarchy(metadata);
    }
    return modulesProjectRelativePathByKey;
  }

  /**
   * This should only be needed if we receive a report of the previous version without the path per module explicitly set
   * (due to blue/green deployment)
   * Can be removed in any future version
   */
  private Map<String, String> collectModulesPathFromHierarchy(ScannerReport.Metadata metadata) {
    ScannerReport.Component root = reader.readComponent(metadata.getRootComponentRef());
    Map<String, String> modulesPathByKey = new LinkedHashMap<>();
    LinkedList<Integer> queue = new LinkedList<>();
    queue.addAll(root.getChildRefList());

    while (!queue.isEmpty()) {
      ScannerReport.Component component = reader.readComponent(queue.removeFirst());
      if (component.getType() == ScannerReport.Component.ComponentType.MODULE) {
        queue.addAll(component.getChildRefList());
        modulesPathByKey.put(component.getKey(), component.getProjectRelativePath());
      }
    }

    return modulesPathByKey;
  }

}
