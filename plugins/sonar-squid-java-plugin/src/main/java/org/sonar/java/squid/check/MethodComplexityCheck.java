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

package org.sonar.java.squid.check;

import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.squid.api.CheckMessage;
import org.sonar.squid.api.SourceFile;
import org.sonar.squid.api.SourceMethod;
import org.sonar.squid.measures.Metric;

@Rule(key = "MethodCyclomaticComplexity", priority = Priority.MAJOR)
public class MethodComplexityCheck extends SquidCheck {

  public static final int DEFAULT_MAX = 10;

  @RuleProperty(defaultValue = "" + DEFAULT_MAX)
  private Integer max = DEFAULT_MAX;

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
