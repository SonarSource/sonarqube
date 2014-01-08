package org.sonar.core.rule;

public class RuleTagDto {

  private Long id;
  private Integer ruleId;
  private String tag;
  private String type;

  public Long getId() {
    return id;
  }

  public RuleTagDto setId(Long id) {
    this.id = id;
    return this;
  }

  public Integer getRuleId() {
    return ruleId;
  }

  public RuleTagDto setRuleId(Integer ruleId) {
    this.ruleId = ruleId;
    return this;
  }

  public String getTag() {
    return tag;
  }

  public RuleTagDto setTag(String tag) {
    this.tag = tag;
    return this;
  }

  public String getType() {
    return type;
  }

  public RuleTagDto setType(String type) {
    this.type = type;
    return this;
  }
}
