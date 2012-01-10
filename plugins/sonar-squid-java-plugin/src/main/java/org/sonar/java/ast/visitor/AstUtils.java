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
package org.sonar.java.ast.visitor;

import org.sonar.squid.api.AnalysisException;
import org.sonar.squid.api.SourceCode;

import antlr.collections.AST;

import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.Scope;
import com.puppycrawl.tools.checkstyle.api.ScopeUtils;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;

public final class AstUtils {

  private AstUtils() {
  }

  public static AST findType(DetailAST ast) {
    DetailAST typeAst = ast.findFirstToken(TokenTypes.TYPE);
    if (typeAst != null) {
      return typeAst.getFirstChild();
    }
    return null;
  }

  public static boolean isClassVariable(DetailAST ast) {
    return ast.getType() == TokenTypes.VARIABLE_DEF && ast.getParent().getType() == TokenTypes.OBJBLOCK
        && isClass(ast.getParent().getParent());
  }

  public static boolean isClass(DetailAST ast) {
    return ast.getType() == TokenTypes.CLASS_DEF || ast.getType() == TokenTypes.ENUM_DEF || ast.getType() == TokenTypes.ANNOTATION_DEF
        || ast.getType() == TokenTypes.INTERFACE_DEF;
  }

  public static boolean isInterfaceVariable(DetailAST ast) {
    return ast.getType() == TokenTypes.VARIABLE_DEF && ast.getParent().getType() == TokenTypes.OBJBLOCK
        && isInterface(ast.getParent().getParent());
  }

  public static boolean isInterface(DetailAST ast) {
    return ast.getType() == TokenTypes.INTERFACE_DEF;
  }

  public static boolean isFinal(DetailAST detailAst) {
    return isModifier(detailAst, TokenTypes.FINAL);
  }

  public static boolean isStatic(DetailAST detailAst) {
    return isModifier(detailAst, TokenTypes.LITERAL_STATIC);
  }

  public static boolean isModifier(DetailAST detailAst, int modifierType) {
    DetailAST modifiers = detailAst.findFirstToken(TokenTypes.MODIFIERS);
    if (modifiers != null) {
      boolean isModifierMatching = modifiers.branchContains(modifierType);
      if (!isModifierMatching && isInterfaceVariable(detailAst)) {
        // by default if not specified, a var def in an interface is
        // public final static
        return (modifierType == TokenTypes.LITERAL_STATIC || modifierType == TokenTypes.FINAL);
      }
      return isModifierMatching;
    }
    return false;
  }

  public static Scope getScope(DetailAST ast) {
    DetailAST modifierAst = ast.findFirstToken(TokenTypes.MODIFIERS);
    Scope found = modifierAst != null ? ScopeUtils.getScopeFromMods(modifierAst) : Scope.NOTHING;
    if (found.compareTo(Scope.PACKAGE) == 0 && (ast.getType() == TokenTypes.METHOD_DEF || ast.getType() == TokenTypes.VARIABLE_DEF)) {
      // check if we found a parent interface declaration
      // interface methods or var defs are by default public when not
      // specified
      found = (isScope(Scope.PACKAGE, found) && findParent(ast, TokenTypes.INTERFACE_DEF) != null) ? Scope.PUBLIC : found;
    }
    return found;
  }

  public static boolean isScope(Scope toCompare, Scope scope) {
    return scope.compareTo(toCompare) == 0;
  }

  public static boolean isType(DetailAST ast, int type) {
    return ast.getType() == type;
  }

  public static DetailAST findParent(DetailAST ast, int tokenType) {
    DetailAST parent = ast.getParent();
    if (parent != null) {
      return parent.getType() == tokenType ? parent : findParent(parent, tokenType);
    }
    return null;
  }

  public static void ensureResourceType(SourceCode resource, Class<? extends SourceCode> resourceType) {
    if (!resource.isType(resourceType)) {
      throw new AnalysisException("Resource " + resource.getKey() + " must be of type " + resourceType.getName());
    }
  }
}
