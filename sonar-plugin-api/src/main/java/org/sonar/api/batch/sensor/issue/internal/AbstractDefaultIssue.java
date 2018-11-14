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
package org.sonar.api.batch.sensor.issue.internal;

import com.google.common.base.Preconditions;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.fs.internal.DefaultInputDir;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.batch.fs.internal.DefaultInputProject;
import org.sonar.api.batch.sensor.internal.DefaultStorable;
import org.sonar.api.batch.sensor.internal.SensorStorage;
import org.sonar.api.batch.sensor.issue.Issue.Flow;
import org.sonar.api.batch.sensor.issue.IssueLocation;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.utils.PathUtils;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.toList;

public abstract class AbstractDefaultIssue<T extends AbstractDefaultIssue> extends DefaultStorable {
  protected IssueLocation primaryLocation;
  protected List<List<IssueLocation>> flows = new ArrayList<>();
  protected DefaultInputProject project;

  protected AbstractDefaultIssue(DefaultInputProject project) {
    this(project, null);
  }

  public AbstractDefaultIssue(DefaultInputProject project, @Nullable SensorStorage storage) {
    super(storage);
    this.project = project;
  }

  public IssueLocation primaryLocation() {
    return primaryLocation;
  }

  public List<Flow> flows() {
    return this.flows.stream()
      .<Flow>map(l -> () -> unmodifiableList(new ArrayList<>(l)))
      .collect(toList());
  }

  public NewIssueLocation newLocation() {
    return new DefaultIssueLocation();
  }

  public T at(NewIssueLocation primaryLocation) {
    Preconditions.checkArgument(primaryLocation != null, "Cannot use a location that is null");
    checkState(this.primaryLocation == null, "at() already called");
    this.primaryLocation = rewriteLocation((DefaultIssueLocation) primaryLocation);
    Preconditions.checkArgument(this.primaryLocation.inputComponent() != null, "Cannot use a location with no input component");
    return (T) this;
  }

  public T addLocation(NewIssueLocation secondaryLocation) {
    flows.add(Collections.singletonList(rewriteLocation((DefaultIssueLocation) secondaryLocation)));
    return (T) this;
  }

  public T addFlow(Iterable<NewIssueLocation> locations) {
    List<IssueLocation> flowAsList = new ArrayList<>();
    for (NewIssueLocation issueLocation : locations) {
      flowAsList.add(rewriteLocation((DefaultIssueLocation) issueLocation));
    }
    flows.add(flowAsList);
    return (T) this;
  }

  private DefaultIssueLocation rewriteLocation(DefaultIssueLocation location) {
    InputComponent component = location.inputComponent();
    Optional<Path> dirOrModulePath = Optional.empty();

    if (component instanceof DefaultInputDir) {
      DefaultInputDir dirComponent = (DefaultInputDir) component;
      dirOrModulePath = Optional.of(project.getBaseDir().relativize(dirComponent.path()));
    } else if (component instanceof DefaultInputModule && !Objects.equals(project.key(), component.key())) {
      DefaultInputModule moduleComponent = (DefaultInputModule) component;
      dirOrModulePath = Optional.of(project.getBaseDir().relativize(moduleComponent.getBaseDir()));
    }

    if (dirOrModulePath.isPresent()) {
      String path = PathUtils.sanitize(dirOrModulePath.get().toString());
      DefaultIssueLocation fixedLocation = new DefaultIssueLocation();
      fixedLocation.on(project);
      StringBuilder fullMessage = new StringBuilder();
      if (!isNullOrEmpty(path)) {
        fullMessage.append("[").append(path).append("] ");
      }
      fullMessage.append(location.message());
      fixedLocation.message(fullMessage.toString());
      return fixedLocation;
    } else {
      return location;
    }
  }
}
