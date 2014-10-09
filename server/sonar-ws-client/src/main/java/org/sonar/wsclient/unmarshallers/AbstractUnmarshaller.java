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

import org.sonar.wsclient.services.Model;
import org.sonar.wsclient.services.WSUtils;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractUnmarshaller<M extends Model> implements Unmarshaller<M> {

  @Override
  public final M toModel(String json) {
    WSUtils utils = WSUtils.getINSTANCE();
    M result = null;
    Object array = utils.parse(json);
    if (array instanceof List) {
      if (utils.getArraySize(array) >= 1) {
        Object elt = utils.getArrayElement(array, 0);
        if (elt != null) {
          result = parse(elt);
        }
      }
    } else {
      result = parse(array);
    }
    return result;

  }

  @Override
  public final List<M> toModels(String json) {
    WSUtils utils = WSUtils.getINSTANCE();
    List<M> result = new ArrayList<M>();
    Object array = utils.parse(json);
    for (int i = 0; i < utils.getArraySize(array); i++) {
      Object elt = utils.getArrayElement(array, i);
      if (elt != null) {
        result.add(parse(elt));
      }
    }
    return result;
  }

  protected abstract M parse(Object elt);
}
