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
package org.sonar.api.test;

import org.mockito.ArgumentMatcher;
import org.sonar.api.resources.Resource;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.Violation;

public class IsViolation extends ArgumentMatcher<Violation> {

  private Rule rule;
  private String message;
  private Resource resource;
  private Integer lineId;

  public IsViolation(Violation wanted) {
    this.lineId = wanted.getLineId();
    this.message = wanted.getMessage();
    this.resource = wanted.getResource();
    this.rule = wanted.getRule();
  }

  public IsViolation(Rule rule, String message, Resource resource, Integer lineId) {
    this.rule = rule;
    this.message = message;
    this.resource = resource;
    this.lineId = lineId;
  }

  @Override
  public boolean matches(Object o) {
    Violation violation = (Violation) o;
    if (lineId != null && !lineId.equals(violation.getLineId())) {
      return false;
    }

    if (message != null && !message.equals(violation.getMessage())) {
      return false;
    }

    if (resource != null && !resource.equals(violation.getResource())) {
      return false;
    }

    if (rule != null && !rule.equals(violation.getRule())) {
      return false;
    }

    return true;
  }
}
