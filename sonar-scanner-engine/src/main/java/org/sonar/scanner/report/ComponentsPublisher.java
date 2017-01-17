/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import javax.annotation.CheckForNull;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.fs.InputDir;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputModule;
import org.sonar.api.batch.fs.InputPath;
import org.sonar.api.batch.fs.internal.DefaultInputComponent;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.batch.fs.internal.InputComponentTree;
import org.sonar.api.batch.fs.internal.InputModuleHierarchy;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.scanner.protocol.output.ScannerReport.Component.ComponentType;
import org.sonar.scanner.protocol.output.ScannerReport.ComponentLink;
import org.sonar.scanner.protocol.output.ScannerReport.ComponentLink.ComponentLinkType;
import org.sonar.scanner.protocol.output.ScannerReportWriter;

/**
 * Adds components and analysis metadata to output report
 */
public class ComponentsPublisher implements ReportPublisherStep {

  private InputComponentTree componentTree;
  private InputModuleHierarchy moduleHierarchy;

  public ComponentsPublisher(InputModuleHierarchy moduleHierarchy, InputComponentTree inputComponentTree) {
    this.moduleHierarchy = moduleHierarchy;
    this.componentTree = inputComponentTree;
  }

  @Override
  public void publish(ScannerReportWriter writer) {
    recursiveWriteComponent((DefaultInputComponent) moduleHierarchy.root(), writer);
  }

  private void recursiveWriteComponent(DefaultInputComponent component, ScannerReportWriter writer) {
    ScannerReport.Component.Builder builder = ScannerReport.Component.newBuilder();

    // non-null fields
    builder.setRef(component.batchId());
    builder.setType(getType(component));

    // Don't set key on directories and files to save space since it can be deduced from path
    if (component instanceof InputModule) {
      DefaultInputModule inputModule = (DefaultInputModule) component;
      // Here we want key without branch
      builder.setKey(inputModule.key());

      // protocol buffers does not accept null values
      String name = getName(inputModule);
      if (name != null) {
        builder.setName(name);
      }
      String description = getDescription(inputModule);
      if (description != null) {
        builder.setDescription(description);
      }

      writeVersion(inputModule, builder);
    }

    if (component.isFile()) {
      InputFile file = (InputFile) component;
      builder.setIsTest(file.type() == InputFile.Type.TEST);
      builder.setLines(file.lines());

      String lang = getLanguageKey(file);
      if (lang != null) {
        builder.setLanguage(lang);
      }
    }

    String path = getPath(component);
    if (path != null) {
      builder.setPath(path);
    }

    for (InputComponent child : componentTree.getChildren(component)) {
      builder.addChildRef(((DefaultInputComponent) child).batchId());
    }
    writeLinks(component, builder);
    writer.writeComponent(builder.build());

    for (InputComponent child : componentTree.getChildren(component)) {
      recursiveWriteComponent((DefaultInputComponent) child, writer);
    }
  }

  private void writeVersion(DefaultInputModule module, ScannerReport.Component.Builder builder) {
    ProjectDefinition def = module.definition();
    String version = getVersion(def);
    if (version != null) {
      builder.setVersion(version);
    }
  }

  @CheckForNull
  private String getPath(InputComponent component) {
    if (component instanceof InputPath) {
      InputPath inputPath = (InputPath) component;
      if (StringUtils.isEmpty(inputPath.relativePath())) {
        return "/";
      } else {
        return inputPath.relativePath();
      }
    } else if (component instanceof InputModule) {
      InputModule module = (InputModule) component;
      return moduleHierarchy.relativePath(module);
    }
    throw new IllegalStateException("Unkown component: " + component.getClass());
  }

  private static String getVersion(ProjectDefinition def) {
    String version = def.getOriginalVersion();
    if (StringUtils.isNotBlank(version)) {
      return version;
    }

    return def.getParent() != null ? getVersion(def.getParent()) : null;
  }

  private void writeLinks(InputComponent c, ScannerReport.Component.Builder builder) {
    if (c instanceof InputModule) {
      DefaultInputModule inputModule = (DefaultInputModule) c;
      ProjectDefinition def = inputModule.definition();
      ComponentLink.Builder linkBuilder = ComponentLink.newBuilder();

      writeProjectLink(builder, def, linkBuilder, CoreProperties.LINKS_HOME_PAGE, ComponentLinkType.HOME);
      writeProjectLink(builder, def, linkBuilder, CoreProperties.LINKS_CI, ComponentLinkType.CI);
      writeProjectLink(builder, def, linkBuilder, CoreProperties.LINKS_ISSUE_TRACKER, ComponentLinkType.ISSUE);
      writeProjectLink(builder, def, linkBuilder, CoreProperties.LINKS_SOURCES, ComponentLinkType.SCM);
      writeProjectLink(builder, def, linkBuilder, CoreProperties.LINKS_SOURCES_DEV, ComponentLinkType.SCM_DEV);
    }
  }

  private static void writeProjectLink(ScannerReport.Component.Builder componentBuilder, ProjectDefinition def, ComponentLink.Builder linkBuilder, String linkProp,
    ComponentLinkType linkType) {
    String link = def.properties().get(linkProp);
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
  private static String getName(DefaultInputModule module) {
    if (StringUtils.isNotEmpty(module.definition().getBranch())) {
      return module.definition().getOriginalName() + " " + module.definition().getBranch();
    } else {
      return module.definition().getOriginalName();
    }
  }

  @CheckForNull
  private static String getDescription(DefaultInputModule module) {
    return module.definition().getDescription();
  }

  private ComponentType getType(InputComponent r) {
    if (r instanceof InputFile) {
      return ComponentType.FILE;
    } else if (r instanceof InputDir) {
      return ComponentType.DIRECTORY;
    } else if ((r instanceof InputModule) && moduleHierarchy.isRoot((InputModule) r)) {
      return ComponentType.PROJECT;
    } else if (r instanceof InputModule) {
      return ComponentType.MODULE;
    }

    throw new IllegalArgumentException("Unknown resource type: " + r);
  }

}
