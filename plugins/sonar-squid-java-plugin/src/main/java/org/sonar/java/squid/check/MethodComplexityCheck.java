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

package org.sonar.java.squid.check;

import org.sonar.check.IsoCategory;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.squid.api.CheckMessage;
import org.sonar.squid.api.SourceFile;
import org.sonar.squid.api.SourceMethod;
import org.sonar.squid.measures.Metric;

@Rule(key = "MethodCyclomaticComplexity", name = "Avoid too complex method", isoCategory = IsoCategory.Maintainability,
    priority = Priority.MAJOR, description = "<p>The Cyclomatic Complexity is measured by the number of (&&, ||) operators "
        + "and (if, while, do, for, ?:, catch, switch, case, return, throw) statements in the body of a constructor, "
        + "method, static initializer, or instance initializer. "
        + "The minimun Cyclomatic Complexity of a method is 1 and the last return stament, if exists, is not taken into account. "
        + "The more complex is a method, the more possible different paths through the source code exist. "
        + "Generally 1-4 is considered good, 5-7 ok, 8-10 consider re-factoring, and 11+ re-factor now. "
        + "Indeed above 10, it's pretty difficult to be able to think about all possible paths when maintaining the source code, "
        + "so the risk of regression increases exponentially.</p>")
public class MethodComplexityCheck extends SquidCheck {

  @RuleProperty(description = "Maximum complexity allowed.", defaultValue = "10")
  private Integer max;

  @Override
  public void visitMethod(SourceMethod sourceMethod) {
    int complexity = sourceMethod.getInt(Metric.COMPLEXITY);
    if (complexity > max) {
      CheckMessage message = new CheckMessage(this, "The Cyclomatic Complexity of this method is " + complexity + " which is greater than "
          + max + " authorized.");
      message.setLine(sourceMethod.getStartAtLine());
      message.setCost(complexity - max);
      sourceMethod.getParent(SourceFile.class).log(message);
    }
  }

  public void setMax(int max) {
    this.max = max;
  }

}
