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

package org.sonar.core.source;

import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;

import java.util.Arrays;
import java.util.List;

public class DecorationDataHolder {

  private static final String ENTITY_SEPARATOR = ";";
  private static final String FIELD_SEPARATOR = ",";
  private static final String SYMBOL_PREFIX = "symbol-";

  private final Multimap<Integer, String> lowerBoundsDefinitions;
  private final List<Integer> upperBoundsDefinitions;

  public DecorationDataHolder() {
    lowerBoundsDefinitions = TreeMultimap.create();
    upperBoundsDefinitions = Lists.newArrayList();
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
      lowerBoundsDefinitions.put(Integer.parseInt(ruleFields[0]), ruleFields[2]);
      upperBoundsDefinitions.add(Integer.parseInt(ruleFields[1]));
    }
  }

  public Multimap<Integer, String> getLowerBoundsDefinitions() {
    return lowerBoundsDefinitions;
  }

  public List<Integer> getUpperBoundsDefinitions() {
    return upperBoundsDefinitions;
  }

  private void loadSymbolOccurrences(int declarationStartOffset, int symbolLength, String[] symbolOccurrences) {
    for (int i = 0; i < symbolOccurrences.length; i++) {
      int occurrenceStartOffset = Integer.parseInt(symbolOccurrences[i]);
      int occurrenceEndOffset = occurrenceStartOffset + symbolLength;
      lowerBoundsDefinitions.put(occurrenceStartOffset, SYMBOL_PREFIX + declarationStartOffset + " highlightable");
      upperBoundsDefinitions.add(occurrenceEndOffset);
    }
  }
}
