/*
 * Copyright (C) 2017-2022 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package org.sonar.core.sarif;

import com.google.gson.annotations.SerializedName;

public class Tool {
  @SerializedName("driver")
  private final Driver driver;

  public Tool(Driver driver) {
    this.driver = driver;
  }

  public Driver getDriver() {
    return driver;
  }
}
