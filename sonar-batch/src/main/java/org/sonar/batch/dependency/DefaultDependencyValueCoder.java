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
package org.sonar.batch.dependency;

import com.persistit.Value;
import com.persistit.encoding.CoderContext;
import com.persistit.encoding.ValueCoder;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.sensor.dependency.internal.DefaultDependency;
import org.sonar.batch.scan.filesystem.InputPathCache;

class DefaultDependencyValueCoder implements ValueCoder {

  private InputPathCache inputPathCache;

  public DefaultDependencyValueCoder(InputPathCache inputPathCache) {
    this.inputPathCache = inputPathCache;
  }

  @Override
  public void put(Value value, Object object, CoderContext context) {
    DefaultDependency dep = (DefaultDependency) object;
    value.putUTF(((DefaultInputFile) dep.from()).moduleKey());
    value.putUTF(((DefaultInputFile) dep.from()).relativePath());
    value.putUTF(((DefaultInputFile) dep.to()).moduleKey());
    value.putUTF(((DefaultInputFile) dep.to()).relativePath());
    value.put(dep.weight());
  }

  @Override
  public Object get(Value value, Class clazz, CoderContext context) {
    String fromModuleKey = value.getString();
    String fromRelativePath = value.getString();
    InputFile from = inputPathCache.getFile(fromModuleKey, fromRelativePath);
    if (from == null) {
      throw new IllegalStateException("Unable to load InputFile " + fromModuleKey + ":" + fromRelativePath);
    }
    String toModuleKey = value.getString();
    String toRelativePath = value.getString();
    InputFile to = inputPathCache.getFile(toModuleKey, toRelativePath);
    if (to == null) {
      throw new IllegalStateException("Unable to load InputFile " + toModuleKey + ":" + toRelativePath);
    }
    int weight = value.getInt();
    return new DefaultDependency()
      .from(from)
      .to(to)
      .weight(weight);
  }
}
