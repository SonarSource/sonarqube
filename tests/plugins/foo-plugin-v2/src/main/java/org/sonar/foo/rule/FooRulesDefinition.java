/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.foo.rule;

import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.foo.Foo;

public class FooRulesDefinition implements RulesDefinition {

  public static final String FOO_REPOSITORY = "foo";
  public static final String FOO_REPOSITORY_2 = "foo2";

  @Override
  public void define(Context context) {
    defineRulesXoo(context);
  }

  private static void defineRulesXoo(Context context) {
    NewRepository repoFoo1 = context.createRepository(FOO_REPOSITORY, Foo.KEY).setName("Foo");
    createRule(repoFoo1, "UnchangedRule");
    createRule(repoFoo1, "ChangedRule");
    createRule(repoFoo1, "NewRule");
    createRule(repoFoo1, "ToBeDeactivatedRule");
    createRule(repoFoo1, "RuleWithUnchangedParameter").createParam("unchanged").setDefaultValue("10");
    // Update of the default value of a parameter is not taken into account in quality profile as long as it's not also explicitly set in the quality profile
    createRule(repoFoo1, "RuleWithChangedParameter").createParam("toBeChanged").setDefaultValue("20");
    createRule(repoFoo1, "RuleWithRemovedParameter");
    createRule(repoFoo1, "RuleWithAddedParameter").createParam("added").setDefaultValue("10");
    createRule(repoFoo1, "Renamed").addDeprecatedRuleKey(FOO_REPOSITORY, "ToBeRenamed");
    repoFoo1.done();

    NewRepository repoFoo2 = context.createRepository(FOO_REPOSITORY_2, Foo.KEY).setName("Foo2");
    createRule(repoFoo2, "RenamedAndMoved").addDeprecatedRuleKey(FOO_REPOSITORY, "ToBeRenamedAndMoved");
    repoFoo2.done();
  }

  private static NewRule createRule(NewRepository repo, String key) {
    return repo.createRule(key).setName(key).setHtmlDescription(key);
  }

}
