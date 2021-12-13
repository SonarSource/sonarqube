/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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
import * as React from 'react';
import { Button } from '../../components/controls/buttons';
import { translate } from '../../helpers/l10n';
import { Application, ApplicationProject } from '../../types/application';
import { Branch } from '../../types/branch-like';
import ApplicationBranches from './ApplicationBranches';
import ApplicationProjects from './ApplicationProjects';
import EditForm from './EditForm';

export interface ApplicationConsoleAppRendererProps {
  loading: boolean;
  application: Application;
  canBrowseAllChildProjects: boolean;
  onAddProject: (project: ApplicationProject) => void;
  onRefresh: () => void;
  onEdit: (name: string, description: string) => Promise<void>;
  onRemoveProject: (projectKey: string) => void;
  onUpdateBranches: (branches: Branch[]) => void;
}

export default function ApplicationConsoleAppRenderer(props: ApplicationConsoleAppRendererProps) {
  const [editing, setEditing] = React.useState(false);

  const { application, canBrowseAllChildProjects, loading } = props;

  if (loading) {
    return <i className="spinner spacer" />;
  }

  return (
    <div className="page page-limited">
      <div className="boxed-group" id="view-details">
        <div className="boxed-group-actions">
          <Button
            className="little-spacer-right"
            id="view-details-edit"
            onClick={() => setEditing(true)}>
            {translate('edit')}
          </Button>
          <Button className="little-spacer-right" onClick={props.onRefresh}>
            {translate('application_console.recompute')}
          </Button>
        </div>

        <header className="boxed-group-header" id="view-details-header">
          <h2 className="text-limited" title={application.name}>
            {application.name}
          </h2>
        </header>

        <div className="boxed-group-inner" id="view-details-content">
          <div className="big-spacer-bottom">
            {application.description && (
              <div className="little-spacer-bottom">{application.description}</div>
            )}
            <div className="subtitle">
              {translate('key')}: {application.key}
            </div>
          </div>

          <ApplicationProjects
            onAddProject={props.onAddProject}
            onRemoveProject={props.onRemoveProject}
            application={application}
          />

          <ApplicationBranches
            application={application}
            canBrowseAllChildProjects={canBrowseAllChildProjects}
            onUpdateBranches={props.onUpdateBranches}
          />
        </div>

        {editing && (
          <EditForm
            header={translate('application_console.edit')}
            onClose={() => setEditing(false)}
            onEdit={props.onEdit}
            application={application}
          />
        )}
      </div>
    </div>
  );
}
