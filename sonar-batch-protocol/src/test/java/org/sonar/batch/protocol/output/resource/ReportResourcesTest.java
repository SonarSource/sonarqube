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
package org.sonar.batch.protocol.output.resource;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.sonar.batch.protocol.output.resource.ReportResource.Type;

import java.text.SimpleDateFormat;
import java.util.Date;

import static org.fest.assertions.Assertions.assertThat;

public class ReportResourcesTest {

  @Test
  public void to_json() throws Exception {
    ReportResources res = new ReportResources();
    Date d = new SimpleDateFormat("dd/MM/yyyy").parse("12/12/2012");
    res.setAnalysisDate(d);
    ReportResource root = new ReportResource()
      .setBatchId(1)
      .setId(11)
      .setName("Root project")
      .setSnapshotId(111)
      .setType(Type.PRJ);
    ReportResource module = new ReportResource()
      .setBatchId(2)
      .setId(22)
      .setName("Module")
      .setSnapshotId(222)
      .setPath("module1")
      .setType(Type.MOD);
    root.addChild(module);
    ReportResource dir = new ReportResource()
      .setBatchId(3)
      .setId(33)
      .setName("src")
      .setSnapshotId(333)
      .setPath("src")
      .setType(Type.DIR);
    module.addChild(dir);
    ReportResource file = new ReportResource()
      .setBatchId(4)
      .setId(44)
      .setName("Foo.java")
      .setSnapshotId(444)
      .setPath("Foo.java")
      .setType(Type.FIL);
    dir.addChild(file);
    res.setRoot(root);

    JSONAssert
      .assertEquals(
        IOUtils.toString(this.getClass().getResourceAsStream("ReportResourceTest/expected.json"), "UTF-8"),
        res.toJson(), true);
  }

  @Test
  public void from_json() throws Exception {
    ReportResources res = ReportResources
      .fromJson(
      IOUtils.toString(this.getClass().getResourceAsStream("ReportResourceTest/expected.json"), "UTF-8"));

    assertThat(res.analysisDate()).isEqualTo(new SimpleDateFormat("dd/MM/yyyy").parse("12/12/2012"));
    ReportResource root = res.root();
    assertThat(root.batchId()).isEqualTo(1);
    assertThat(root.id()).isEqualTo(11);
    assertThat(root.name()).isEqualTo("Root project");
    assertThat(root.snapshotId()).isEqualTo(111);
    assertThat(root.path()).isNull();
    assertThat(root.type()).isEqualTo(Type.PRJ);
    assertThat(root.children()).hasSize(1);

  }
}
