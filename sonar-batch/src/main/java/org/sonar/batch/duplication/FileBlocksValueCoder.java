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
import org.sonar.duplications.block.Block;
import org.sonar.duplications.block.ByteArray;
import org.sonar.duplications.block.FileBlocks;

import java.util.ArrayList;
import java.util.List;

class FileBlocksValueCoder implements ValueCoder {

  @Override
  public void put(Value value, Object object, CoderContext context) {
    FileBlocks blocks = (FileBlocks) object;
    value.putUTF(blocks.resourceId());
    value.put(blocks.blocks().size());
    for (Block b : blocks.blocks()) {
      value.putByteArray(b.getBlockHash().getBytes());
      value.put(b.getIndexInFile());
      value.put(b.getStartLine());
      value.put(b.getEndLine());
      value.put(b.getStartUnit());
      value.put(b.getEndUnit());
    }
  }

  @Override
  public Object get(Value value, Class clazz, CoderContext context) {
    String resourceId = value.getString();
    int count = value.getInt();
    List<Block> blocks = new ArrayList<Block>(count);
    for (int i = 0; i < count; i++) {
      Block.Builder b = Block.builder();
      b.setResourceId(resourceId);
      b.setBlockHash(new ByteArray(value.getByteArray()));
      b.setIndexInFile(value.getInt());
      int startLine = value.getInt();
      int endLine = value.getInt();
      b.setLines(startLine, endLine);
      int startUnit = value.getInt();
      int endUnit = value.getInt();
      b.setUnit(startUnit, endUnit);
      blocks.add(b.build());
    }
    return new FileBlocks(resourceId, blocks);
  }
}
