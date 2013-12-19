/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.server.qualityprofile;

import com.google.common.collect.Lists;

import java.util.List;

/**
 * Track changes made to active rules through profile inheritance that impact ES index
 * @since 4.2
 *
 */
public class RuleInheritanceActions {

  private List<Integer> idsToIndex;
  private List<Integer> idsToDelete;

  public RuleInheritanceActions() {
    idsToIndex = Lists.newArrayList();
    idsToDelete = Lists.newArrayList();
  }

  public void add(RuleInheritanceActions actions) {
    idsToIndex.addAll(actions.idsToIndex);
    idsToDelete.addAll(actions.idsToDelete);
  }

  public void addToIndex(Integer activeRuleId) {
    idsToIndex.add(activeRuleId);
  }

  public void addToDelete(Integer activeRuleId) {
    idsToDelete.add(activeRuleId);
  }

  public List<Integer> idsToIndex() {
    return idsToIndex;
  }

  public List<Integer> idsToDelete() {
    return idsToDelete;
  }
}
