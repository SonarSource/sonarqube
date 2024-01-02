/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.db.audit.model;

import org.sonar.db.plugin.PluginDto;

public class PluginNewValue extends NewValue {

  /**
   * @deprecated The uuids in the audit logs are not product requirement anymore and will be removed in 11.x
   */
  @Deprecated(since = "10.2")
  private final String pluginUuid;
  private final String kee;
  private final String basePluginKey;
  private final String type;

  public PluginNewValue(PluginDto pluginDto) {
    this.pluginUuid = pluginDto.getUuid();
    this.kee = pluginDto.getKee();
    this.basePluginKey = pluginDto.getBasePluginKey();
    this.type = pluginDto.getType().name();
  }

  /**
   * @deprecated The uuids in the audit logs are not product requirement anymore and will be removed in 11.x
   */
  @Deprecated(since = "10.2")
  public String getPluginUuid() {
    return this.pluginUuid;
  }

  public String getKee() {
    return this.kee;
  }

  public String getBasePluginKey() {
    return this.basePluginKey;
  }

  public String getType() {
    return this.type;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("{");
    addField(sb, "\"pluginUuid\": ", this.pluginUuid, true);
    addField(sb, "\"kee\": ", this.kee, true);
    addField(sb, "\"basePluginKey\": ", this.basePluginKey, true);
    addField(sb, "\"type\": ", this.type, true);
    endString(sb);
    return sb.toString();
  }
}
