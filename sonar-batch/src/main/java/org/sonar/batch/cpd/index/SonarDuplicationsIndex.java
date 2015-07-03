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
package org.sonar.batch.cpd.index;

import java.util.Collection;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.duplications.block.Block;
import org.sonar.duplications.block.ByteArray;
import org.sonar.duplications.index.AbstractCloneIndex;
import org.sonar.duplications.index.CloneIndex;
import org.sonar.duplications.index.PackedMemoryCloneIndex;

public class SonarDuplicationsIndex extends AbstractCloneIndex {

  private final CloneIndex mem = new PackedMemoryCloneIndex();

  public void insert(InputFile inputFile, Collection<Block> blocks) {
    for (Block block : blocks) {
      mem.insert(block);
    }
  }

  public Collection<Block> getByInputFile(InputFile inputFile, String resourceKey) {
    return mem.getByResourceId(resourceKey);
  }

  @Override
  public Collection<Block> getBySequenceHash(ByteArray hash) {
    return mem.getBySequenceHash(hash);
  }

  @Override
  public Collection<Block> getByResourceId(String resourceId) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void insert(Block block) {
    throw new UnsupportedOperationException();
  }

}
