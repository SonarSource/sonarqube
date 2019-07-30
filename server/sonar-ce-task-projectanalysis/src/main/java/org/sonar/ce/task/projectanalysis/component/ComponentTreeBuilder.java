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

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.ce.task.projectanalysis.analysis.Branch;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.scanner.protocol.output.ScannerReport.Component.FileStatus;
import org.sonar.server.project.Project;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang.StringUtils.removeStart;
import static org.apache.commons.lang.StringUtils.trimToNull;

public class ComponentTreeBuilder {

  private final ComponentKeyGenerator keyGenerator;
  private final ComponentKeyGenerator publicKeyGenerator;
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
  private final Branch branch;
  @Nullable
  private final ProjectAttributes projectAttributes;

  private ScannerReport.Component rootComponent;
  private String scmBasePath;

  public ComponentTreeBuilder(
    ComponentKeyGenerator keyGenerator,
    ComponentKeyGenerator publicKeyGenerator,
    Function<String, String> uuidSupplier,
    Function<Integer, ScannerReport.Component> scannerComponentSupplier,
    Project project,
    Branch branch,
    ProjectAttributes projectAttributes) {

    this.keyGenerator = keyGenerator;
    this.publicKeyGenerator = publicKeyGenerator;
    this.uuidSupplier = uuidSupplier;
    this.scannerComponentSupplier = scannerComponentSupplier;
    this.project = project;
    this.branch = branch;
    this.projectAttributes = requireNonNull(projectAttributes, "projectAttributes can't be null");
  }

  public Component buildProject(ScannerReport.Component project, String scmBasePath) {
    this.rootComponent = project;
    this.scmBasePath = trimToNull(scmBasePath);

    Node root = createProjectHierarchy(project);
    return buildComponent(root, "", "");
  }

  private Node createProjectHierarchy(ScannerReport.Component rootComponent) {
    Preconditions.checkArgument(rootComponent.getType() == ScannerReport.Component.ComponentType.PROJECT, "Expected root component of type 'PROJECT'");

    LinkedList<ScannerReport.Component> queue = new LinkedList<>();
    rootComponent.getChildRefList()
      .stream()
      .map(scannerComponentSupplier)
      .forEach(queue::addLast);

    Node root = new Node();
    root.reportComponent = rootComponent;

    while (!queue.isEmpty()) {
      ScannerReport.Component component = queue.removeFirst();
      switch (component.getType()) {
        case FILE:
          addFile(root, component);
          break;
        default:
          throw new IllegalArgumentException(format("Unsupported component type '%s'", component.getType()));
      }
    }
    return root;
  }

  private static void addFile(Node root, ScannerReport.Component file) {
    Preconditions.checkArgument(!StringUtils.isEmpty(file.getProjectRelativePath()), "Files should have a project relative path: " + file);
    String[] split = StringUtils.split(file.getProjectRelativePath(), '/');
    Node currentNode = root;

    for (int i = 0; i < split.length; i++) {
      currentNode = currentNode.children().computeIfAbsent(split[i], k -> new Node());
    }
    currentNode.reportComponent = file;
  }

  private Component buildComponent(Node node, String currentPath, String parentPath) {
    List<Component> childComponents = buildChildren(node, currentPath);
    ScannerReport.Component component = node.reportComponent();

    if (component != null) {
      if (component.getType() == ScannerReport.Component.ComponentType.FILE) {
        return buildFile(component);
      } else if (component.getType() == ScannerReport.Component.ComponentType.PROJECT) {
        return buildProject(childComponents);
      }
    }

    return buildDirectory(parentPath, currentPath, childComponents);
  }

  private List<Component> buildChildren(Node node, String currentPath) {
    List<Component> children = new ArrayList<>();

    for (Map.Entry<String, Node> e : node.children().entrySet()) {
      String path = buildPath(currentPath, e.getKey());
      Node childNode = e.getValue();

      // collapse folders that only contain one folder
      while (childNode.children().size() == 1 && childNode.children().values().iterator().next().children().size() > 0) {
        Map.Entry<String, Node> childEntry = childNode.children().entrySet().iterator().next();
        path = buildPath(path, childEntry.getKey());
        childNode = childEntry.getValue();
      }
      children.add(buildComponent(childNode, path, currentPath));
    }
    return children;
  }

  private static String buildPath(String currentPath, String file) {
    if (currentPath.isEmpty()) {
      return file;
    }
    return currentPath + "/" + file;
  }

  private Component buildProject(List<Component> children) {
    String projectKey = keyGenerator.generateKey(rootComponent.getKey(), null);
    String uuid = uuidSupplier.apply(projectKey);
    String projectPublicKey = publicKeyGenerator.generateKey(rootComponent.getKey(), null);
    ComponentImpl.Builder builder = ComponentImpl.builder(Component.Type.PROJECT)
      .setUuid(uuid)
      .setDbKey(projectKey)
      .setKey(projectPublicKey)
      .setStatus(convertStatus(rootComponent.getStatus()))
      .setProjectAttributes(projectAttributes)
      .setReportAttributes(createAttributesBuilder(rootComponent.getRef(), rootComponent.getProjectRelativePath(), scmBasePath).build())
      .addChildren(children);
    setNameAndDescription(rootComponent, builder);
    return builder.build();
  }

