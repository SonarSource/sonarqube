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

package org.sonar.core.source;

import com.google.common.collect.Lists;

import java.util.*;

public class DecorationDataHolder {

  private static final String ENTITY_SEPARATOR = ";";
  private static final String FIELD_SEPARATOR = ",";
  private static final String SYMBOL_PREFIX = "symbol-";
  private static final String HIGHLIGHTABLE = "highlightable";

  private List<TagEntry> openingTagsEntries;
  private int openingTagsIndex;
  private List<Integer> closingTagsOffsets;
  private int closingTagsIndex;

  public DecorationDataHolder() {
    openingTagsEntries = Lists.newArrayList();
    closingTagsOffsets = Lists.newArrayList();
  }

  public void loadSymbolReferences(String symbolsReferences) {
    String[] symbols = symbolsReferences.split(ENTITY_SEPARATOR);
    for (int i = 0; i < symbols.length; i++) {
      String[] symbolFields = symbols[i].split(FIELD_SEPARATOR);
      int declarationStartOffset = Integer.parseInt(symbolFields[0]);
      int declarationEndOffset = Integer.parseInt(symbolFields[1]);
      int symbolLength = declarationEndOffset - declarationStartOffset;
      String[] symbolOccurrences = Arrays.copyOfRange(symbolFields, 2, symbolFields.length);
      loadSymbolOccurrences(declarationStartOffset, symbolLength, symbolOccurrences);
    }
  }

  public void loadSyntaxHighlightingData(String syntaxHighlightingRules) {
    String[] rules = syntaxHighlightingRules.split(ENTITY_SEPARATOR);
    for (int i = 0; i < rules.length; i++) {
      String[] ruleFields = rules[i].split(FIELD_SEPARATOR);
      insertAndPreserveOrder(new TagEntry(Integer.parseInt(ruleFields[0]), ruleFields[2]), openingTagsEntries);
      insertAndPreserveOrder(Integer.parseInt(ruleFields[1]), closingTagsOffsets);
    }
  }

  public List<TagEntry> getOpeningTagsEntries() {
    return openingTagsEntries;
  }

  public TagEntry getCurrentOpeningTagEntry() {
    return openingTagsIndex < openingTagsEntries.size() ? openingTagsEntries.get(openingTagsIndex) : null;
  }

  public void nextOpeningTagEntry() {
    openingTagsIndex++;
  }

  public List<Integer> getClosingTagsOffsets() {
    return closingTagsOffsets;
  }

  public int getCurrentClosingTagOffset() {
    return closingTagsIndex < closingTagsOffsets.size() ? closingTagsOffsets.get(closingTagsIndex) : -1;
  }

  public void nextClosingTagOffset() {
    closingTagsIndex++;
  }

  private void loadSymbolOccurrences(int declarationStartOffset, int symbolLength, String[] symbolOccurrences) {
    for (int i = 0; i < symbolOccurrences.length; i++) {
      int occurrenceStartOffset = Integer.parseInt(symbolOccurrences[i]);
      int occurrenceEndOffset = occurrenceStartOffset + symbolLength;
      insertAndPreserveOrder(new TagEntry(occurrenceStartOffset, SYMBOL_PREFIX + declarationStartOffset + " " + HIGHLIGHTABLE), openingTagsEntries);
      insertAndPreserveOrder(occurrenceEndOffset, closingTagsOffsets);
    }
  }

  private void insertAndPreserveOrder(TagEntry newEntry, List<TagEntry> orderedEntries) {
    int insertionIndex = 0;
    Iterator<TagEntry> entriesIterator = orderedEntries.iterator();
    while(entriesIterator.hasNext() && entriesIterator.next().getStartOffset() <= newEntry.getStartOffset()) {
      insertionIndex++;
    }
    orderedEntries.add(insertionIndex, newEntry);
  }

  private void insertAndPreserveOrder(int newOffset, List<Integer> orderedOffsets) {
    int insertionIndex = 0;
    Iterator<Integer> entriesIterator = orderedOffsets.iterator();
    while(entriesIterator.hasNext() && entriesIterator.next() <= newOffset) {
      insertionIndex++;
    }
    orderedOffsets.add(insertionIndex, newOffset);
  }
}
