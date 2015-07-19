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

import java.io.File;
import java.util.Date;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.resources.Project;
import org.sonar.batch.index.BatchComponentCache;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.batch.protocol.output.BatchReportReader;
import org.sonar.batch.protocol.output.BatchReportWriter;
import org.sonar.batch.scan.ImmutableProjectReactor;

import static org.assertj.core.api.Assertions.assertThat;

public class MetadataPublisherTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  ProjectDefinition projectDef;
  Project project;

  MetadataPublisher underTest;

  @Before
  public void prepare() {
    projectDef = ProjectDefinition.create().setKey("foo");
    project = new Project("foo").setAnalysisDate(new Date(1234567L));
    BatchComponentCache componentCache = new BatchComponentCache();
    org.sonar.api.resources.Resource sampleFile = org.sonar.api.resources.File.create("src/Foo.php").setEffectiveKey("foo:src/Foo.php");
    componentCache.add(project, null);
    componentCache.add(sampleFile, project);
    underTest = new MetadataPublisher(componentCache, new ImmutableProjectReactor(projectDef));
  }

  @Test
  public void write_metadata() throws Exception {
    File outputDir = temp.newFolder();
    BatchReportWriter writer = new BatchReportWriter(outputDir);

    underTest.publish(writer);

    BatchReportReader reader = new BatchReportReader(outputDir);
    BatchReport.Metadata metadata = reader.readMetadata();
    assertThat(metadata.getAnalysisDate()).isEqualTo(1234567L);
    assertThat(metadata.getProjectKey()).isEqualTo("foo");
  }

  @Test
  public void write_project_branch() throws Exception {
    projectDef.properties().put(CoreProperties.PROJECT_BRANCH_PROPERTY, "myBranch");
    project.setKey("foo:myBranch");
    project.setEffectiveKey("foo:myBranch");

    File outputDir = temp.newFolder();
    BatchReportWriter writer = new BatchReportWriter(outputDir);

    underTest.publish(writer);

    BatchReportReader reader = new BatchReportReader(outputDir);
    BatchReport.Metadata metadata = reader.readMetadata();
    assertThat(metadata.getAnalysisDate()).isEqualTo(1234567L);
    assertThat(metadata.getProjectKey()).isEqualTo("foo");
    assertThat(metadata.getBranch()).isEqualTo("myBranch");
  }

}
