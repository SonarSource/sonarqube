/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nullable;
import org.sonar.api.batch.sensor.internal.DefaultStorable;
import org.sonar.api.batch.sensor.internal.SensorStorage;
import org.sonar.api.batch.sensor.issue.Issue.Flow;
import org.sonar.api.batch.sensor.issue.IssueLocation;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.toList;

public abstract class AbstractDefaultIssue<T extends AbstractDefaultIssue>  extends DefaultStorable {
  protected IssueLocation primaryLocation;
  protected List<List<IssueLocation>> flows = new ArrayList<>();
  
  protected AbstractDefaultIssue() {
    super(null);
  }
  
  public AbstractDefaultIssue(@Nullable SensorStorage storage) {
    super(storage);
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
    this.primaryLocation = (DefaultIssueLocation) primaryLocation;
    Preconditions.checkArgument(this.primaryLocation.inputComponent() != null, "Cannot use a location with no input component");
    return (T) this;
  }

  public T addLocation(NewIssueLocation secondaryLocation) {
    flows.add(Arrays.asList((IssueLocation) secondaryLocation));
    return (T) this;
  }

  public T addFlow(Iterable<NewIssueLocation> locations) {
    List<IssueLocation> flowAsList = new ArrayList<>();
    for (NewIssueLocation issueLocation : locations) {
      flowAsList.add((DefaultIssueLocation) issueLocation);
    }
    flows.add(flowAsList);
    return (T) this;
  }
  
}
