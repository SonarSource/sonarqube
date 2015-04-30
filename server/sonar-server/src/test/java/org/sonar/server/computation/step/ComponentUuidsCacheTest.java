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

package org.sonar.server.computation.step;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.batch.protocol.Constants;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.batch.protocol.output.BatchReportReader;
import org.sonar.batch.protocol.output.BatchReportWriter;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

public class ComponentUuidsCacheTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  File reportDir;

  @Before
  public void setUp() throws Exception {
    reportDir = temp.newFolder();
  }

  @Test
  public void get_uuid_from_ref() throws Exception {
    BatchReportWriter writer = new BatchReportWriter(reportDir);
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(1)
      .setType(Constants.ComponentType.PROJECT)
      .setUuid("ABCD")
      .build());

    ComponentUuidsCache cache = new ComponentUuidsCache(new BatchReportReader(reportDir));
    assertThat(cache.getUuidFromRef(1)).isEqualTo("ABCD");
  }
}
