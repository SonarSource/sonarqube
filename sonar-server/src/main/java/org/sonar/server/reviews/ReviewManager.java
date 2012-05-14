/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.server.reviews;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.reviews.ReviewAction;
import org.sonar.api.reviews.ReviewCommand;
import org.sonar.api.reviews.ReviewContext;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * This class helps handling {@link ReviewCommand} and {@link ReviewAction}, based on a {@link ReviewContext}.
 * 
 * @since 3.1
 */
public class ReviewManager {

  private Map<String, ReviewCommand> idToCommand = Maps.newLinkedHashMap();

  /**
   * Creates a {@link ReviewManager}
   * @param reviewCommands
   */
  public ReviewManager(ReviewCommand[] reviewCommands) {
    for (ReviewCommand reviewCommand : reviewCommands) {
      idToCommand.put(reviewCommand.getId(), reviewCommand);
    }
  }

  /**
   * Creates a {@link ReviewManager}
   */
  public ReviewManager() {
    this(new ReviewCommand[0]);
  }

  /**
   * Returns the available commands based on the given context.
   * 
   * @param reviewContext the review context
   * @return the list of available commands for this context
   */
  public Collection<ReviewCommand> getAvailableCommandsFor(ReviewContext reviewContext) {
    Preconditions.checkNotNull(reviewContext, "The review context must not be NULL when searching for available commands.");
    List<ReviewCommand> commands = Lists.newArrayList();
    for (ReviewCommand reviewCommand : idToCommand.values()) {
      if (reviewCommand.isAvailableFor(reviewContext)) {
        commands.add(reviewCommand);
      }
    }
    return commands;
  }

  /**
   * Filter the given command collection based on the review context and on the name of the interface that the command must implement.
   * 
   * @param initialCommands the initial list of commands
   * @param reviewContext the review context
   * @param interfaceName the name of the interface
   * @return the filtered list of commands
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  public Collection<ReviewCommand> filterCommands(Collection<ReviewCommand> initialCommands, ReviewContext reviewContext, String interfaceName) {
    Preconditions.checkNotNull(initialCommands, "The list of review commands must not be NULL when filtering commands.");
    Preconditions.checkState(StringUtils.isNotBlank(interfaceName), "The interface name must not be blank when searching for available commands.");

    Class interfaceClass = null;
    try {
      interfaceClass = Class.forName(interfaceName);
    } catch (ClassNotFoundException e) {
      throw new IllegalArgumentException("The following interface for review commands does not exist: " + interfaceName);
    }

    List<ReviewCommand> commands = Lists.newArrayList();
    for (ReviewCommand reviewCommand : initialCommands) {
      if (interfaceClass.isAssignableFrom(reviewCommand.getClass()) && reviewCommand.isAvailableFor(reviewContext)) {
        commands.add(reviewCommand);
      }
    }
    return commands;
  }

  /**
   * Executes the actions linked to the command which ID is passed as a paramter. 
   * 
   * @param commandId the command ID
   * @param reviewContext the review context that will be passed to the actions
   */
  public void executeCommandActions(String commandId, ReviewContext reviewContext) {
    Preconditions.checkNotNull(reviewContext, "The review context must not be NULL when executing the actions of a command.");
    ReviewCommand command = getCommand(commandId);
    Preconditions.checkState(command != null, "The command with the following ID does not exist: " + commandId);

    for (ReviewAction reviewAction : command.getActions()) {
      reviewAction.execute(reviewContext);
    }
  }

  /**
   * Returns the command corresponding to the given ID.
   * 
   * @param commandId the command ID
   * @return the command corresponding to the given ID, or null if none matches.
   */
  public ReviewCommand getCommand(String commandId) {
    return idToCommand.get(commandId);
  }

}