  private ComponentImpl buildFile(ScannerReport.Component component) {
    String key = keyGenerator.generateKey(rootComponent.getKey(), component.getProjectRelativePath());
    String publicKey = publicKeyGenerator.generateKey(rootComponent.getKey(), component.getProjectRelativePath());
    return ComponentImpl.builder(Component.Type.FILE)
      .setUuid(uuidSupplier.apply(key))
      .setDbKey(key)
      .setKey(publicKey)
      .setName(component.getProjectRelativePath())
      .setShortName(FilenameUtils.getName(component.getProjectRelativePath()))
      .setStatus(convertStatus(component.getStatus()))
      .setDescription(trimToNull(component.getDescription()))
      .setReportAttributes(createAttributesBuilder(component.getRef(), component.getProjectRelativePath(), scmBasePath).build())
      .setFileAttributes(createFileAttributes(component))
      .build();
  }

  private ComponentImpl buildDirectory(String parentPath, String path, List<Component> children) {
    String key = keyGenerator.generateKey(rootComponent.getKey(), path);
    String publicKey = publicKeyGenerator.generateKey(rootComponent.getKey(), path);
    return ComponentImpl.builder(Component.Type.DIRECTORY)
      .setUuid(uuidSupplier.apply(key))
      .setDbKey(key)
      .setKey(publicKey)
      .setName(path)
      .setShortName(removeStart(removeStart(path, parentPath), "/"))
      .setStatus(convertStatus(FileStatus.UNAVAILABLE))
      .setReportAttributes(createAttributesBuilder(null, path, scmBasePath).build())
      .addChildren(children)
      .build();
  }

  public Component buildChangedComponentTreeRoot(Component project) {
    return buildChangedComponentTree(project);
  }

  private static ComponentImpl.Builder changedComponentBuilder(Component component) {
    return ComponentImpl.builder(component.getType())
      .setUuid(component.getUuid())
      .setDbKey(component.getDbKey())
      .setKey(component.getKey())
      .setStatus(component.getStatus())
      .setReportAttributes(component.getReportAttributes())
      .setName(component.getName())
      .setShortName(component.getShortName())
      .setDescription(component.getDescription());
  }

  @Nullable
  private static Component buildChangedComponentTree(Component component) {
    switch (component.getType()) {
      case PROJECT:
        return buildChangedProject(component);
      case DIRECTORY:
        return buildChangedIntermediate(component);
      case FILE:
        return buildChangedFile(component);

      default:
        throw new IllegalArgumentException(format("Unsupported component type '%s'", component.getType()));
    }
  }

  private static Component buildChangedProject(Component component) {
    return changedComponentBuilder(component)
      .setProjectAttributes(new ProjectAttributes(component.getProjectAttributes()))
      .addChildren(buildChangedComponentChildren(component))
      .build();
  }

  @Nullable
  private static Component buildChangedIntermediate(Component component) {
    List<Component> children = buildChangedComponentChildren(component);
    if (children.isEmpty()) {
      return null;
    }
    return changedComponentBuilder(component)
      .addChildren(children)
      .build();
  }

  @Nullable
  private static Component buildChangedFile(Component component) {
    if (component.getStatus() == Component.Status.SAME) {
      return null;
    }
    return changedComponentBuilder(component)
      .setFileAttributes(component.getFileAttributes())
      .build();
  }

  private static List<Component> buildChangedComponentChildren(Component component) {
    return component.getChildren()
      .stream()
      .map(ComponentTreeBuilder::buildChangedComponentTree)
      .filter(Objects::nonNull)
      .collect(MoreCollectors.toList());
  }

  private void setNameAndDescription(ScannerReport.Component component, ComponentImpl.Builder builder) {
    if (branch.isMain()) {
      builder
        .setName(nameOfProject(component))
        .setDescription(component.getDescription());
    } else {
      builder
        .setName(project.getName())
        .setDescription(project.getDescription());
    }
  }

  private static Component.Status convertStatus(FileStatus status) {
    switch (status) {
      case ADDED:
        return Component.Status.ADDED;
      case SAME:
        return Component.Status.SAME;
      case CHANGED:
        return Component.Status.CHANGED;
      case UNAVAILABLE:
        return Component.Status.UNAVAILABLE;
      case UNRECOGNIZED:
      default:
        throw new IllegalArgumentException("Unsupported ComponentType value " + status);
    }
  }

  private String nameOfProject(ScannerReport.Component component) {
    String name = trimToNull(component.getName());
    if (name != null) {
      return name;
    }
    return project.getName();
  }

  private static ReportAttributes.Builder createAttributesBuilder(@Nullable Integer ref, String path, @Nullable String scmBasePath) {
    return ReportAttributes.newBuilder(ref)
      .setScmPath(computeScmPath(scmBasePath, path));
  }

  @CheckForNull
  private static String computeScmPath(@Nullable String scmBasePath, String scmRelativePath) {
    if (scmRelativePath.isEmpty()) {
      return scmBasePath;
    }
    if (scmBasePath == null) {
      return scmRelativePath;
    }

    return scmBasePath + '/' + scmRelativePath;
  }

  private static FileAttributes createFileAttributes(ScannerReport.Component component) {
    checkArgument(component.getType() == ScannerReport.Component.ComponentType.FILE);
    checkArgument(component.getLines() > 0, "File '%s' has no line", component.getProjectRelativePath());
    String lang = trimToNull(component.getLanguage());
    return new FileAttributes(
      component.getIsTest(),
      lang != null ? lang.intern() : null,
      component.getLines());
  }

  private static class Node {
    private final Map<String, Node> children = new LinkedHashMap<>();
    private ScannerReport.Component reportComponent;

    private Map<String, Node> children() {
      return children;
    }

    @CheckForNull
    private ScannerReport.Component reportComponent() {
      return reportComponent;
    }
  }
}
