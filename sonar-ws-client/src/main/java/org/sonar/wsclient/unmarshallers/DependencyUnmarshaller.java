/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.wsclient.unmarshallers;

import org.sonar.wsclient.services.Dependency;
import org.sonar.wsclient.services.WSUtils;

public class DependencyUnmarshaller extends AbstractUnmarshaller<Dependency> {

  @Override
  protected Dependency parse(Object json) {
    WSUtils utils = WSUtils.getINSTANCE();
    return new Dependency()
        .setId(utils.getString(json, "id"))
        .setFromId(utils.getLong(json, "fi"))
        .setToId(utils.getLong(json, "ti"))
        .setFromKey(utils.getString(json, "fk"))
        .setToKey(utils.getString(json, "tk"))
        .setUsage(utils.getString(json, "u"))
        .setWeight(utils.getInteger(json, "w"))
        .setFromName(utils.getString(json, "fn"))
        .setFromQualifier(utils.getString(json, "fq"))
        .setToName(utils.getString(json, "tn"))
        .setToQualifier(utils.getString(json, "tq"));
  }
}
