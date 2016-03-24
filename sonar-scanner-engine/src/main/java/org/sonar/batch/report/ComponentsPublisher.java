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
package org.sonar.batch.report;

import javax.annotation.CheckForNull;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.ResourceUtils;
import org.sonar.batch.index.BatchComponent;
import org.sonar.batch.index.BatchComponentCache;
import org.sonar.batch.scan.ImmutableProjectReactor;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.scanner.protocol.output.ScannerReport.Component.ComponentType;
import org.sonar.scanner.protocol.output.ScannerReport.ComponentLink;
import org.sonar.scanner.protocol.output.ScannerReport.ComponentLink.ComponentLinkType;
import org.sonar.scanner.protocol.output.ScannerReportWriter;

/**
 * Adds components and analysis metadata to output report
 */
public class ComponentsPublisher implements ReportPublisherStep {

  private final BatchComponentCache resourceCache;
  private final ImmutableProjectReactor reactor;

  public ComponentsPublisher(ImmutableProjectReactor reactor, BatchComponentCache resourceCache) {
    this.reactor = reactor;
    this.resourceCache = resourceCache;
  }

  @Override
  public void publish(ScannerReportWriter writer) {
    BatchComponent rootProject = resourceCache.get(reactor.getRoot().getKeyWithBranch());
    recursiveWriteComponent(rootProject, writer);
  }

  private void recursiveWriteComponent(BatchComponent batchComponent, ScannerReportWriter writer) {
    Resource r = batchComponent.resource();
    ScannerReport.Component.Builder builder = ScannerReport.Component.newBuilder();

    // non-null fields
    builder.setRef(batchComponent.batchId());
    builder.setType(getType(r));

    // Don't set key on directories and files to save space since it can be deduced from path
    if (batchComponent.isProjectOrModule()) {
      // Here we want key without branch
      ProjectDefinition def = reactor.getProjectDefinition(batchComponent.key());
      builder.setKey(def.getKey());
    }

    // protocol buffers does not accept null values

    if (batchComponent.isFile()) {
      builder.setIsTest(ResourceUtils.isUnitTestFile(r));
      builder.setLines(((InputFile) batchComponent.inputComponent()).lines());
    }
    String name = getName(r);
    if (name != null) {
      builder.setName(name);
    }
    String description = getDescription(r);
    if (description != null) {
      builder.setDescription(description);
    }
    String path = r.getPath();
    if (path != null) {
      builder.setPath(path);
    }
    String lang = getLanguageKey(r);
    if (lang != null) {
      builder.setLanguage(lang);
    }
    for (BatchComponent child : batchComponent.children()) {
      builder.addChildRef(child.batchId());
    }
    writeLinks(batchComponent, builder);
    writeVersion(batchComponent, builder);
    writer.writeComponent(builder.build());

    for (BatchComponent child : batchComponent.children()) {
      recursiveWriteComponent(child, writer);
    }
  }

  private void writeVersion(BatchComponent c, ScannerReport.Component.Builder builder) {
    if (c.isProjectOrModule()) {
      ProjectDefinition def = reactor.getProjectDefinition(c.key());
      String version = getVersion(def);
      builder.setVersion(version);
    }
  }

  private static String getVersion(ProjectDefinition def) {
    String version = def.getVersion();
    return StringUtils.isNotBlank(version) ? version : getVersion(def.getParent());
  }

  private void writeLinks(BatchComponent c, ScannerReport.Component.Builder builder) {
    if (c.isProjectOrModule()) {
      ProjectDefinition def = reactor.getProjectDefinition(c.key());
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
  private static String getLanguageKey(Resource r) {
    Language language = r.getLanguage();
    return ResourceUtils.isFile(r) && language != null ? language.getKey() : null;
  }

  @CheckForNull
  private static String getName(Resource r) {
    // Don't return name for directories and files since it can be guessed from the path
    return (ResourceUtils.isFile(r) || ResourceUtils.isDirectory(r)) ? null : r.getName();
  }

  @CheckForNull
  private static String getDescription(Resource r) {
    // Only for projets and modules
    return ResourceUtils.isProject(r) ? r.getDescription() : null;
  }

  private static ComponentType getType(Resource r) {
    if (ResourceUtils.isFile(r)) {
      return ComponentType.FILE;
    } else if (ResourceUtils.isDirectory(r)) {
      return ComponentType.DIRECTORY;
    } else if (ResourceUtils.isModuleProject(r)) {
      return ComponentType.MODULE;
    } else if (ResourceUtils.isRootProject(r)) {
      return ComponentType.PROJECT;
    }
    throw new IllegalArgumentException("Unknown resource type: " + r);
  }

}
