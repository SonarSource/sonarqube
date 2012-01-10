/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.duplications.index;

import java.io.Serializable;
import java.util.Collection;
import java.util.Comparator;

import org.sonar.duplications.block.Block;
import org.sonar.duplications.block.ByteArray;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.TreeMultimap;

public class MemoryCloneIndex implements CloneIndex {

  private final TreeMultimap<String, Block> filenameIndex;
  private final HashMultimap<ByteArray, Block> sequenceHashIndex;

  private static final class ValueComparator implements Comparator<Block>, Serializable {

    private static final long serialVersionUID = 6048010242032502222L;

    public int compare(Block o1, Block o2) {
      if (o2.getResourceId().equals(o1.getResourceId())) {
        return o1.getIndexInFile() - o2.getIndexInFile();
      }
      return o1.getResourceId().compareTo(o2.getResourceId());
    }
  }

  private static final class KeyComparator implements Comparator<String>, Serializable {

    private static final long serialVersionUID = 8705841881237170539L;

    public int compare(String o1, String o2) {
      return o1.compareTo(o2);
    }
  }

  public MemoryCloneIndex() {
    filenameIndex = TreeMultimap.create(new KeyComparator(), new ValueComparator());
    sequenceHashIndex = HashMultimap.create();
  }

  public Collection<String> getAllUniqueResourceId() {
    return filenameIndex.keySet();
  }

  public boolean containsResourceId(String resourceId) {
    return filenameIndex.containsKey(resourceId);
  }

  public Collection<Block> getByResourceId(String fileName) {
    return filenameIndex.get(fileName);
  }

  public Collection<Block> getBySequenceHash(ByteArray sequenceHash) {
    return sequenceHashIndex.get(sequenceHash);
  }

  public void insert(Block tuple) {
    filenameIndex.put(tuple.getResourceId(), tuple);
    sequenceHashIndex.put(tuple.getBlockHash(), tuple);
  }

}
