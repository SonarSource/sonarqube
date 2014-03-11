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

import org.sonar.wsclient.services.DependencyTree;
import org.sonar.wsclient.services.WSUtils;

import java.util.ArrayList;
import java.util.List;

public class DependencyTreeUnmarshaller extends AbstractUnmarshaller<DependencyTree> {
  @Override
  protected DependencyTree parse(Object json) {
    WSUtils utils = WSUtils.getINSTANCE();
    DependencyTree tree = new DependencyTree()
        .setDepId(utils.getString(json, "did"))
        .setResourceId(utils.getString(json, "rid"))
        .setResourceKey(utils.getString(json, "k"))
        .setResourceName(utils.getString(json, "n"))
        .setResourceScope(utils.getString(json, "s"))
        .setResourceQualifier(utils.getString(json, "q"))
        .setResourceVersion(utils.getString(json, "v"))
        .setUsage(utils.getString(json, "u"))
        .setWeight(utils.getInteger(json, "w"));

    List<DependencyTree> to = new ArrayList<DependencyTree>();
    tree.setTo(to);

    Object toJson = utils.getField(json, "to");
    if (toJson != null) {
      for (int i = 0; i < utils.getArraySize(toJson); i++) {
        to.add(parse(utils.getArrayElement(toJson, i)));
      }
    }
    return tree;
  }
}
