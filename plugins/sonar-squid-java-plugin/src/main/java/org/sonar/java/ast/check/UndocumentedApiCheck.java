/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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

import org.apache.commons.lang.StringUtils;
import org.sonar.api.utils.WildcardPattern;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.PatternUtils;
import org.sonar.java.ast.visitor.PublicApiVisitor;
import org.sonar.squid.api.CheckMessage;
import org.sonar.squid.api.SourceClass;
import org.sonar.squid.api.SourceCode;
import org.sonar.squid.api.SourceFile;
import org.sonar.squid.api.SourceMethod;

import com.puppycrawl.tools.checkstyle.api.DetailAST;

@Rule(key = "UndocumentedApi", priority = Priority.MAJOR)
public class UndocumentedApiCheck extends JavaAstCheck {

  @RuleProperty
  private String forClasses = "";

  private WildcardPattern[] patterns;

  @Override
  public List<Integer> getWantedTokens() {
    return PublicApiVisitor.TOKENS;
  }

  @Override
  public void visitToken(DetailAST ast) {
    SourceCode currentResource = peekSourceCode();
    SourceClass sourceClass = peekParentClass();
    if (WildcardPattern.match(getPatterns(), sourceClass.getKey())) {
      if (currentResource instanceof SourceMethod && ((SourceMethod) currentResource).isAccessor()) {
        return;
      }

      if (PublicApiVisitor.isPublicApi(ast) && !PublicApiVisitor.isDocumentedApi(ast, getFileContents())) {
        SourceFile sourceFile = currentResource.getParent(SourceFile.class);
        CheckMessage message = new CheckMessage(this, "Avoid undocumented API.");
        message.setLine(ast.getLineNo());
        sourceFile.log(message);
      }
    }
  }

  private WildcardPattern[] getPatterns() {
    if (patterns == null) {
      patterns = PatternUtils.createPatterns(StringUtils.defaultIfEmpty(forClasses, "**"));
    }
    return patterns;
  }

  public String getForClasses() {
    return forClasses;
  }

  public void setForClasses(String forClasses) {
    this.forClasses = forClasses;
  }

}
