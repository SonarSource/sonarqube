/*
 * Copyright (C) 2017-2022 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package org.sonar.core.sarif;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class CodeFlow {
  @SerializedName("threadFlows")
  private final List<ThreadFlow> threadFlows;

  private CodeFlow(List<ThreadFlow> threadFlows) {
    this.threadFlows = threadFlows;
  }

  public static CodeFlow of(List<ThreadFlow> threadFlows) {
    return new CodeFlow(threadFlows);
  }

  public List<ThreadFlow> getThreadFlows() {
    return threadFlows;
  }
}
