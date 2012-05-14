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
package org.sonar.api.reviews;

import com.google.common.annotations.Beta;
import org.sonar.api.ServerExtension;

import java.util.Collection;

/**
 * Represents a command that can be displayed for a review.
 * 
 * @since 3.1
 */
@Beta
public abstract class ReviewCommand implements ServerExtension {

  /**
   * Returns the ID of the command.
   * 
   * @return the ID
   */
  public abstract String getId();

  /**
   * Returns the name of the command.
   * 
   * @return the name
   */
  public abstract String getName();

  /**
   * Returns the {@link ReviewAction} linked to this command.
   * 
   * @return the list of actions
   */
  public abstract Collection<ReviewAction> getActions();

  /**
   * Tells is the command is available in the given review context.
   * 
   * @param reviewContext the context of the review
   * @return true if the command is available, false otherwise
   */
  public abstract boolean isAvailableFor(ReviewContext reviewContext);

}
