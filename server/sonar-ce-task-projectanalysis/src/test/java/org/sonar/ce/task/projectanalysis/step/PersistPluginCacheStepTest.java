/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.step;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.ce.task.projectanalysis.batch.BatchReportReaderRule;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.db.DbClient;
import org.sonar.db.DbInputStream;
import org.sonar.db.DbTester;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PersistPluginCacheStepTest {
  @Rule
  public BatchReportReaderRule reader = new BatchReportReaderRule();
  @Rule
  public DbTester dbTester = DbTester.create();
  private final DbClient client = dbTester.getDbClient();
  private final TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();
  private final PersistPluginCacheStep step = new PersistPluginCacheStep(reader, dbTester.getDbClient(), treeRootHolder);

  @Test
  public void inserts_cache() throws IOException {
    reader.setPluginCache("test".getBytes(UTF_8));

    Component root = mock(Component.class);
    when(root.getUuid()).thenReturn("branch");
    treeRootHolder.setRoot(root);

    step.execute(mock(ComputationStep.Context.class));
    assertThat(dbTester.countRowsOfTable("scanner_cache")).isOne();
    try (DbInputStream data = client.scannerCacheDao().selectData(dbTester.getSession(), "branch")) {
      assertThat(IOUtils.toString(data, UTF_8)).isEqualTo("test");
    }
  }

  @Test
  public void updates_cache() throws IOException {
    client.scannerCacheDao().insert(dbTester.getSession(), "branch", new ByteArrayInputStream("test".getBytes(UTF_8)));
    inserts_cache();
  }
}
