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
package org.sonar.wsclient.unmarshallers;

import org.sonar.wsclient.services.ManualMeasure;
import org.sonar.wsclient.services.WSUtils;

/**
 * @since 2.10
 */
public class ManualMeasureUnmarshaller extends AbstractUnmarshaller<ManualMeasure> {

  @Override
  protected ManualMeasure parse(Object json) {
    WSUtils utils = WSUtils.getINSTANCE();
    return new ManualMeasure()
        .setId(utils.getLong(json, "id"))
        .setMetricKey(utils.getString(json, "metric"))
        .setResourceKey(utils.getString(json, "resource"))
        .setCreatedAt(utils.getDateTime(json, "created_at"))
        .setUpdatedAt(utils.getDateTime(json, "updated_at"))
        .setUserLogin(utils.getString(json, "login"))
        .setUsername(utils.getString(json, "username"))
        .setValue(utils.getDouble(json, "val"))
        .setTextValue(utils.getString(json, "text"));
  }
}
