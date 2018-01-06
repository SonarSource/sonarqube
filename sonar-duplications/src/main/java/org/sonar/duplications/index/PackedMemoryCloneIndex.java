/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.duplications.index;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.sonar.duplications.block.Block;
import org.sonar.duplications.block.ByteArray;
import org.sonar.duplications.utils.FastStringComparator;

import javax.annotation.Nullable;

/**
 * Provides an index optimized by memory.
 * <p>
 * Each object in Java has an overhead - see
 * <a href="http://devblog.streamy.com/2009/07/24/determine-size-of-java-object-class/">"HOWTO: Determine the size of a Java Object or Class"</a>.
 * So to optimize memory consumption, we use flat arrays, however this increases time of queries.
 * During  usual detection of duplicates most time consuming method is a {@link #getByResourceId(String)}:
 * around 50% of time spent in this class and number of invocations of this method is 1% of total invocations,
 * however total time spent in this class less than 1 second for small projects and around 2 seconds for projects like JDK.
 * </p>
 * <p>
 * Note that this implementation currently does not support deletion, however it's possible to implement.
 * </p>
 */
public class PackedMemoryCloneIndex extends AbstractCloneIndex {

  private static final int DEFAULT_INITIAL_CAPACITY = 1024;

  private static final int BLOCK_INTS = 5;

  private final int hashInts;

  private final int blockInts;

  /**
   * Indicates that index requires sorting to perform queries.
   */
  private boolean sorted;

  /**
   * Current number of blocks in index.
   */
  private int size;

  private String[] resourceIds;
  private int[] blockData;

  private int[] resourceIdsIndex;

  private final Block.Builder blockBuilder = Block.builder();

  public PackedMemoryCloneIndex() {
    this(8, DEFAULT_INITIAL_CAPACITY);
  }

  /**
   * @param hashBytes size of hash in bytes
   * @param initialCapacity the initial capacity
   */
  public PackedMemoryCloneIndex(int hashBytes, int initialCapacity) {
    this.sorted = false;
    this.hashInts = hashBytes / 4;
    this.blockInts = hashInts + BLOCK_INTS;
    this.size = 0;
    this.resourceIds = new String[initialCapacity];
    this.blockData = new int[initialCapacity * blockInts];
    this.resourceIdsIndex = new int[initialCapacity];
  }

  /**
   * {@inheritDoc}
   * <p>
   * <strong>Note that this implementation does not guarantee that blocks would be sorted by index.</strong>
   * </p>
   */
  @Override
  public Collection<Block> getByResourceId(String resourceId) {
    ensureSorted();

    // prepare resourceId for binary search
    resourceIds[size] = resourceId;
    resourceIdsIndex[size] = size;

    int index = DataUtils.binarySearch(byResourceId);

    List<Block> result = new ArrayList<>();
    int realIndex = resourceIdsIndex[index];
    while (index < size && FastStringComparator.INSTANCE.compare(resourceIds[realIndex], resourceId) == 0) {
      result.add(getBlock(realIndex, resourceId));

      index++;
      realIndex = resourceIdsIndex[index];
    }
    return result;
  }

  private Block createBlock(int index, String resourceId, @Nullable ByteArray byteHash) {
    int offset = index * blockInts;
    ByteArray blockHash;

    if (byteHash == null) {
      int[] hash = new int[hashInts];
      for (int j = 0; j < hashInts; j++) {
        hash[j] = blockData[offset++];
      }
      blockHash = new ByteArray(hash);
    } else {
      blockHash = byteHash;
      offset += hashInts;
    }

    int indexInFile = blockData[offset++];
    int firstLineNumber = blockData[offset++];
    int lastLineNumber = blockData[offset++];
    int startUnit = blockData[offset++];
    int endUnit = blockData[offset];

    return blockBuilder
      .setResourceId(resourceId)
      .setBlockHash(blockHash)
      .setIndexInFile(indexInFile)
      .setLines(firstLineNumber, lastLineNumber)
      .setUnit(startUnit, endUnit)
      .build();
  }

  private Block getBlock(int index, String resourceId) {
    return createBlock(index, resourceId, null);
  }

  private class ResourceIterator implements Iterator<ResourceBlocks> {
    private int index = 0;

    @Override
    public boolean hasNext() {
      return index < size;
    }

    @Override
    public ResourceBlocks next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }

      String resourceId = resourceIds[resourceIdsIndex[index]];
      List<Block> blocks = new ArrayList<>();

      // while we are at the same resource, keep going
      do {
        blocks.add(getBlock(resourceIdsIndex[index], resourceId));
        index++;
      } while (hasNext() && FastStringComparator.INSTANCE.compare(resourceIds[resourceIdsIndex[index]], resourceId) == 0);

