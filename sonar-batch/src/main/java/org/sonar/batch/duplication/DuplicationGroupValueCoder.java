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
import org.sonar.batch.duplication.DuplicationGroup.Block;

import java.util.ArrayList;

class DuplicationGroupValueCoder implements ValueCoder {

  private DuplicationBlockValueCoder blockCoder = new DuplicationBlockValueCoder();

  @Override
  public void put(Value value, Object object, CoderContext context) {
    DuplicationGroup c = (DuplicationGroup) object;
    blockCoder.put(value, c.originBlock(), context);
    value.put(c.duplicates().size());
    for (DuplicationGroup.Block block : c.duplicates()) {
      blockCoder.put(value, block, context);
    }
  }

  @Override
  public Object get(Value value, Class clazz, CoderContext context) {
    DuplicationGroup g = new DuplicationGroup((Block) blockCoder.get(value, DuplicationGroup.Block.class, context));
    int count = value.getInt();
    ArrayList<DuplicationGroup.Block> blocks = new ArrayList<DuplicationGroup.Block>(count);
    for (int i = 0; i < count; i++) {
      blocks.add((Block) blockCoder.get(value, DuplicationGroup.Block.class, context));
    }
    g.setDuplicates(blocks);
    return g;
  }
}
