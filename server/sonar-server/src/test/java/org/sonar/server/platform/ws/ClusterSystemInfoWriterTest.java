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
package org.sonar.server.platform.ws;

import java.io.StringWriter;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.process.systeminfo.protobuf.ProtobufSystemInfo.Attribute;
import org.sonar.process.systeminfo.protobuf.ProtobufSystemInfo.Section;
import org.sonar.server.health.ClusterHealth;
import org.sonar.server.health.Health;
import org.sonar.server.health.HealthChecker;
import org.sonar.server.platform.monitoring.cluster.AppNodesInfoLoader;
import org.sonar.server.platform.monitoring.cluster.GlobalInfoLoader;
import org.sonar.server.platform.monitoring.cluster.NodeInfo;
import org.sonar.server.platform.monitoring.cluster.SearchNodesInfoLoader;
import org.sonar.server.telemetry.TelemetryDataLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ClusterSystemInfoWriterTest {
  private GlobalInfoLoader globalInfoLoader = mock(GlobalInfoLoader.class);
  private AppNodesInfoLoader appNodesInfoLoader = mock(AppNodesInfoLoader.class);
  private SearchNodesInfoLoader searchNodesInfoLoader = mock(SearchNodesInfoLoader.class);
  private HealthChecker healthChecker = mock(HealthChecker.class);
  private TelemetryDataLoader telemetry = mock(TelemetryDataLoader.class, Mockito.RETURNS_MOCKS);
  private ClusterSystemInfoWriter underTest = new ClusterSystemInfoWriter(globalInfoLoader, appNodesInfoLoader, searchNodesInfoLoader, healthChecker, telemetry);

  @Before
  public void before() throws InterruptedException {
    when(globalInfoLoader.load()).thenReturn(Collections.singletonList(createSection("globalInfo")));
    when(appNodesInfoLoader.load()).thenReturn(Collections.singletonList(createNodeInfo("appNodes")));
    when(searchNodesInfoLoader.load()).thenReturn(Collections.singletonList(createNodeInfo("searchNodes")));
    Health health = Health.newHealthCheckBuilder().setStatus(Health.Status.GREEN).build();
    when(healthChecker.checkCluster()).thenReturn(new ClusterHealth(health, Collections.emptySet()));
  }

  @Test
  public void writeInfo() throws InterruptedException {
    StringWriter writer = new StringWriter();
    JsonWriter jsonWriter = JsonWriter.of(writer);
    jsonWriter.beginObject();
    underTest.write(jsonWriter);
    jsonWriter.endObject();

    assertThat(writer.toString()).isEqualTo("{\"Health\":\"GREEN\","
      + "\"Health Causes\":[],\"\":{\"name\":\"globalInfo\"},"
      + "\"Application Nodes\":[{\"Name\":\"appNodes\",\"\":{\"name\":\"appNodes\"}}],"
      + "\"Search Nodes\":[{\"Name\":\"searchNodes\",\"\":{\"name\":\"searchNodes\"}}],"
      + "\"Statistics\":{\"id\":\"\",\"version\":\"\",\"database\":{\"name\":\"\",\"version\":\"\"},\"plugins\":[],"
      + "\"userCount\":0,\"projectCount\":0,\"usingBranches\":false,\"ncloc\":0,\"projectCountByLanguage\":[],\"nclocByLanguage\":[]}}");
  }

  private static NodeInfo createNodeInfo(String name) {
    NodeInfo info = new NodeInfo(name);
    info.addSection(createSection(name));
    return info;
  }

  private static Section createSection(String name) {
    return Section.newBuilder()
      .addAttributes(Attribute.newBuilder().setKey("name").setStringValue(name).build())
      .build();
  }
}
