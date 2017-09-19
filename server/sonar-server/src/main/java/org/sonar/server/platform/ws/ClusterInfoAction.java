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
package org.sonar.server.platform.ws;

import java.util.Arrays;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.server.user.UserSession;

public class ClusterInfoAction implements SystemWsAction {

  private final UserSession userSession;

  public ClusterInfoAction(UserSession userSession) {
    this.userSession = userSession;
  }

  @Override
  public void define(WebService.NewController controller) {
    controller.createAction("cluster_info")
      .setDescription("WIP")
      .setSince("6.6")
      .setInternal(true)
      .setResponseExample(getClass().getResource("/org/sonar/server/platform/ws/info-example.json"))
      .setHandler(this);
  }

  @Override
  public void handle(Request request, Response response) {
    userSession.checkIsSystemAdministrator();

    try (JsonWriter json = response.newJsonWriter()) {
      json.beginObject();

      json.name("System");
      json.beginObject();
      json.prop("High Availability", true);
      json.prop("Cluster Name", "fooo");
      json.prop("Server Id", "ABC123");
      json.prop("Health", "RED");
      json
        .name("Health Causes")
        .beginArray().values(Arrays.asList("foo", "bar")).endArray();
      json.endObject();

      json.name("Settings");
      json.beginObject();
      json.prop("sonar.forceAuthentication", true);
      json.prop("sonar.externalIdentityProviders", "GitHub, BitBucket");
      json.endObject();

      json.name("Database");
      json
        .beginObject()
        .prop("Name", "PostgreSQL")
        .prop("Version", "9.6.3")
        .endObject();

      json.name("Compute Engine");
      json
        .beginObject()
        .prop("Pending", 5)
        .prop("In Progress", 4)
        .prop("workers", 8)
        .prop("workersPerNode", 4)
        .endObject();

      json.name("Search");
      json
        .beginObject()
        .prop("Health", "GREEN")
        .prop("Number of Nodes", 4)
        .prop("Index Components - Docs", 152_515_155)
        .prop("Index Components - Shards", 20)
        .prop("Index Components - Size", "25GB")
        .prop("Index Issues - Docs", 5)
        .prop("Index Issues - Shards", 5)
        .prop("Index Issues - Size", "52MB")
        .prop("Index Tests - Docs", 56605)
        .prop("Index Tests - Shards", 2)
        .prop("Index Tests - Size", "520MB")
        .endObject();

      json.name("Application Nodes");
      json
        .beginArray()
        .beginObject()
        .prop("Name", "Mont Blanc")
        .prop("Host", "10.158.92.16")
        .prop("Health", "YELLOW")
        .name("healthCauses").beginArray().beginObject().prop("message", "Db connectivity error").endObject().endArray()
        .prop("Start Time", "2017-05-30T10:23:45")
        .prop("Official Distribution", true)
        .prop("Processors", 4);
      json
        .name("Web JVM").beginObject()
        .prop("JVM Name", "Java HotSpot(TM) 64-Bit Server VM")
        .prop("JVM Vendor", "Oracle Corporation")
        .prop("Max Memory", "948MB")
        .prop("Free Memory", "38MB")
        .endObject()

        .name("Web JVM Properties").beginObject()
        .prop("catalina.home", "/sonarsource/var/tmp/sonarsource/sssonarqube/tc")
        .prop("glowroot.tmp.dir", "/var/tmp/sonarsource/ssglowroot-agent")
        .prop("glowroot.adad.dir", "/var/tmp/sonarsource/ssglowroot-agent")
        .prop("java.specification.version", "1.8")
        .endObject()

        .name("Web Database Connectivity").beginObject()
        .prop("Driver", "PostgreSQL JDBC Driver")
        .prop("Driver Version", "PostgreSQL JDBC Driver")
        .prop("Pool Idle Connections", 2)
        .prop("Pool Max Connections", 50)
        .prop("URL", "jdbc:postgresql://next-rds.cn6pfc2xc6oq.us-east-1.rds.amazonaws.com/dory")
        .endObject();

      json
        .name("Compute Engine JVM").beginObject()
        .prop("JVM Name", "Java HotSpot(TM) 64-Bit Server VM")
        .prop("JVM Vendor", "Oracle Corporation")
        .prop("Max Memory", "25MB")
        .prop("Free Memory", "8MB")
        .endObject();

      json
        .name("Compute Engine JVM Properties").beginObject()
        .prop("java.ext.dirs", "/opt/sonarsource/jvm/java-1.8.0-sun-x64/jre/lib/ext:/usr/java/packages/lib/ext")
        .prop("java.io.tmpdir", "/opt/sonarsource/jvm/java-1.8.0-sun-x64/jre/lib/ext:/usr/java/packages/lib/ext")
        .prop("java.library.path", "/opt/sonarsource/jvm/java-1.8.0-sun-x64/jre/lib/ext:/usr/java/packages/lib/ext")
        .prop("java.net.preferIPv4Stack", true)
        .prop("java.rmi.server.randomIDs", true)
        .prop("java.specification.version", "1.8")
        .endObject();

      json.endObject().endArray();

      json.name("Search Nodes");
      json
        .beginArray()
        .beginObject()
        .prop("Name", "Parmelan")
        .prop("Host", "10.158.92.19")
        .prop("Health", "GREEN")
        .name("Health Causes").beginArray().endArray()
        .prop("Start Time", "2017-05-30T10:23:45")
        .prop("Processors", 2)
        .prop("Disk Available", "25GB")
        .prop("JVM Threads", 52)

        .name("JVM Properties").beginObject()
        .prop("java.ext.dirs", "/opt/sonarsource/jvm/java-1.8.0-sun-x64/jre/lib/ext:/usr/java/packages/lib/ext")
        .prop("java.io.tmpdir", "/opt/sonarsource/jvm/java-1.8.0-sun-x64/jre/lib/ext:/usr/java/packages/lib/ext")
        .prop("java.library.path", "/opt/sonarsource/jvm/java-1.8.0-sun-x64/jre/lib/ext:/usr/java/packages/lib/ext")
        .prop("java.net.preferIPv4Stack", true)
        .prop("java.rmi.server.randomIDs", true)
        .endObject()

        .name("JVM").beginObject()
        .prop("java.ext.dirs", "/opt/sonarsource/jvm/java-1.8.0-sun-x64/jre/lib/ext:/usr/java/packages/lib/ext")
        .prop("java.io.tmpdir", "/opt/sonarsource/jvm/java-1.8.0-sun-x64/jre/lib/ext:/usr/java/packages/lib/ext")
        .prop("java.library.path", "/opt/sonarsource/jvm/java-1.8.0-sun-x64/jre/lib/ext:/usr/java/packages/lib/ext")
        .prop("java.net.preferIPv4Stack", true)
        .prop("java.rmi.server.randomIDs", true)
        .endObject()

        .endObject()
        .endArray();

      json.endObject();
    }
  }
}
