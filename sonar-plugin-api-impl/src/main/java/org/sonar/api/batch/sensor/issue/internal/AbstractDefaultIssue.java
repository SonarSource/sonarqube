/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
import org.sonar.api.batch.sensor.issue.MessageFormatting;
import org.sonar.api.batch.sensor.issue.NewIssue.FlowType;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.batch.sensor.issue.NewMessageFormatting;
import org.sonar.api.utils.PathUtils;

import static org.sonar.api.utils.Preconditions.checkArgument;
import static org.sonar.api.utils.Preconditions.checkState;

public abstract class AbstractDefaultIssue<T extends AbstractDefaultIssue> extends DefaultStorable {
  protected IssueLocation primaryLocation;
  protected List<DefaultIssueFlow> flows = new ArrayList<>();
  protected DefaultInputProject project;

  public AbstractDefaultIssue(DefaultInputProject project, @Nullable SensorStorage storage) {
    super(storage);
    this.project = project;
  }

  public IssueLocation primaryLocation() {
    return primaryLocation;
  }

  public List<Flow> flows() {
    return Collections.unmodifiableList(flows);
  }

  public NewIssueLocation newLocation() {
    return new DefaultIssueLocation();
  }

  public T at(NewIssueLocation primaryLocation) {
    checkArgument(primaryLocation != null, "Cannot use a location that is null");
    checkState(this.primaryLocation == null, "at() already called");
    this.primaryLocation = rewriteLocation((DefaultIssueLocation) primaryLocation);
    checkArgument(this.primaryLocation.inputComponent() != null, "Cannot use a location with no input component");
    return (T) this;
  }

  public T addLocation(NewIssueLocation secondaryLocation) {
    flows.add(new DefaultIssueFlow(List.of(rewriteLocation((DefaultIssueLocation) secondaryLocation)), FlowType.UNDEFINED, null));
    return (T) this;
  }

  public T addFlow(Iterable<NewIssueLocation> locations) {
    return addFlow(locations, FlowType.UNDEFINED, null);
  }

  public T addFlow(Iterable<NewIssueLocation> flowLocations, FlowType type, @Nullable String flowDescription) {
    checkArgument(type != null, "Type can't be null");
    List<IssueLocation> flowAsList = new ArrayList<>();
    for (NewIssueLocation issueLocation : flowLocations) {
      flowAsList.add(rewriteLocation((DefaultIssueLocation) issueLocation));
    }
    flows.add(new DefaultIssueFlow(flowAsList, type, flowDescription));
    return (T) this;
  }

  private DefaultIssueLocation rewriteLocation(DefaultIssueLocation location) {
    InputComponent component = location.inputComponent();
    Optional<Path> dirOrModulePath = Optional.empty();

    if (component instanceof DefaultInputDir defaultInputDir) {
      dirOrModulePath = Optional.of(project.getBaseDir().relativize(defaultInputDir.path()));
    } else if (component instanceof DefaultInputModule defaultInputModule && !Objects.equals(project.key(), component.key())) {
      dirOrModulePath = Optional.of(project.getBaseDir().relativize(defaultInputModule.getBaseDir()));
    }

    if (dirOrModulePath.isPresent()) {
      String path = PathUtils.sanitize(dirOrModulePath.get().toString());
      DefaultIssueLocation fixedLocation = new DefaultIssueLocation();
      fixedLocation.on(project);
      StringBuilder fullMessage = new StringBuilder();
      String prefixMessage;
      if (path != null && !path.isEmpty()) {
        prefixMessage = "[" + path + "] ";
      } else {
        prefixMessage = "";
      }

      fullMessage.append(prefixMessage);
      fullMessage.append(location.message());

      List<NewMessageFormatting> paddedFormattings = location.messageFormattings().stream()
        .map(m -> padMessageFormatting(m, prefixMessage.length()))
        .toList();

      fixedLocation.message(fullMessage.toString(), paddedFormattings);

      return fixedLocation;
    } else {
      return location;
    }
  }

  private static NewMessageFormatting padMessageFormatting(MessageFormatting messageFormatting, int length) {
    return new DefaultMessageFormatting().type(messageFormatting.type())
      .start(messageFormatting.start() + length)
      .end(messageFormatting.end() + length);
  }
}
