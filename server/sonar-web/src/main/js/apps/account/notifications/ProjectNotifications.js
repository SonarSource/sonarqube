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

import ProjectNotification from './ProjectNotification';
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

  return (
      <section>
        <h2 className="spacer-bottom">
          {translate('my_profile.per_project_notifications.title')}
        </h2>

        {!notifications.length && (
            <div className="note">
              {translate('my_account.no_project_notifications')}
            </div>
        )}

        {notifications.map(p => (
            <ProjectNotification
                key={p.project.internalId}
                data={p}
                channels={channels}
                onRemoveProject={onRemoveProject}/>
        ))}

        <div className="spacer-top panel bg-muted">
          <span className="text-middle spacer-right">
            Set notifications for:
          </span>
          <Select.Async
              name="new_project"
              style={{ width: '150px' }}
              loadOptions={loadOptions}
              onChange={handleAddProject}
              placeholder="Search Project"/>
        </div>
      </section>
  );
}
