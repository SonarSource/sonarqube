/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.computation.task.projectanalysis.component;

import java.util.function.Function;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.db.component.SnapshotDto;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.server.computation.task.projectanalysis.analysis.Project;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.trimToNull;

public class ComponentTreeBuilder {

  private static final String DEFAULT_PROJECT_VERSION = "not provided";

  private final ComponentKeyGenerator keyGenerator;
  /**
   * Will supply the UUID for any component in the tree, given it's key.
   * <p>
   * The String argument of the {@link Function#apply(Object)} method is the component's key.
   * </p>
   */
  private final Function<String, String> uuidSupplier;

  /**
   * Will supply the {@link ScannerReport.Component} of all the components in the component tree as we crawl it from the
   * root.
   * <p>
   * The Integer argument of the {@link Function#apply(Object)} method is the component's ref.
   * </p>
   */
  private final Function<Integer, ScannerReport.Component> scannerComponentSupplier;

  private final Project project;

  @Nullable
  private final SnapshotDto baseAnalysis;

  public ComponentTreeBuilder(
    ComponentKeyGenerator keyGenerator,
    Function<String, String> uuidSupplier,
    Function<Integer, ScannerReport.Component> scannerComponentSupplier,
    Project project,
    @Nullable SnapshotDto baseAnalysis) {

    this.keyGenerator = keyGenerator;
    this.uuidSupplier = uuidSupplier;
    this.scannerComponentSupplier = scannerComponentSupplier;
    this.project = project;
    this.baseAnalysis = baseAnalysis;
  }

  public Component buildProject(ScannerReport.Component project) {
    return buildComponent(project, project);
  }

  private Component[] buildChildren(ScannerReport.Component component, ScannerReport.Component parentModule) {
    return component.getChildRefList()
      .stream()
      .map(componentRef -> buildComponent(scannerComponentSupplier.apply(componentRef), parentModule))
      .toArray(Component[]::new);
  }

  private ComponentImpl buildComponent(ScannerReport.Component component, ScannerReport.Component closestModule) {
    switch (component.getType()) {
      case PROJECT:
        String projectKey = keyGenerator.generateKey(component, null);
        String uuid = uuidSupplier.apply(projectKey);
        return ComponentImpl.builder(Component.Type.PROJECT)
          .setUuid(uuid)
          .setKey(projectKey)
          .setName(nameOfProject(component))
          .setDescription(trimToNull(component.getDescription()))
          .setReportAttributes(createAttributesBuilder(component)
            .setVersion(createProjectVersion(component))
            .build())
          .addChildren(buildChildren(component, component))
          .build();

      case MODULE:
        String moduleKey = keyGenerator.generateKey(component, null);
        return ComponentImpl.builder(Component.Type.MODULE)
          .setUuid(uuidSupplier.apply(moduleKey))
          .setKey(moduleKey)
          .setName(nameOfOthers(component, moduleKey))
          .setDescription(trimToNull(component.getDescription()))
          .setReportAttributes(createAttributesBuilder(component).build())
          .addChildren(buildChildren(component, component))
          .build();

      case DIRECTORY:
      case FILE:
        String key = keyGenerator.generateKey(closestModule, component);
        return ComponentImpl.builder(convertDirOrFileType(component.getType()))
          .setUuid(uuidSupplier.apply(key))
          .setKey(key)
          .setName(nameOfOthers(component, key))
          .setDescription(trimToNull(component.getDescription()))
          .setReportAttributes(createAttributesBuilder(component).build())
          .setFileAttributes(createFileAttributes(component))
          .addChildren(buildChildren(component, closestModule))
          .build();

      default:
        throw new IllegalArgumentException(format("Unsupported component type '%s'", component.getType()));
    }
  }

  private String nameOfProject(ScannerReport.Component component) {
    String name = trimToNull(component.getName());
    if (name != null) {
      return name;
    }
    return project.getName();
  }

  private static String nameOfOthers(ScannerReport.Component reportComponent, String defaultName) {
    String name = trimToNull(reportComponent.getName());
    return name == null ? defaultName : name;
  }

  private String createProjectVersion(ScannerReport.Component component) {
    String version = trimToNull(component.getVersion());
    if (version != null) {
      return version;
    }
    if (baseAnalysis != null) {
      return firstNonNull(baseAnalysis.getVersion(), DEFAULT_PROJECT_VERSION);
    }
    return DEFAULT_PROJECT_VERSION;
  }

  private static ReportAttributes.Builder createAttributesBuilder(ScannerReport.Component component) {
    return ReportAttributes.newBuilder(component.getRef())
      .setVersion(trimToNull(component.getVersion()))
      .setPath(trimToNull(component.getPath()));
  }

  @CheckForNull
  private static FileAttributes createFileAttributes(ScannerReport.Component component) {
    if (component.getType() != ScannerReport.Component.ComponentType.FILE) {
      return null;
    }

    checkArgument(component.getLines() > 0, "File '%s' has no line", component.getPath());
    return new FileAttributes(
      component.getIsTest(),
      trimToNull(component.getLanguage()),
      component.getLines());
  }

  private static Component.Type convertDirOrFileType(ScannerReport.Component.ComponentType type) {
    switch (type) {
      case DIRECTORY:
        return Component.Type.DIRECTORY;
      case FILE:
        return Component.Type.FILE;
      default:
        throw new IllegalArgumentException("Unsupported ComponentType value " + type);
    }
  }
}
