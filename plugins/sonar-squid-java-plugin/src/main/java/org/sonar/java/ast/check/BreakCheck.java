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

import java.util.Arrays;
import java.util.List;

import org.sonar.check.IsoCategory;
import org.sonar.check.Rule;
import org.sonar.squid.api.CheckMessage;
import org.sonar.squid.api.SourceFile;

import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;

@Rule(key = "BreakCheck", name = "BreakCheck", isoCategory = IsoCategory.Maintainability)
public class BreakCheck extends JavaAstCheck {

  private boolean insideSwitch = false;

  @Override
  public List<Integer> getWantedTokens() {
    return wantedTokens;
  }

  @Override
  public void visitToken(DetailAST ast) {
    if (ast.getType() == TokenTypes.LITERAL_SWITCH) {
      insideSwitch = true;
    }
    if ((ast.getType() == TokenTypes.LITERAL_BREAK) && !insideSwitch) {
      CheckMessage message = new CheckMessage(this, "Avoid usage of break outside switch statement");
      message.setLine(ast.getLineNo());
      SourceFile sourceFile = peekSourceCode().getParent(SourceFile.class);
      sourceFile.log(message);
    }
  }

  @Override
  public void leaveToken(DetailAST ast) {
    if (ast.getType() == TokenTypes.LITERAL_SWITCH) {
      insideSwitch = false;
    }
  }

  private static final List<Integer> wantedTokens = Arrays.asList(TokenTypes.LITERAL_BREAK, TokenTypes.LITERAL_SWITCH);

}
