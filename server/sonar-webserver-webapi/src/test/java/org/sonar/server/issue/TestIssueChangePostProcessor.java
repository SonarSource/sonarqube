/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.issue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;

public class TestIssueChangePostProcessor implements IssueChangePostProcessor {

  private boolean called = false;
  private final List<ComponentDto> calledComponents = new ArrayList<>();

  @Override
  public void process(DbSession dbSession, List<DefaultIssue> changedIssues, Collection<ComponentDto> components, boolean fromAlm) {
    called = true;
    calledComponents.addAll(components);
  }

  public boolean wasCalled() {
    return called;
  }

  public List<ComponentDto> calledComponents() {
    return calledComponents;
  }
}
