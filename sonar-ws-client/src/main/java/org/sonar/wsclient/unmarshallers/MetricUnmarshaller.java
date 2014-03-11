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

import org.sonar.wsclient.services.Metric;
import org.sonar.wsclient.services.WSUtils;

public class MetricUnmarshaller extends AbstractUnmarshaller<Metric> {

  @Override
  protected Metric parse(Object json) {
    WSUtils utils = WSUtils.getINSTANCE();
    return new Metric()
        .setKey(utils.getString(json, "key"))
        .setName(utils.getString(json, "name"))
        .setDomain(utils.getString(json, "domain"))
        .setDescription(utils.getString(json, "description"))
        .setDirection(utils.getInteger(json, "direction"))
        .setType(utils.getString(json, "val_type"))
        .setUserManaged(utils.getBoolean(json, "user_managed"))
        .setHidden(utils.getBoolean(json, "hidden"));
  }
}
