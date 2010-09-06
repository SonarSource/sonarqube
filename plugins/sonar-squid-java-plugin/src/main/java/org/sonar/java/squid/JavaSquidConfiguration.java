/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
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
package org.sonar.java.squid;

import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Set;

import org.sonar.squid.api.SquidConfiguration;

public class JavaSquidConfiguration extends SquidConfiguration {

  private boolean analysePropertyAccessors = true;
  private Set<String> fieldsToExcludeFromLcom4Calculation = new HashSet<String>();

  private static final double COMMENTED_CODE_DEFAULT_THRESHOLD = 0.9;
  private double commentedCodeThreshold = COMMENTED_CODE_DEFAULT_THRESHOLD;

  public JavaSquidConfiguration() {
  }

  public JavaSquidConfiguration(boolean analysePropertyAccessors) {
    this.analysePropertyAccessors = analysePropertyAccessors;
  }

  public JavaSquidConfiguration(double commentedCodeThreshold) {
    setCommentedCodeThreshold(commentedCodeThreshold);
  }

  public JavaSquidConfiguration(boolean analysePropertyAccessors, Charset charset) {
    super(charset);
    this.analysePropertyAccessors = analysePropertyAccessors;
  }

  public JavaSquidConfiguration(boolean analysePropertyAccessors, Charset charset, double commentedCodeThreshold) {
    super(charset);
    this.analysePropertyAccessors = analysePropertyAccessors;
    setCommentedCodeThreshold(commentedCodeThreshold);
  }

  public boolean isAnalysePropertyAccessors() {
    return analysePropertyAccessors;
  }

  public Set<String> getFielsToExcludeFromLcom4Calculation() {
    return fieldsToExcludeFromLcom4Calculation;
  }

  public void addFieldToExcludeFromLcom4Calculation(String fieldName) {
    fieldsToExcludeFromLcom4Calculation.add(fieldName);
  }

  private void setCommentedCodeThreshold(double commentedCodeThreshold) {
    if (commentedCodeThreshold < 0 || commentedCodeThreshold > 1) {
      throw new IllegalArgumentException("Commented Code Threshold should be between [0...1]. Current value : " + commentedCodeThreshold);
    }
    this.commentedCodeThreshold = commentedCodeThreshold;
  }

  public double getCommentedCodeThreshold() {
    return commentedCodeThreshold;
  }
}