/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.issue.ws;

import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.CheckForNull;
import org.sonar.server.issue.SearchRequest;

public enum SearchAdditionalField {

  ACTIONS("actions"),
  /**
   * @deprecated since 5.5, action plan feature has been removed
   */
  @Deprecated
  DEPRECATED_ACTION_PLANS("actionPlans"),
  COMMENTS("comments"),
  LANGUAGES("languages"),
  RULES("rules"),
  TRANSITIONS("transitions"),
  USERS("users");

  public static final String ALL_ALIAS = "_all";
  static final EnumSet<SearchAdditionalField> ALL_ADDITIONAL_FIELDS = EnumSet.allOf(SearchAdditionalField.class);
  private static final Map<String, SearchAdditionalField> BY_LABELS = new HashMap<>();
  static {
    for (SearchAdditionalField f : values()) {
      BY_LABELS.put(f.label, f);
    }
  }

  private final String label;

  SearchAdditionalField(String label) {
    this.label = label;
  }

  @CheckForNull
  public static SearchAdditionalField findByLabel(String label) {
    return BY_LABELS.get(label);
  }

  public static Collection<String> possibleValues() {
    List<String> possibles = Lists.newArrayList(ALL_ALIAS);
    possibles.addAll(BY_LABELS.keySet());
    return possibles;
  }

  public static EnumSet<SearchAdditionalField> getFromRequest(SearchRequest request) {
    List<String> labels = request.getAdditionalFields();
    if (labels == null) {
      return EnumSet.noneOf(SearchAdditionalField.class);
    }
    EnumSet<SearchAdditionalField> fields = EnumSet.noneOf(SearchAdditionalField.class);
    for (String label : labels) {
      if (label.equals(ALL_ALIAS)) {
        return EnumSet.allOf(SearchAdditionalField.class);
      }
      fields.add(findByLabel(label));
    }
    return fields;
  }
}
