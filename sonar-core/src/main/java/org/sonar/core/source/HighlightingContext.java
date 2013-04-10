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

import java.util.List;

public class HighlightingContext {

  private static final String RULE_SEPARATOR = ";";
  private static final String FIELD_SEPARATOR = ",";

  private final Multimap<Integer, String> lowerBoundsDefinitions;
  private final List<Integer> upperBoundsDefinitions;

  private HighlightingContext() {
    lowerBoundsDefinitions = TreeMultimap.create();
    upperBoundsDefinitions = Lists.newArrayList();
  }

  public static HighlightingContext buildFrom(String serializedRules) {

    HighlightingContext context = new HighlightingContext();

    String[] rules = serializedRules.split(RULE_SEPARATOR);
    for (int i = 0; i < rules.length; i++) {
      String[] ruleFields = rules[i].split(FIELD_SEPARATOR);
      context.lowerBoundsDefinitions.put(Integer.parseInt(ruleFields[0]), ruleFields[2]);
      context.upperBoundsDefinitions.add(Integer.parseInt(ruleFields[1]));
    }
    return context;
  }

  public Multimap<Integer, String> getLowerBoundsDefinitions() {
    return lowerBoundsDefinitions;
  }

  public List<Integer> getUpperBoundsDefinitions() {
    return upperBoundsDefinitions;
  }
}
