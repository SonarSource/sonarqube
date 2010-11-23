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
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.squid.api.CheckMessage;
import org.sonar.squid.api.SourceClass;
import org.sonar.squid.measures.Metric;

@Rule(key = "ClassComplexityCheck", name = "ClassComplexityCheck", isoCategory = IsoCategory.Maintainability)
public class ClassComplexityCheck extends SquidCheck {

  @RuleProperty(description = "Threshold.")
  private Integer threshold;

  @Override
  public void visitClass(SourceClass sourceClass) {
    int complexity = sourceClass.getInt(Metric.COMPLEXITY);
    if (complexity > threshold) {
      CheckMessage message = new CheckMessage(this, "Class complexity exceeds " + threshold + ".");
      message.setLine(sourceClass.getStartAtLine());
      message.setCost(complexity - threshold);
      getSourceFile(sourceClass).log(message);
    }
  }

  public void setThreshold(int threshold) {
    this.threshold = threshold;
  }

}
