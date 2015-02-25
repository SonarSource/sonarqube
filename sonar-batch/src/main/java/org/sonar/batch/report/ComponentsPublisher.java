/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.batch.report;

import org.sonar.api.batch.bootstrap.ProjectReactor;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.ResourceUtils;
import org.sonar.batch.index.BatchResource;
import org.sonar.batch.index.ResourceCache;
import org.sonar.batch.protocol.Constants;
import org.sonar.batch.protocol.output.BatchOutputWriter;
import org.sonar.batch.protocol.output.BatchReport;

import javax.annotation.CheckForNull;

/**
 * Adds components and analysis metadata to output report
 */
public class ComponentsPublisher implements ReportPublisher {

  private final ResourceCache resourceCache;
  private final ProjectReactor reactor;

  public ComponentsPublisher(ProjectReactor reactor, ResourceCache resourceCache) {
    this.reactor = reactor;
    this.resourceCache = resourceCache;
  }

  @Override
  public void publish(BatchOutputWriter writer) {
    BatchResource rootProject = resourceCache.get(reactor.getRoot().getKeyWithBranch());
    BatchReport.Metadata.Builder builder = BatchReport.Metadata.newBuilder()
      .setAnalysisDate(((Project) rootProject.resource()).getAnalysisDate().getTime())
      .setProjectKey(((Project) rootProject.resource()).key())
      .setRootComponentRef(rootProject.batchId());
    Integer sid = rootProject.snapshotId();
    if (sid != null) {
      builder.setSnapshotId(sid);
    }
    writer.writeMetadata(builder.build());
    recursiveWriteComponent(rootProject, writer);
  }

  private void recursiveWriteComponent(BatchResource batchResource, BatchOutputWriter writer) {
    Resource r = batchResource.resource();
    BatchReport.Component.Builder builder = BatchReport.Component.newBuilder();

    // non-null fields
    builder.setRef(batchResource.batchId());
    builder.setType(getType(r));

    // protocol buffers does not accept null values

    String uuid = r.getUuid();
    if (uuid != null) {
      builder.setUuid(uuid);
    }
    Integer sid = batchResource.snapshotId();
    if (sid != null) {
      builder.setSnapshotId(sid);
    }
    if (ResourceUtils.isFile(r)) {
      builder.setIsTest(ResourceUtils.isUnitTestClass(r));
    }
    String name = getName(r);
    if (name != null) {
      builder.setName(name);
    }
    String path = r.getPath();
    if (path != null) {
      builder.setPath(path);
    }
    String lang = getLanguageKey(r);
    if (lang != null) {
      builder.setLanguage(lang);
    }
    for (BatchResource child : batchResource.children()) {
      builder.addChildRefs(child.batchId());
    }
    writer.writeComponent(builder.build());

    for (BatchResource child : batchResource.children()) {
      recursiveWriteComponent(child, writer);
    }
  }

  @CheckForNull
  private String getLanguageKey(Resource r) {
    Language language = r.getLanguage();
    return ResourceUtils.isFile(r) && language != null ? language.getKey() : null;
  }

  @CheckForNull
  private String getName(Resource r) {
    // Don't return name for directories and files since it can be guessed from the path
    return (ResourceUtils.isFile(r) || ResourceUtils.isDirectory(r)) ? null : r.getName();
  }

  private Constants.ComponentType getType(Resource r) {
    if (ResourceUtils.isFile(r)) {
      return Constants.ComponentType.FILE;
    } else if (ResourceUtils.isDirectory(r)) {
      return Constants.ComponentType.DIRECTORY;
    } else if (ResourceUtils.isModuleProject(r)) {
      return Constants.ComponentType.MODULE;
    } else if (ResourceUtils.isRootProject(r)) {
      return Constants.ComponentType.PROJECT;
    } else if (ResourceUtils.isView(r)) {
      return Constants.ComponentType.VIEW;
    } else if (ResourceUtils.isSubview(r)) {
      return Constants.ComponentType.SUBVIEW;
    }
    throw new IllegalArgumentException("Unknown resource type: " + r);
  }

}
