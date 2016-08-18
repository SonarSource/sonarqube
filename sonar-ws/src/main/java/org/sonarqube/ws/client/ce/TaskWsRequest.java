/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonarqube.ws.client.ce;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TaskWsRequest {
  private final String taskUuid;
  private final List<String> additionalFields;

  private TaskWsRequest(Builder builder) {
    this.taskUuid = builder.taskUuid;
    this.additionalFields = createAdditionalFields(builder);
  }
  public static Builder newBuilder(String taskUuid) {
    return new Builder(taskUuid);
  }

  private static List<String> createAdditionalFields(Builder builder) {
    if (!builder.errorStacktrace && !builder.scannerContext) {
      return Collections.emptyList();
    }
    List<String> res = new ArrayList<>(2);
    if (builder.errorStacktrace) {
      res.add("stacktrace");
    }
    if (builder.scannerContext) {
      res.add("scannerContext");
    }
    return ImmutableList.copyOf(res);
  }

  public String getTaskUuid() {
    return taskUuid;
  }

  public List<String> getAdditionalFields() {
    return additionalFields;
  }

  public static final class Builder {
    private final String taskUuid;
    private boolean errorStacktrace = false;
    private boolean scannerContext = false;

    private Builder(String taskUuid) {
      this.taskUuid = taskUuid;
    }

    public Builder withErrorStacktrace() {
      this.errorStacktrace = true;
      return this;
    }

    public Builder withScannerContext() {
      this.scannerContext = true;
      return this;
    }

    public TaskWsRequest build() {
      return new TaskWsRequest(this);
    }
  }
}
