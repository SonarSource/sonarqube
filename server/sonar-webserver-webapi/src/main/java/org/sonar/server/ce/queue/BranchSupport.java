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
package org.sonar.server.ce.queue;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.server.ServerSide;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;

import static com.google.common.base.Preconditions.checkState;
import static org.sonar.core.ce.CeTaskCharacteristics.BRANCH;
import static org.sonar.core.ce.CeTaskCharacteristics.BRANCH_TYPE;
import static org.sonar.core.ce.CeTaskCharacteristics.PULL_REQUEST;

/**
 * Branch code for {@link ReportSubmitter}.
 * <p>
 * Does not support branches unless an implementation of {@link BranchSupportDelegate} is available.
 */
@ServerSide
public class BranchSupport {
  private static final Set<String> BRANCH_CHARACTERISTICS = Set.of(BRANCH, BRANCH_TYPE, PULL_REQUEST);
  @CheckForNull
  private final BranchSupportDelegate delegate;

  public BranchSupport(@Nullable BranchSupportDelegate delegate) {
    this.delegate = delegate;
  }

  ComponentKey createComponentKey(String projectKey, Map<String, String> characteristics) {
    boolean containsBranchCharacteristics = characteristics.keySet().stream().anyMatch(BRANCH_CHARACTERISTICS::contains);
    if (containsBranchCharacteristics) {
      checkState(delegate != null, "Current edition does not support branch feature");
      return delegate.createComponentKey(projectKey, characteristics);
    }
    return new ComponentKeyImpl(projectKey);
  }

  ComponentDto createBranchComponent(DbSession dbSession, ComponentKey componentKey, OrganizationDto organizationDto, ComponentDto mainComponentDto, BranchDto mainComponentBranchDto) {
    checkState(delegate != null, "Current edition does not support branch feature");

    return delegate.createBranchComponent(dbSession, componentKey, organizationDto, mainComponentDto, mainComponentBranchDto);
  }

  public abstract static class ComponentKey {
    public abstract String getKey();

    public abstract Optional<String> getBranchName();

    public abstract Optional<String> getPullRequestKey();

    public final boolean isMainBranch() {
      return !getBranchName().isPresent() && !getPullRequestKey().isPresent();
    }
  }

  private static final class ComponentKeyImpl extends ComponentKey {
    private final String key;

    public ComponentKeyImpl(String key) {
      this.key = key;
    }

    @Override
    public String getKey() {
      return key;
    }

    @Override
    public Optional<String> getBranchName() {
      return Optional.empty();
    }

    @Override
    public Optional<String> getPullRequestKey() {
      return Optional.empty();
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      ComponentKeyImpl that = (ComponentKeyImpl) o;
      return Objects.equals(key, that.key);
    }

    @Override
    public int hashCode() {
      return Objects.hash(key);
    }
  }
}
