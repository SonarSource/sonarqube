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
package org.sonar.batch.duplication;

import com.persistit.Value;
import com.persistit.encoding.CoderContext;
import com.persistit.encoding.ValueCoder;
import org.sonar.api.batch.sensor.duplication.Duplication;

class DuplicationBlockValueCoder implements ValueCoder {

  @Override
  public void put(Value value, Object object, CoderContext context) {
    Duplication.Block b = (Duplication.Block) object;
    value.putUTF(b.resourceKey());
    value.put(b.startLine());
    value.put(b.length());
  }

  @Override
  public Object get(Value value, Class clazz, CoderContext context) {
    String resourceKey = value.getString();
    int startLine = value.getInt();
    int length = value.getInt();
    return new Duplication.Block(resourceKey, startLine, length);
  }
}
