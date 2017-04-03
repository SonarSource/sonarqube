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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;
import com.google.common.base.Supplier;
import java.util.function.Function;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.scanner.protocol.output.ScannerReport;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Iterables.toArray;
import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.trimToNull;
import static org.sonar.core.component.ComponentKeys.createEffectiveKey;
import static org.sonar.core.component.ComponentKeys.createKey;
import static org.sonar.core.util.stream.MoreCollectors.toList;

public class ComponentRootBuilder {
  private static final String DEFAULT_PROJECT_VERSION = "not provided";

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
  /**
   * Will supply the ComponentDto of the project (if it exists) if we need it to get the name of the project
   *
   * @see #nameOfProject(ScannerReport.Component, String, Supplier)
   */
  private final Supplier<Optional<ComponentDto>> projectDtoSupplier;
  /**
   * Will supply the SnapshotDto of the base analysis of the project (if it exists) if we need it to get the version
   * of the project.
   * <p>
   * The String argument of the {@link Function#apply(Object)} method is the project's UUID.
   * </p>
   *
   * @see #createProjectVersion(ScannerReport.Component, String, Function)
   */
  private final Function<String, Optional<SnapshotDto>> analysisSupplier;
  @CheckForNull
  private final String branch;

  public ComponentRootBuilder(@Nullable String branch,
    Function<String, String> uuidSupplier,
    Function<Integer, ScannerReport.Component> scannerComponentSupplier,
    Supplier<Optional<ComponentDto>> projectDtoSupplier,
    Function<String, Optional<SnapshotDto>> analysisSupplier) {
    this.uuidSupplier = uuidSupplier;
    this.scannerComponentSupplier = scannerComponentSupplier;
    this.projectDtoSupplier = projectDtoSupplier;
    this.branch = branch;
    this.analysisSupplier = analysisSupplier;
  }

  public Component build(ScannerReport.Component reportProject, String projectKey) {
    return buildComponent(reportProject, projectKey);
  }

  private ComponentImpl buildComponent(ScannerReport.Component reportComponent, String latestModuleKey) {
    switch (reportComponent.getType()) {
      case PROJECT:
        return buildProjectComponent(reportComponent, latestModuleKey);
      case MODULE:
        String moduleKey = createKey(reportComponent.getKey(), branch);
        return buildOtherComponent(reportComponent, moduleKey, moduleKey);
      case DIRECTORY:
      case FILE:
        return buildOtherComponent(reportComponent, createEffectiveKey(latestModuleKey, reportComponent.getPath()), latestModuleKey);
      default:
        throw new IllegalArgumentException(format("Unsupported component type '%s'", reportComponent.getType()));
    }
  }

  private ComponentImpl buildProjectComponent(ScannerReport.Component reportComponent, String latestModuleKey) {
    ComponentImpl.Builder builder = createCommonBuilder(reportComponent, latestModuleKey, latestModuleKey);
    return builder
      .setName(nameOfProject(reportComponent, latestModuleKey, projectDtoSupplier))
      .setReportAttributes(createProjectReportAttributes(reportComponent, builder.getUuid(), analysisSupplier))
      .build();
  }

  private ComponentImpl buildOtherComponent(ScannerReport.Component reportComponent, String componentKey, String latestModuleKey) {
    return createCommonBuilder(reportComponent, componentKey, latestModuleKey)
      .setName(nameOfOthers(reportComponent, componentKey))
      .setReportAttributes(createOtherReportAttributes(reportComponent))
      .build();
  }

  private ComponentImpl.Builder createCommonBuilder(ScannerReport.Component reportComponent, String componentKey, String latestModuleKey) {
    return ComponentImpl.builder(convertType(reportComponent.getType()))
      .setUuid(uuidSupplier.apply(componentKey))
      .setKey(componentKey)
      .setDescription(trimToNull(reportComponent.getDescription()))
      .setFileAttributes(createFileAttributes(reportComponent))
      .addChildren(toArray(buildChildren(reportComponent, latestModuleKey), Component.class));
  }

  private Iterable<Component> buildChildren(ScannerReport.Component component, String latestModuleKey) {
    return component.getChildRefList()
      .stream()
      .map(componentRef -> buildComponent(scannerComponentSupplier.apply(componentRef), latestModuleKey))
      .collect(toList(component.getChildRefList().size()));
  }

  private static String nameOfProject(ScannerReport.Component project, String projectKey, Supplier<Optional<ComponentDto>> projectDtoSupplier) {
    String name = trimToNull(project.getName());
    if (name == null) {
      return projectDtoSupplier.get().transform(ComponentDto::name).or(projectKey);
    }
    return name;
  }

  private static String nameOfOthers(ScannerReport.Component reportComponent, String componentKey) {
    String name = trimToNull(reportComponent.getName());
    return name == null ? componentKey : name;
  }

  @VisibleForTesting
  static ReportAttributes createProjectReportAttributes(ScannerReport.Component component,
    String projectUuid, Function<String, Optional<SnapshotDto>> analysisSupplier) {
    return createCommonBuilder(component)
      .setVersion(createProjectVersion(component, projectUuid, analysisSupplier))
      .build();
  }

  private static String createProjectVersion(ScannerReport.Component component,
    String projectUuid, Function<String, Optional<SnapshotDto>> analysisSupplier) {
    String version = trimToNull(component.getVersion());
    if (version != null) {
      return version;
    }
    Optional<SnapshotDto> snapshotDto = analysisSupplier.apply(projectUuid);
    if (snapshotDto.isPresent()) {
      return MoreObjects.firstNonNull(snapshotDto.get().getVersion(), DEFAULT_PROJECT_VERSION);
    }
    return DEFAULT_PROJECT_VERSION;
  }

  @VisibleForTesting
  static ReportAttributes createOtherReportAttributes(ScannerReport.Component component) {
    return createCommonBuilder(component)
      .setVersion(trimToNull(component.getVersion()))
      .build();
  }

  private static ReportAttributes.Builder createCommonBuilder(ScannerReport.Component component) {
    return ReportAttributes.newBuilder(component.getRef())
      .setPath(trimToNull(component.getPath()));
  }

  @VisibleForTesting
  @CheckForNull
  static FileAttributes createFileAttributes(ScannerReport.Component component) {
    if (component.getType() != ScannerReport.Component.ComponentType.FILE) {
      return null;
    }

    checkArgument(component.getLines() > 0, "File '%s' has no line", component.getPath());
    return new FileAttributes(
      component.getIsTest(),
      trimToNull(component.getLanguage()),
      component.getLines());
  }

  @VisibleForTesting
  static Component.Type convertType(ScannerReport.Component.ComponentType type) {
    switch (type) {
      case PROJECT:
        return Component.Type.PROJECT;
      case MODULE:
        return Component.Type.MODULE;
      case DIRECTORY:
        return Component.Type.DIRECTORY;
      case FILE:
        return Component.Type.FILE;
      default:
        throw new IllegalArgumentException("Unsupported ComponentType value " + type);
    }
  }
}