      return new ResourceBlocks(resourceId, blocks);
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }
  }

  public static class ResourceBlocks {
    private Collection<Block> blocks;
    private String resourceId;

    public ResourceBlocks(String resourceId, Collection<Block> blocks) {
      this.resourceId = resourceId;
      this.blocks = blocks;
    }

    public Collection<Block> blocks() {
      return blocks;
    }

    public String resourceId() {
      return resourceId;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Iterator<ResourceBlocks> iterator() {
    ensureSorted();
    return new ResourceIterator();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Collection<Block> getBySequenceHash(ByteArray sequenceHash) {
    ensureSorted();

    // prepare hash for binary search
    int[] hash = sequenceHash.toIntArray();
    if (hash.length != hashInts) {
      throw new IllegalArgumentException("Expected " + hashInts + " ints in hash, but got " + hash.length);
    }
    int offset = size * blockInts;
    for (int i = 0; i < hashInts; i++) {
      blockData[offset++] = hash[i];
    }

    int index = DataUtils.binarySearch(byBlockHash);

    List<Block> result = new ArrayList<>();
    while (index < size && !isLessByHash(size, index)) {
      // extract block (note that there is no need to extract hash)
      String resourceId = resourceIds[index];
      result.add(createBlock(index, resourceId, sequenceHash));
      index++;
    }
    return result;
  }

  /**
   * {@inheritDoc}
   * <p>
   * <strong>Note that this implementation allows insertion of two blocks with same index for one resource.</strong>
   * </p>
   */
  @Override
  public void insert(Block block) {
    sorted = false;
    ensureCapacity();

    resourceIds[size] = block.getResourceId();

    int[] hash = block.getBlockHash().toIntArray();
    if (hash.length != hashInts) {
      throw new IllegalArgumentException("Expected " + hashInts + " ints in hash, but got " + hash.length);
    }
    int offset = size * blockInts;
    for (int i = 0; i < hashInts; i++) {
      blockData[offset++] = hash[i];
    }
    blockData[offset++] = block.getIndexInFile();
    blockData[offset++] = block.getStartLine();
    blockData[offset++] = block.getEndLine();
    blockData[offset++] = block.getStartUnit();
    blockData[offset] = block.getEndUnit();

    size++;
  }

  /**
   * Increases the capacity, if necessary.
   */
  private void ensureCapacity() {
    if (size < resourceIds.length) {
      return;
    }
    int newCapacity = (resourceIds.length * 3) / 2 + 1;
    // Increase size of resourceIds
    String[] oldResourceIds = resourceIds;
    resourceIds = new String[newCapacity];
    System.arraycopy(oldResourceIds, 0, resourceIds, 0, oldResourceIds.length);
    // Increase size of blockData
    int[] oldBlockData = blockData;
    blockData = new int[newCapacity * blockInts];
    System.arraycopy(oldBlockData, 0, blockData, 0, oldBlockData.length);
    // Increase size of byResourceIndices (no need to copy old, because would be restored in method ensureSorted)
    resourceIdsIndex = new int[newCapacity];
    sorted = false;
  }

  /**
   * Performs sorting, if necessary.
   */
  private void ensureSorted() {
    if (sorted) {
      return;
    }

    ensureCapacity();

    DataUtils.sort(byBlockHash);
    for (int i = 0; i < size; i++) {
      resourceIdsIndex[i] = i;
    }
    DataUtils.sort(byResourceId);

    sorted = true;
  }

  private boolean isLessByHash(int i, int j) {
    int i2 = i * blockInts;
    int j2 = j * blockInts;
    for (int k = 0; k < hashInts; k++, i2++, j2++) {
      if (blockData[i2] < blockData[j2]) {
        return true;
      }
      if (blockData[i2] > blockData[j2]) {
        return false;
      }
    }
    return false;
  }

  private final DataUtils.Sortable byBlockHash = new DataUtils.Sortable() {
    @Override
    public void swap(int i, int j) {
      String tmp = resourceIds[i];
      resourceIds[i] = resourceIds[j];
      resourceIds[j] = tmp;

      i *= blockInts;
      j *= blockInts;
      for (int k = 0; k < blockInts; k++, i++, j++) {
        int x = blockData[i];
        blockData[i] = blockData[j];
        blockData[j] = x;
      }
    }

    @Override
    public boolean isLess(int i, int j) {
      return isLessByHash(i, j);
    }

    @Override
    public int size() {
      return size;
    }
  };

  private final DataUtils.Sortable byResourceId = new DataUtils.Sortable() {
    @Override
    public void swap(int i, int j) {
      int tmp = resourceIdsIndex[i];
      resourceIdsIndex[i] = resourceIdsIndex[j];
      resourceIdsIndex[j] = tmp;
    }

    @Override
    public boolean isLess(int i, int j) {
      String s1 = resourceIds[resourceIdsIndex[i]];
      String s2 = resourceIds[resourceIdsIndex[j]];
      return FastStringComparator.INSTANCE.compare(s1, s2) < 0;
    }

    @Override
    public int size() {
      return size;
    }
  };

  @Override
  /**
   * Computation is O(N)
   */
  public int noResources() {
    ensureSorted();
    int count = 0;
    String lastResource = null;

    for (int i = 0; i < size; i++) {
      String resource = resourceIds[resourceIdsIndex[i]];
      if (resource != null && !resource.equals(lastResource)) {
        count++;
        lastResource = resource;
      }
    }
    return count;
  }
}
