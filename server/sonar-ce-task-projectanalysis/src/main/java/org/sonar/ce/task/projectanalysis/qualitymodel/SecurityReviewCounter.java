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
package org.sonar.ce.task.projectanalysis.qualitymodel;

import org.sonar.core.issue.DefaultIssue;

import static org.sonar.api.issue.Issue.STATUS_REVIEWED;
import static org.sonar.api.issue.Issue.STATUS_TO_REVIEW;

final class SecurityReviewCounter {
  private int hotspotsReviewed;
  private int hotspotsToReview;

  SecurityReviewCounter() {
    // prevents instantiation
  }

  void processHotspot(DefaultIssue issue) {
    if (issue.status().equals(STATUS_REVIEWED)) {
      hotspotsReviewed++;
    } else if (issue.status().equals(STATUS_TO_REVIEW)) {
      hotspotsToReview++;
    }
  }

  void add(SecurityReviewCounter otherCounter) {
    hotspotsReviewed += otherCounter.hotspotsReviewed;
    hotspotsToReview += otherCounter.hotspotsToReview;
  }

  public int getHotspotsReviewed() {
    return hotspotsReviewed;
  }

  public int getHotspotsToReview() {
    return hotspotsToReview;
  }
}
