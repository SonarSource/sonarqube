/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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

  @Override
  public void define(Context context) {
    defineRulesXoo(context);
  }

  private static void defineRulesXoo(Context context) {
    NewRepository repo = context.createRepository(FOO_REPOSITORY, Foo.KEY).setName("Foo");
    createRule(repo, "UnchangedRule");
    createRule(repo, "ChangedRule");
    createRule(repo, "ToBeDeactivatedRule");
    createRule(repo, "ToBeRemovedRule");
    createRule(repo, "RuleWithUnchangedParameter").createParam("unchanged").setDefaultValue("10");
    createRule(repo, "RuleWithChangedParameter").createParam("toBeChanged").setDefaultValue("10");
    createRule(repo, "RuleWithRemovedParameter").createParam("toBeRemoved").setDefaultValue("10");
    createRule(repo, "RuleWithAddedParameter").createParam("added");
    repo.done();
  }

  private static NewRule createRule(NewRepository repo, String key) {
    return repo.createRule(key).setName(key).setHtmlDescription(key);
  }

}
