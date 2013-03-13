package org.sonar.server.macro;

public class RuleMacro implements Macro{

  private final String contextPath;

  public RuleMacro(String contextPath){
    this.contextPath = contextPath;
  }

  /**
   * First parameter is the repository, second one is the rule key
   */
  public String getRegex() {
    return "\\{rule:([a-zA-Z0-9._]++):([a-zA-Z0-9._]++)\\}";
  }

  public String getReplacement(){
    return "<a class='open-modal rule-modal' href='" + contextPath + "/rules/show/$1:$2?modal=true&layout=false'>$1:$2</a>";
  }
}
