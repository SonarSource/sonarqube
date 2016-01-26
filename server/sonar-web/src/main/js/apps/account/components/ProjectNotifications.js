/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import React from 'react';
import Select from 'react-select';

import NotificationsList from './NotificationsList';
import { translate } from '../../../helpers/l10n';
import { getProjectsWithInternalId } from '../../../api/components';

export default function ProjectNotifications ({ notifications, channels, onAddProject, onRemoveProject }) {
  const loadOptions = (query) => {
    return getProjectsWithInternalId(query)
        .then(results => results.map(r => {
          return {
            value: r.id,
            label: r.text
          };
        }))
        .then(options => {
          return { options };
        });
  };

  const handleAddProject = (selected) => {
    const project = {
      internalId: selected.value,
      name: selected.label
    };
    onAddProject(project);
  };

  const handleRemoveProject = (project) => (
      (e) => {
        e.preventDefault;
        onRemoveProject(project);
      }
  );

  return (
      <div>
        <header className="page-header">
          <h2 className="page-title">
            {translate('my_profile.per_project_notifications.title')}
          </h2>
          <div className="pull-right">
            <Select.Async
                name="new_project"
                style={{ width: '150px' }}
                loadOptions={loadOptions}
                onChange={handleAddProject}
                placeholder="Add Project"/>
          </div>
        </header>

        {!notifications.length && (
            <div className="note">
              {translate('my_account.no_project_notifications')}
            </div>
        )}

        {notifications.map(p => (
            <table key={p.project.internalId} className="form spacer-bottom">
              <thead>
                <tr>
                  <th>
                    <a onClick={handleRemoveProject(p.project)}
                       className="spacer-right icon-delete js-delete-project" href="#"></a>
                    <h3 className="display-inline-block">{p.project.name}</h3>
                  </th>
                  {channels.map(channel => (
                      <th key={channel} className="text-center">
                        <h4>{translate('notification.channel', channel)}</h4>
                      </th>
                  ))}
                </tr>
              </thead>
              <NotificationsList
                  notifications={p.notifications}
                  checkboxId={(d, c) => `project_notifs_${p.project.internalId}_${d}_${c}`}
                  checkboxName={(d, c) => `project_notifs[${p.project.internalId}][${d}][${c}]`}/>
            </table>
        ))}
      </div>
  );
}
