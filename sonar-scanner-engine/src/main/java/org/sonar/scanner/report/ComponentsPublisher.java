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
package org.sonar.scanner.report;

import java.util.Map;
import javax.annotation.CheckForNull;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputFile.Status;
import org.sonar.api.batch.fs.internal.AbstractProjectOrModule;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.DefaultInputProject;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.scanner.protocol.output.ScannerReport.Component.ComponentType;
import org.sonar.scanner.protocol.output.ScannerReport.Component.FileStatus;
import org.sonar.scanner.protocol.output.ScannerReport.ComponentLink;
import org.sonar.scanner.protocol.output.ScannerReport.ComponentLink.ComponentLinkType;
import org.sonar.scanner.protocol.output.ScannerReportWriter;
import org.sonar.scanner.scan.filesystem.InputComponentStore;

/**
 * Adds components and analysis metadata to output report
 */
public class ComponentsPublisher implements ReportPublisherStep {

  private final InputComponentStore inputComponentStore;
  private final DefaultInputProject project;


  public ComponentsPublisher(DefaultInputProject project, InputComponentStore inputComponentStore) {
    this.project = project;
    this.inputComponentStore = inputComponentStore;
  }

  @Override
  public void publish(ScannerReportWriter writer) {
    ScannerReport.Component.Builder projectBuilder = prepareProjectBuilder();

    ScannerReport.Component.Builder fileBuilder = ScannerReport.Component.newBuilder();
    for (DefaultInputFile file : inputComponentStore.allFilesToPublish()) {
      projectBuilder.addChildRef(file.scannerId());

      fileBuilder.clear();

      // non-null fields
      fileBuilder.setRef(file.scannerId());
      fileBuilder.setType(ComponentType.FILE);

      fileBuilder.setIsTest(file.type() == InputFile.Type.TEST);
      fileBuilder.setLines(file.lines());
      fileBuilder.setStatus(convert(file.status()));

      String lang = getLanguageKey(file);
      if (lang != null) {
        fileBuilder.setLanguage(lang);
      }
      fileBuilder.setProjectRelativePath(file.getProjectRelativePath());
      writer.writeComponent(fileBuilder.build());
    }

    writer.writeComponent(projectBuilder.build());
  }

  private ScannerReport.Component.Builder prepareProjectBuilder() {
    ScannerReport.Component.Builder projectBuilder = ScannerReport.Component.newBuilder();
    projectBuilder.setRef(project.scannerId());
    projectBuilder.setType(ComponentType.PROJECT);
    // Here we want key without branch
    projectBuilder.setKey(project.key());

    // protocol buffers does not accept null values
    String name = getName(project);
    if (name != null) {
      projectBuilder.setName(name);
    }
    String description = getDescription(project);
    if (description != null) {
      projectBuilder.setDescription(description);
    }

    writeLinks(project, projectBuilder);
    return projectBuilder;
  }

  private static FileStatus convert(Status status) {
    switch (status) {
      case ADDED:
        return FileStatus.ADDED;
      case CHANGED:
        return FileStatus.CHANGED;
      case SAME:
        return FileStatus.SAME;
      default:
        throw new IllegalArgumentException("Unexpected status: " + status);
    }
  }

  private static void writeLinks(DefaultInputProject project, ScannerReport.Component.Builder builder) {
    ComponentLink.Builder linkBuilder = ComponentLink.newBuilder();

    writeProjectLink(builder, project.properties(), linkBuilder, CoreProperties.LINKS_HOME_PAGE, ComponentLinkType.HOME);
    writeProjectLink(builder, project.properties(), linkBuilder, CoreProperties.LINKS_CI, ComponentLinkType.CI);
    writeProjectLink(builder, project.properties(), linkBuilder, CoreProperties.LINKS_ISSUE_TRACKER, ComponentLinkType.ISSUE);
    writeProjectLink(builder, project.properties(), linkBuilder, CoreProperties.LINKS_SOURCES, ComponentLinkType.SCM);
  }

  private static void writeProjectLink(ScannerReport.Component.Builder componentBuilder, Map<String, String> properties, ComponentLink.Builder linkBuilder, String linkProp,
                                       ComponentLinkType linkType) {
    String link = properties.get(linkProp);
    if (StringUtils.isNotBlank(link)) {
      linkBuilder.setType(linkType);
      linkBuilder.setHref(link);
      componentBuilder.addLink(linkBuilder.build());
      linkBuilder.clear();
    }
  }

  @CheckForNull
  private static String getLanguageKey(InputFile file) {
    return file.language();
  }

  @CheckForNull
  private static String getName(AbstractProjectOrModule module) {
    if (StringUtils.isNotEmpty(module.definition().getBranch())) {
      return module.definition().getName() + " " + module.definition().getBranch();
    } else {
      return module.definition().getOriginalName();
    }
  }

  @CheckForNull
  private static String getDescription(AbstractProjectOrModule module) {
    return module.definition().getDescription();
  }

}
