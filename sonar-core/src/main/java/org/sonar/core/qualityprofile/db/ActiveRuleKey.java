package org.sonar.core.qualityprofile.db;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import org.sonar.api.rule.RuleKey;

import java.io.Serializable;

/**
 * Created by gamars on 05/05/14.
 *
 * @since 4.4
 */
public class ActiveRuleKey implements Serializable{
  private final QProfileKey qProfileKey;
  private final RuleKey ruleKey;

  protected ActiveRuleKey(QProfileKey qProfileKey, RuleKey ruleKey) {
    this.qProfileKey = qProfileKey;
    this.ruleKey = ruleKey;
  }

  /**
   * Create a key. Parameters are NOT null.
   */
  public static ActiveRuleKey of(QProfileKey qProfileKey, RuleKey ruleKey) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(qProfileKey.qProfile()), "QProfile is missing key");
    Preconditions.checkArgument(!Strings.isNullOrEmpty(qProfileKey.lang()), "QProfile is missing lang");
    Preconditions.checkArgument(!Strings.isNullOrEmpty(ruleKey.repository()), "RuleKey is missing repository");
    Preconditions.checkArgument(!Strings.isNullOrEmpty(ruleKey.rule()), "RuleKey is missing key");
    return new ActiveRuleKey(qProfileKey, ruleKey);
  }

  /**
   * Create a key from a string representation (see {@link #toString()}. An {@link IllegalArgumentException} is raised
   * if the format is not valid.
   */
  public static ActiveRuleKey parse(String s) {
    String[] split = s.split(":");
    Preconditions.checkArgument(split.length == 4, "Bad format of activeRule key: " + s);
    return ActiveRuleKey.of(QProfileKey.of(split[0], split[1]),
                             RuleKey.of(split[2], split[3]));
  }

  /**
   * Never null
   */
  public RuleKey ruleKey() {
    return ruleKey;
  }

  /**
   * Never null
   */
  public QProfileKey qProfile() {
    return qProfileKey;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ActiveRuleKey activeRuleKey = (ActiveRuleKey) o;
    if (!qProfileKey.equals(activeRuleKey.qProfileKey)) {
      return false;
    }
    if (!ruleKey.equals(activeRuleKey.ruleKey)) {
      return false;
    }
    return true;
  }

  @Override
  public int hashCode() {
    int result = qProfileKey.hashCode();
    result = 31 * result + ruleKey.hashCode();
    return result;
  }

  /**
   * Format is "qprofile:rule", for example "Java:squid:AvoidCycle:xpxp"
   */
  @Override
  public String toString() {
    return String.format("%s:%s", ruleKey.toString(), qProfileKey.toString());
  }
}

