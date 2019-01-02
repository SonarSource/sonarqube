/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import org.sonar.api.server.ServerSide;
import org.sonar.core.component.ComponentKeys;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

/**
 * Branch code for {@link ReportSubmitter}.
 * <p>
 * Does not support branches (except deprecated branch feature provided by "sonar.branch") unless an implementation of
 * {@link BranchSupportDelegate} is available.
 */
@ServerSide
public class BranchSupport {
  @CheckForNull
  private final BranchSupportDelegate delegate;

  /**
   * Constructor called by Pico when no implementation of {@link BranchSupportDelegate} is available.
   */
  public BranchSupport() {
    this(null);
  }

  public BranchSupport(@Nullable BranchSupportDelegate delegate) {
    this.delegate = delegate;
  }

  ComponentKey createComponentKey(String projectKey, @Nullable String deprecatedBranch, Map<String, String> characteristics) {
    if (characteristics.isEmpty()) {
      return new ComponentKeyImpl(projectKey, deprecatedBranch, ComponentKeys.createKey(projectKey, deprecatedBranch));
    } else {
      checkState(delegate != null, "Current edition does not support branch feature");
    }

    checkArgument(deprecatedBranch == null, "Deprecated branch feature can't be used at the same time as new branch support");
    return delegate.createComponentKey(projectKey, characteristics);
  }

  ComponentDto createBranchComponent(DbSession dbSession, ComponentKey componentKey, OrganizationDto organization,
    ComponentDto mainComponentDto, BranchDto mainComponentBranchDto) {
    checkState(delegate != null, "Current edition does not support branch feature");

    return delegate.createBranchComponent(dbSession, componentKey, organization, mainComponentDto, mainComponentBranchDto);
  }

  public abstract static class ComponentKey {
    public abstract String getKey();

    public abstract String getDbKey();

    public abstract Optional<String> getDeprecatedBranchName();

    public abstract Optional<Branch> getBranch();

    public abstract Optional<String> getPullRequestKey();

    public final boolean isDeprecatedBranch() {
      return getDeprecatedBranchName().isPresent();
    }

    public final boolean isMainBranch() {
      return !getBranch().isPresent() && !getPullRequestKey().isPresent();
    }

    /**
     * @return the {@link ComponentKey} of the main branch for this component.
     */
    public abstract ComponentKey getMainBranchComponentKey();
  }

  @Immutable
  public static final class Branch {
    private final String name;
    private final BranchType type;

    public Branch(String name, BranchType type) {
      this.name = requireNonNull(name, "name can't be null");
      this.type = requireNonNull(type, "type can't be null");
    }

    public String getName() {
      return name;
    }

    public BranchType getType() {
      return type;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      Branch branch = (Branch) o;
      return name.equals(branch.name) && type == branch.type;
    }

    @Override
    public int hashCode() {
      return Objects.hash(name, type);
    }

    @Override
    public String toString() {
      return "Branch{" +
        "name='" + name + '\'' +
        ", type=" + type +
        '}';
    }
  }

  private static final class ComponentKeyImpl extends ComponentKey {
    private final String key;
    private final String dbKey;
    @CheckForNull
    private final String deprecatedBranchName;

    public ComponentKeyImpl(String key, @Nullable String deprecatedBranchName, String dbKey) {
      this.key = key;
      this.deprecatedBranchName = deprecatedBranchName;
      this.dbKey = dbKey;
    }

    @Override
    public String getKey() {
      return key;
    }

    @Override
    public String getDbKey() {
      return dbKey;
    }

    @Override
    public Optional<String> getDeprecatedBranchName() {
      return Optional.ofNullable(deprecatedBranchName);
    }

    @Override
    public Optional<Branch> getBranch() {
      return Optional.empty();
    }

    @Override
    public Optional<String> getPullRequestKey() {
      return Optional.empty();
    }

    @Override
    public ComponentKey getMainBranchComponentKey() {
      return this;
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
      return Objects.equals(key, that.key) &&
        Objects.equals(dbKey, that.dbKey) &&
        Objects.equals(deprecatedBranchName, that.deprecatedBranchName);
    }

    @Override
    public int hashCode() {
      return Objects.hash(key, dbKey, deprecatedBranchName);
    }
  }
}
