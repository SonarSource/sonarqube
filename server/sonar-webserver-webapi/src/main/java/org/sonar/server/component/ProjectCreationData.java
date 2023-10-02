package org.sonar.server.component;

import javax.annotation.Nullable;

public record ProjectCreationData(NewComponent newComponent, @Nullable String userUuid, @Nullable String userLogin, @Nullable String mainBranchName, boolean isManaged) {
  public ProjectCreationData(NewComponent newComponent, @Nullable String userUuid, @Nullable String userLogin, @Nullable String mainBranchName) {
    this(newComponent, userUuid, userLogin, mainBranchName, false);
  }

  public ProjectCreationData(NewComponent newComponent, @Nullable String userUuid, @Nullable String userLogin) {
    this(newComponent, userUuid, userLogin, null, false);
  }
}
