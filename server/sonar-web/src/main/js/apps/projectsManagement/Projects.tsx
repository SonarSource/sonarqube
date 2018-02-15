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
import * as React from 'react';
import * as classNames from 'classnames';
import ProjectRow from './ProjectRow';
import { Project } from './utils';
import { Organization } from '../../app/types';
import { translate } from '../../helpers/l10n';

interface Props {
  currentUser: { login: string };
  onProjectDeselected: (project: string) => void;
  onProjectSelected: (project: string) => void;
  organization: Organization;
  projects: Project[];
  ready?: boolean;
  selection: string[];
}

export default class Projects extends React.PureComponent<Props> {
  onProjectCheck = (project: Project, checked: boolean) => {
    if (checked) {
      this.props.onProjectSelected(project.key);
    } else {
      this.props.onProjectDeselected(project.key);
    }
  };

  render() {
    return (
      <div className="boxed-group boxed-group-inner">
        <table
          className={classNames('data', 'zebra', { 'new-loading': !this.props.ready })}
          id="projects-management-page-projects">
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
            {this.props.projects.map(project => (
              <ProjectRow
                currentUser={this.props.currentUser}
                key={project.key}
                onProjectCheck={this.onProjectCheck}
                organization={this.props.organization && this.props.organization.key}
                project={project}
                selected={this.props.selection.includes(project.key)}
              />
            ))}
          </tbody>
        </table>
      </div>
    );
  }
}
