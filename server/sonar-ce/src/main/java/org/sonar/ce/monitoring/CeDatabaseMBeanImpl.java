/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.ce.monitoring;

import org.sonar.db.DatabaseMBean;
import org.sonar.db.DbClient;
import org.sonar.process.systeminfo.protobuf.ProtobufSystemInfo;

public class CeDatabaseMBeanImpl extends DatabaseMBean implements CeDatabaseMBean {

  private static final String OBJECT_NAME = "ComputeEngineDatabaseConnection";

  public CeDatabaseMBeanImpl(DbClient dbClient) {
    super(dbClient);
  }

  @Override
  protected String name() {
    return OBJECT_NAME;
  }

  @Override
  public ProtobufSystemInfo.Section toProtobuf() {
    ProtobufSystemInfo.Section.Builder builder = ProtobufSystemInfo.Section.newBuilder();
    builder.setName("Compute Engine Database Connection");
    builder.addAttributesBuilder().setKey("Pool Total Connections").setLongValue(getPoolTotalConnections()).build();
    builder.addAttributesBuilder().setKey("Pool Active Connections").setLongValue(getPoolActiveConnections()).build();
    builder.addAttributesBuilder().setKey("Pool Idle Connections").setLongValue(getPoolIdleConnections()).build();
    builder.addAttributesBuilder().setKey("Pool Max Connections").setLongValue(getPoolMaxConnections()).build();
    builder.addAttributesBuilder().setKey("Pool Min Idle Connections").setLongValue(getPoolMinIdleConnections()).build();
    builder.addAttributesBuilder().setKey("Pool Max Wait (ms)").setLongValue(getPoolMaxWaitMillis()).build();
    builder.addAttributesBuilder().setKey("Pool Max Lifetime (ms)").setLongValue(getPoolMaxLifeTimeMillis()).build();
    return builder.build();
  }
}
