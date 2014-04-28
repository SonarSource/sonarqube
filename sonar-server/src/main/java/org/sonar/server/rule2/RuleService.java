package org.sonar.server.rule2;

import org.sonar.server.search.Hit;

import java.util.Collection;
import java.util.Collections;

public class RuleService {

  private RuleDao dao;
  private RuleIndex index;

  public RuleService(RuleDao dao, RuleIndex index){
    this.dao = dao;
    this.index = index;
  }

  public Collection<Hit> search(RuleQuery query){
    return Collections.emptyList();
  }

  public static Rule toRule(RuleDto ruleDto){
    return new RuleImpl();
  }

  public static Rule toRule(Hit hit){
    return new RuleImpl();
  }
}
