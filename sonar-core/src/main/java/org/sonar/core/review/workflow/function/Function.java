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
package org.sonar.core.review.workflow.function;

import com.google.common.annotations.Beta;
import org.sonar.core.review.workflow.review.MutableReview;
import org.sonar.core.review.workflow.review.Review;
import org.sonar.core.review.workflow.review.WorkflowContext;

import java.util.Map;

/**
 * @since 3.1
 */
@Beta
public abstract class Function {

  /**
   * This method is executed when all the conditions pass.
   *
   * @param review the review that can be changed
   * @param initialReview the read-only review as stated before execution of functions
   * @param context information about the user who executed the command and about project
   * @param parameters the command parameters sent by end user, generally from forms displayed in screens
   */
  public abstract void doExecute(MutableReview review, Review initialReview, WorkflowContext context, Map<String, String> parameters);

}
