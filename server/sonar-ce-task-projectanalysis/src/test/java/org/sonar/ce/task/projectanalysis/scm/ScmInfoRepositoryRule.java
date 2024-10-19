/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
package org.sonar.ce.task.projectanalysis.scm;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.rules.ExternalResource;
import org.sonar.ce.task.projectanalysis.component.Component;

import static com.google.common.base.Preconditions.checkNotNull;

public class ScmInfoRepositoryRule extends ExternalResource implements ScmInfoRepository, AfterEachCallback {

  private Map<Integer, ScmInfo> scmInfoByFileRef = new HashMap<>();

  @Override
  protected void after() {
    scmInfoByFileRef.clear();
  }

  @Override
  public Optional<ScmInfo> getScmInfo(Component component) {
    checkNotNull(component, "Component cannot be bull");
    ScmInfo scmInfo = scmInfoByFileRef.get(component.getReportAttributes().getRef());
    return Optional.ofNullable(scmInfo);
  }

  public ScmInfoRepositoryRule setScmInfo(int fileRef, Changeset... changesetList) {
    scmInfoByFileRef.put(fileRef, new ScmInfoImpl(changesetList));
    return this;
  }

  public ScmInfoRepositoryRule setScmInfo(int fileRef, Map<Integer, Changeset> changesets) {
    scmInfoByFileRef.put(fileRef, new ScmInfoImpl(changesets.values().toArray(new Changeset[0])));
    return this;
  }

  @Override
  public void afterEach(ExtensionContext extensionContext) throws Exception {
    after();
  }
}
