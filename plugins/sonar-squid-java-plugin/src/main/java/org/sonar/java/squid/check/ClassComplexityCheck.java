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
import org.sonar.squid.api.SourceClass;
import org.sonar.squid.measures.Metric;

@Rule(key = "ClassCyclomaticComplexity", name = "Avoid too complex class", isoCategory = IsoCategory.Maintainability,
    priority = Priority.MAJOR, description = "<p>The Cyclomatic Complexity is measured by the number of (&&, ||) operators "
        + "and (if, while, do, for, ?:, catch, switch, case, return, throw) statements in the body of a class plus one for "
        + "each constructor, method (but not getter/setter), static initializer, or instance initializer in the class. "
        + "The last return stament in method, if exists, is not taken into account.</p>"
        + "<p>Even when the Cyclomatic Complexity of a class is very high, this complexity might be well distributed among all methods. "
        + "Nevertheless, most of the time, a very complex class is a class which breaks the "
        + "<a href='http://en.wikipedia.org/wiki/Single_responsibility_principle'>Single Responsibility Principle</a> "
        + "and which should be re-factored to be split in several classes.</p>")
public class ClassComplexityCheck extends SquidCheck {

  @RuleProperty(description = "Maximum complexity allowed.", defaultValue = "200")
  private Integer max;

  @Override
  public void visitClass(SourceClass sourceClass) {
    int complexity = sourceClass.getInt(Metric.COMPLEXITY);
    if (complexity > max) {
      CheckMessage message = new CheckMessage(this, "The Cyclomatic Complexity of this class is " + complexity + " which is greater than "
          + max + " authorized.");
      message.setLine(sourceClass.getStartAtLine());
      message.setCost(complexity - max);
      getSourceFile(sourceClass).log(message);
    }
  }

  public void setMax(int max) {
    this.max = max;
  }

}
