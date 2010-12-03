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

import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.squid.api.CheckMessage;
import org.sonar.squid.api.SourceClass;
import org.sonar.squid.api.SourceFile;
import org.sonar.squid.measures.Metric;

@Rule(key = "MaximumInheritanceDepth", name = "Avoid too deep inheritance tree",
    priority = Priority.MAJOR, description = "<p>Inheritance is certainly one of the most valuable concept of object-oriented "
        + "programming. It's a way to compartmentalize and reuse code by creating collections of attributes and behaviors called "
        + "classes which can be based on previously created classes. But abusing of this concept by creating a deep inheritance tree "
        + "can lead to very complex and unmaintainable source code.</p>"
        + "<p>Most of the time a too deep inheritance tree is due to bad object oriented design which has led to systematically use "
        + "'inheritance' when 'composition' would suit better.</p>")
public class DITCheck extends SquidCheck {

  @RuleProperty(description = "Maximum depth of the inheritance tree.", defaultValue = "5")
  private Integer max;

  @Override
  public void visitClass(SourceClass sourceClass) {
    int dit = sourceClass.getInt(Metric.DIT);
    if (dit > max) {
      CheckMessage message = new CheckMessage(this, "This class has " + dit + " parents which is greater than " + max + " authorized.");
      message.setLine(sourceClass.getStartAtLine());
      message.setCost(dit - max);
      sourceClass.getParent(SourceFile.class).log(message);
    }
  }

  public void setMax(int max) {
    this.max = max;
  }
}
