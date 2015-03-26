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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.sensor.duplication.Duplication;
import org.sonar.api.batch.sensor.duplication.internal.DefaultDuplication;
import org.sonar.api.resources.Project;
import org.sonar.batch.duplication.DuplicationCache;
import org.sonar.batch.index.ResourceCache;
import org.sonar.batch.protocol.output.BatchReportReader;
import org.sonar.batch.protocol.output.BatchReportWriter;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DuplicationsPublisherTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private DuplicationCache duplicationCache;
  private DuplicationsPublisher publisher;

  @Before
  public void prepare() {
    ResourceCache resourceCache = new ResourceCache();
    Project p = new Project("foo");
    resourceCache.add(p, null);
    org.sonar.api.resources.Resource sampleFile = org.sonar.api.resources.File.create("src/Foo.php").setEffectiveKey("foo:src/Foo.php");
    resourceCache.add(sampleFile, null);
    org.sonar.api.resources.Resource sampleFile2 = org.sonar.api.resources.File.create("src/Foo2.php").setEffectiveKey("foo:src/Foo2.php");
    resourceCache.add(sampleFile2, null);
    duplicationCache = mock(DuplicationCache.class);
    when(duplicationCache.byComponent(anyString())).thenReturn(Collections.<DefaultDuplication>emptyList());
    publisher = new DuplicationsPublisher(resourceCache, duplicationCache);
  }

  @Test
  public void publishDuplications() throws Exception {

    DefaultDuplication dup1 = new DefaultDuplication()
      .setOriginBlock(new Duplication.Block("foo:src/Foo.php", 1, 10))
      .isDuplicatedBy("foo:src/Foo.php", 20, 50);
    DefaultDuplication dup2 = new DefaultDuplication()
      .setOriginBlock(new Duplication.Block("foo:src/Foo.php", 11, 10))
      .isDuplicatedBy("another", 20, 50);
    DefaultDuplication dup3 = new DefaultDuplication()
      .setOriginBlock(new Duplication.Block("foo:src/Foo.php", 11, 10))
      .isDuplicatedBy("foo:src/Foo2.php", 20, 50);
    when(duplicationCache.byComponent("foo:src/Foo.php")).thenReturn(Arrays.asList(dup1, dup2, dup3));

    File outputDir = temp.newFolder();
    BatchReportWriter writer = new BatchReportWriter(outputDir);

    publisher.publish(writer);

    BatchReportReader reader = new BatchReportReader(outputDir);

    assertThat(reader.readComponentDuplications(1)).hasSize(0);
    List<org.sonar.batch.protocol.output.BatchReport.Duplication> componentDuplications = reader.readComponentDuplications(2);
    assertThat(componentDuplications).hasSize(3);
    org.sonar.batch.protocol.output.BatchReport.Duplication savedDup1 = componentDuplications.get(0);
    assertThat(savedDup1.getOriginPosition().getStartLine()).isEqualTo(1);
    assertThat(savedDup1.getOriginPosition().getEndLine()).isEqualTo(10);
    assertThat(savedDup1.getDuplicate(0).hasOtherFileKey()).isFalse();
    assertThat(savedDup1.getDuplicate(0).hasOtherFileRef()).isFalse();
    assertThat(savedDup1.getDuplicate(0).getRange().getStartLine()).isEqualTo(20);
    assertThat(savedDup1.getDuplicate(0).getRange().getEndLine()).isEqualTo(50);

    org.sonar.batch.protocol.output.BatchReport.Duplication savedDup2 = componentDuplications.get(1);
    assertThat(savedDup2.getOriginPosition().getStartLine()).isEqualTo(11);
    assertThat(savedDup2.getOriginPosition().getEndLine()).isEqualTo(20);
    assertThat(savedDup2.getDuplicate(0).getOtherFileKey()).isEqualTo("another");
    assertThat(savedDup2.getDuplicate(0).hasOtherFileRef()).isFalse();
    assertThat(savedDup2.getDuplicate(0).getRange().getStartLine()).isEqualTo(20);
    assertThat(savedDup2.getDuplicate(0).getRange().getEndLine()).isEqualTo(50);

    org.sonar.batch.protocol.output.BatchReport.Duplication savedDup3 = componentDuplications.get(2);
    assertThat(savedDup3.getOriginPosition().getStartLine()).isEqualTo(11);
    assertThat(savedDup3.getOriginPosition().getEndLine()).isEqualTo(20);
    assertThat(savedDup3.getDuplicate(0).hasOtherFileKey()).isFalse();
    assertThat(savedDup3.getDuplicate(0).getOtherFileRef()).isEqualTo(3);
    assertThat(savedDup3.getDuplicate(0).getRange().getStartLine()).isEqualTo(20);
    assertThat(savedDup3.getDuplicate(0).getRange().getEndLine()).isEqualTo(50);

  }

}
