/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import classNames from 'classnames';
import * as React from 'react';
import { Project } from '../../api/project-management';
import { translate } from '../../helpers/l10n';
import { LoggedInUser } from '../../types/users';
import ProjectRow from './ProjectRow';

interface Props {
  currentUser: Pick<LoggedInUser, 'login'>;
  onProjectDeselected: (project: Project) => void;
  onProjectSelected: (project: Project) => void;
  projects: Project[];
  ready?: boolean;
  selection: Project[];
}

export default function Projects(props: Readonly<Props>) {
  const { ready, projects, currentUser, selection } = props;

  const onProjectCheck = (project: Project, checked: boolean) => {
    if (checked) {
      props.onProjectSelected(project);
    } else {
      props.onProjectDeselected(project);
    }
  };

  return (
    <div className="boxed-group boxed-group-inner">
      <table
        className={classNames('data', 'zebra', { 'new-loading': !ready })}
        id="projects-management-page-projects"
      >
        <thead>
          <tr>
            <th />
            <th>{translate('name')}</th>
            <th />
            <th>{translate('key')}</th>
            <th className="thin nowrap text-right">{translate('last_analysis')}</th>
            <th />
          </tr>
        </thead>
        <tbody>
          {projects.map((project) => (
            <ProjectRow
              currentUser={currentUser}
              key={project.key}
              onProjectCheck={onProjectCheck}
              project={project}
              selected={selection.some((s) => s.key === project.key)}
            />
          ))}
        </tbody>
      </table>
    </div>
  );
}
