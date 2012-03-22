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

package org.sonar.plugins.squid;

import java.util.Arrays;
import java.util.List;

import org.sonar.api.resources.Java;
import org.sonar.api.rules.AnnotationRuleParser;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleRepository;
import org.sonar.java.ast.check.BreakCheck;
import org.sonar.java.ast.check.CommentedOutCodeLineCheck;
import org.sonar.java.ast.check.ContinueCheck;
import org.sonar.java.ast.check.UndocumentedApiCheck;
import org.sonar.java.bytecode.check.ArchitectureCheck;
import org.sonar.java.bytecode.check.CallToDeprecatedMethodCheck;
import org.sonar.java.bytecode.check.LCOM4Check;
import org.sonar.java.bytecode.check.UnusedPrivateMethodCheck;
import org.sonar.java.bytecode.check.UnusedProtectedMethodCheck;
import org.sonar.java.squid.check.*;

public final class SquidRuleRepository extends RuleRepository {
  private AnnotationRuleParser ruleParser;

  public SquidRuleRepository(AnnotationRuleParser ruleParser) {
    super(SquidConstants.REPOSITORY_KEY, Java.KEY);
    setName(SquidConstants.REPOSITORY_NAME);
    this.ruleParser = ruleParser;
  }

  @Override
  public List<Rule> createRules() {
    return ruleParser.parse(SquidConstants.REPOSITORY_KEY, getCheckClasses());
  }

  public static List<Class> getCheckClasses() {
    return Arrays.asList(
        // Bytecode checks
        (Class) CallToDeprecatedMethodCheck.class, UnusedPrivateMethodCheck.class, UnusedProtectedMethodCheck.class,
        ArchitectureCheck.class, LCOM4Check.class, 
        // AST checks
        UndocumentedApiCheck.class, ContinueCheck.class, BreakCheck.class,
        // Squid checks
        DITCheck.class, ClassComplexityCheck.class, MethodComplexityCheck.class, NoSonarCheck.class, EmptyFileCheck.class,
        CommentedOutCodeLineCheck.class);
  }
}
