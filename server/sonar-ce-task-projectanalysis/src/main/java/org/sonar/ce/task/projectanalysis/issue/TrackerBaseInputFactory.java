/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.issue;

import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.filemove.MovedFilesRepository;
import org.sonar.ce.task.projectanalysis.filemove.MovedFilesRepository.OriginalFile;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.tracking.Input;
import org.sonar.db.DbClient;

/**
 * Factory of {@link Input} of base data for issue tracking. Data are lazy-loaded.
 */
public class TrackerBaseInputFactory extends BaseInputFactory {

  private final ComponentIssuesLoader issuesLoader;
  private final DbClient dbClient;
  private final MovedFilesRepository movedFilesRepository;

  public TrackerBaseInputFactory(ComponentIssuesLoader issuesLoader, DbClient dbClient, MovedFilesRepository movedFilesRepository) {
    this.issuesLoader = issuesLoader;
    this.dbClient = dbClient;
    this.movedFilesRepository = movedFilesRepository;
  }

  public Input<DefaultIssue> create(Component component) {
    if (component.getType() == Component.Type.PROJECT) {
      return new ProjectTrackerBaseLazyInput(dbClient, issuesLoader, component);
    }

    if (component.getType() == Component.Type.DIRECTORY) {
      // Folders have no issues
      return new EmptyTrackerBaseLazyInput(dbClient, component);
    }

    return new FileTrackerBaseLazyInput(dbClient, component, movedFilesRepository.getOriginalFile(component).orElse(null));
  }

  private class FileTrackerBaseLazyInput extends BaseLazyInput {

    private FileTrackerBaseLazyInput(DbClient dbClient, Component component, @Nullable OriginalFile originalFile) {
      super(dbClient, component, originalFile);
    }

    @Override
    protected List<DefaultIssue> loadIssues() {
      return issuesLoader.loadOpenIssues(effectiveUuid);
    }

  }

  private static class EmptyTrackerBaseLazyInput extends BaseLazyInput {

    private EmptyTrackerBaseLazyInput(DbClient dbClient, Component component) {
      super(dbClient, component, null);
    }

    @Override
    protected List<DefaultIssue> loadIssues() {
      return Collections.emptyList();
    }

  }

}
