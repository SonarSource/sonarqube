package org.sonarqube.qa.bluegreen;

import org.sonar.api.server.rule.RulesDefinition;

public class RulesDefinitionV2 implements RulesDefinition {

  @Override
  public void define(Context context) {
    NewRepository repo = context.createRepository("bluegreen", "xoo").setName("BlueGreen");
    repo.createRule("a").setName("Rule A").setHtmlDescription("Rule A");
    repo.createRule("b").setName("Rule B").setHtmlDescription("Rule B");
    repo.done();
  }
}
