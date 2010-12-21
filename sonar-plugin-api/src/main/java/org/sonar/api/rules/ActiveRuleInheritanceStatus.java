package org.sonar.api.rules;

/**
 * For internal use only.
 * 
 * @since 2.5
 */
public enum ActiveRuleInheritanceStatus {
  /**
   * WARNING : DO NOT CHANGE THE ENUMERATION ORDER
   * the enum ordinal is used for db persistence
   */
  NO, INHERITED, OVERRIDDEN
}
