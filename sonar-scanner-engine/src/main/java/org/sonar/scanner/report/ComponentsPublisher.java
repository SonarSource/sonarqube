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
package org.sonar.scanner.report;

import java.nio.file.Path;
import java.util.Collection;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.fs.InputDir;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputFile.Status;
import org.sonar.api.batch.fs.InputModule;
import org.sonar.api.batch.fs.internal.DefaultInputComponent;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.batch.fs.internal.InputComponentTree;
import org.sonar.api.batch.fs.internal.InputModuleHierarchy;
import org.sonar.core.util.CloseableIterator;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.scanner.protocol.output.ScannerReport.Component.ComponentType;
import org.sonar.scanner.protocol.output.ScannerReport.Component.FileStatus;
import org.sonar.scanner.protocol.output.ScannerReport.ComponentLink;
import org.sonar.scanner.protocol.output.ScannerReport.ComponentLink.ComponentLinkType;
import org.sonar.scanner.protocol.output.ScannerReport.Issue;
import org.sonar.scanner.protocol.output.ScannerReportReader;
import org.sonar.scanner.protocol.output.ScannerReportWriter;
import org.sonar.scanner.scan.branch.BranchConfiguration;

/**
 * Adds components and analysis metadata to output report
 */
public class ComponentsPublisher implements ReportPublisherStep {

  private final InputComponentTree componentTree;
  private final InputModuleHierarchy moduleHierarchy;
  private final BranchConfiguration branchConfiguration;

  private ScannerReportReader reader;
  private ScannerReportWriter writer;

  public ComponentsPublisher(InputModuleHierarchy moduleHierarchy, InputComponentTree inputComponentTree, BranchConfiguration branchConfiguration) {
    this.moduleHierarchy = moduleHierarchy;
    this.componentTree = inputComponentTree;
    this.branchConfiguration = branchConfiguration;
  }

  @Override
  public void publish(ScannerReportWriter writer) {
    this.reader = new ScannerReportReader(writer.getFileStructure().root());
    this.writer = writer;
    recursiveWriteComponent((DefaultInputComponent) moduleHierarchy.root());
  }

  /**
   * Writes the tree of components recursively, deep-first. 
   * @return true if component was written (not skipped)
   */
  private boolean recursiveWriteComponent(DefaultInputComponent component) {
    Collection<InputComponent> children = componentTree.getChildren(component).stream()
      .filter(c -> recursiveWriteComponent((DefaultInputComponent) c))
      .collect(Collectors.toList());

    if (shouldSkipComponent(component, children)) {
      return false;
    }

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
    } else if (component.isFile()) {
      DefaultInputFile file = (DefaultInputFile) component;
      builder.setIsTest(file.type() == InputFile.Type.TEST);
      builder.setLines(file.lines());
      builder.setStatus(convert(file.status()));

      String lang = getLanguageKey(file);
      if (lang != null) {
        builder.setLanguage(lang);
      }
    }

    String path = getPath(component);
    if (path != null) {
      builder.setPath(path);

      String projectRelativePath = getProjectRelativePath(component);
      if (projectRelativePath != null) {
        builder.setProjectRelativePath(projectRelativePath);
      }
    }

    for (InputComponent child : children) {
      builder.addChildRef(((DefaultInputComponent) child).batchId());
    }
    writeLinks(component, builder);
    writer.writeComponent(builder.build());
    return true;
  }

  private FileStatus convert(Status status) {
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

  private boolean shouldSkipComponent(DefaultInputComponent component, Collection<InputComponent> children) {
    if (component instanceof InputModule && children.isEmpty() && branchConfiguration.isShortLivingBranch()) {
      // no children on a module in short branch analysis -> skip it (except root)
      return !moduleHierarchy.isRoot((InputModule) component);
    } else if (component instanceof InputDir && children.isEmpty()) {
      try (CloseableIterator<Issue> componentIssuesIt = reader.readComponentIssues(component.batchId())) {
        if (!componentIssuesIt.hasNext()) {
          // no files to publish on a directory without issues -> skip it
          return true;
        }
      }
    } else if (component instanceof DefaultInputFile) {
      // skip files not marked for publishing
      DefaultInputFile inputFile = (DefaultInputFile) component;
      return !inputFile.isPublished() || (branchConfiguration.isShortLivingBranch() && inputFile.status() == Status.SAME);
    }
    return false;
  }

  private void writeVersion(DefaultInputModule module, ScannerReport.Component.Builder builder) {
    String version = getVersion(module);
    if (version != null) {
      builder.setVersion(version);
    }
  }

  @CheckForNull
  private String getPath(InputComponent component) {
    if (component instanceof InputFile) {
      DefaultInputFile inputPath = (DefaultInputFile) component;
      return inputPath.getModuleRelativePath();
    } else if (component instanceof InputDir) {
      InputDir inputPath = (InputDir) component;
      if (StringUtils.isEmpty(inputPath.relativePath())) {
        return "/";
      } else {
        return inputPath.relativePath();
      }
    } else if (component instanceof InputModule) {
      InputModule module = (InputModule) component;
      return moduleHierarchy.relativePath(module);
    }
    throw new IllegalStateException("Unknown component: " + component.getClass());
  }

  @CheckForNull
  private String getProjectRelativePath(DefaultInputComponent component) {
    if (component instanceof InputFile) {
      DefaultInputFile inputFile = (DefaultInputFile) component;
      return inputFile.getProjectRelativePath();
    }

    Path projectBaseDir = moduleHierarchy.root().getBaseDir();
    if (component instanceof InputDir) {
      InputDir inputDir = (InputDir) component;
      return projectBaseDir.relativize(inputDir.path()).toString();
    }
    if (component instanceof InputModule) {
      DefaultInputModule module = (DefaultInputModule) component;
      return projectBaseDir.relativize(module.getBaseDir()).toString();
    }
    throw new IllegalStateException("Unknown component: " + component.getClass());
  }

  private String getVersion(DefaultInputModule module) {
    String version = module.getOriginalVersion();
    if (StringUtils.isNotBlank(version)) {
      return version;
    }

    DefaultInputModule parent = moduleHierarchy.parent(module);

    return parent != null ? getVersion(parent) : null;
  }

  private static void writeLinks(InputComponent c, ScannerReport.Component.Builder builder) {
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
      return module.definition().getName() + " " + module.definition().getBranch();
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
