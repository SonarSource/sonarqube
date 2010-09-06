/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
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
package org.sonar.api.web.gwt.client.webservices;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Violations extends ResponsePOJO {
  private List<Violation> violations;
  private Map<Integer, List<Violation>> byLines;

  public Violations(List<Violation> violations) {
    this.violations = violations;
  }

  public Violations() {
    this.violations = new ArrayList<Violation>();
  }

  public void add(Violation v) {
    violations.add(v);
    byLines = null;
  }

  public List<Violation> getAll() {
    return violations;
  }


  public Map<Integer, List<Violation>> getByLines() {
    if (byLines == null) {
      byLines = new HashMap<Integer, List<Violation>>();
      for (Violation violation : violations) {
        List<Violation> lineViolations = byLines.get(violation.getLine());
        if (lineViolations == null) {
          lineViolations = new ArrayList<Violation>();
          byLines.put(violation.getLine(), lineViolations);
        }
        lineViolations.add(violation);
      }
    }
    return byLines;
  }

  public String getLevelForLine(Integer line) {
    List<Violation> lineViolations = getByLines().get(line);
    String level = "";
    if (lineViolations != null) {
      for (Violation lineViolation : lineViolations) {
        if ("BLOCKER".equals(lineViolation.getPriority()) || "CRITICAL".equals(lineViolation.getPriority()) || "MAJOR".equals(lineViolation.getPriority())) {
          level = "error";

        } else if (!"error".equals(level)) {
          level = "warning";
        }
      }
    }
    return level;
  }

  public int countForLine(Integer line) {
    List<Violation> lineViolations = getByLines().get(line);
    if (lineViolations == null) {
      return 0;
    }
    return lineViolations.size();
  }
}
