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

package org.sonar.java.ast.check;

import java.util.List;

import org.sonar.check.IsoCategory;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.ast.visitor.ClassVisitor;
import org.sonar.squid.api.CheckMessage;
import org.sonar.squid.api.SourceCode;
import org.sonar.squid.api.SourceFile;
import org.sonar.squid.measures.Metric;

import com.puppycrawl.tools.checkstyle.api.DetailAST;

@Rule(key = "ClassComplexityCheck", isoCategory = IsoCategory.Maintainability)
public class ClassComplexityCheck extends JavaAstCheck {

  @RuleProperty
  private Integer threshold;

  @Override
  public List<Integer> getWantedTokens() {
    return ClassVisitor.wantedTokens;
  }

  @Override
  public void leaveToken(DetailAST ast) {
    SourceCode currentResource = peekSourceCode();
    int complexity = calculateComplexity(currentResource);
    if (complexity > threshold) {
      CheckMessage message = new CheckMessage(this, "Class complexity exceeds " + threshold + ".");
      message.setLine(ast.getLineNo());
      message.setCost(complexity - threshold);
      SourceFile sourceFile = currentResource.getParent(SourceFile.class);
      sourceFile.log(message);
    }
  }

  private int calculateComplexity(SourceCode sourceCode) {
    int result = 0;
    if (sourceCode.getChildren() != null) {
      for (SourceCode child : sourceCode.getChildren()) {
        result += calculateComplexity(child);
      }
    }
    result += sourceCode.getInt(Metric.COMPLEXITY);
    return result;
  }

  public void setThreshold(int threshold) {
    this.threshold = threshold;
  }

}
