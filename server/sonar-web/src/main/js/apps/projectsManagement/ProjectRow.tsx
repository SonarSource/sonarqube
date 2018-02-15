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
import { Link } from 'react-router';
import ProjectRowActions from './ProjectRowActions';
import { Project } from './utils';
import { Visibility } from '../../app/types';
import PrivateBadge from '../../components/common/PrivateBadge';
import Checkbox from '../../components/controls/Checkbox';
import QualifierIcon from '../../components/shared/QualifierIcon';
import DateTooltipFormatter from '../../components/intl/DateTooltipFormatter';

interface Props {
  currentUser: { login: string };
  onProjectCheck: (project: Project, checked: boolean) => void;
  organization: string | undefined;
  project: Project;
  selected: boolean;
}

export default class ProjectRow extends React.PureComponent<Props> {
  handleProjectCheck = (checked: boolean) => {
    this.props.onProjectCheck(this.props.project, checked);
  };

  render() {
    const { project, selected } = this.props;

    return (
      <tr>
        <td className="thin">
          <Checkbox checked={selected} onCheck={this.handleProjectCheck} />
        </td>

        <td className="nowrap">
          <Link
            className="link-with-icon"
            to={{ pathname: '/dashboard', query: { id: project.key } }}>
            <QualifierIcon qualifier={project.qualifier} /> <span>{project.name}</span>
          </Link>
        </td>

        <td className="thin nowrap">
          {project.visibility === Visibility.Private && (
            <PrivateBadge qualifier={project.qualifier} />
          )}
        </td>

        <td className="nowrap">
          <span className="note">{project.key}</span>
        </td>

        <td className="thin nowrap text-right">
          {project.lastAnalysisDate ? (
            <DateTooltipFormatter date={project.lastAnalysisDate} />
          ) : (
            <span className="note">—</span>
          )}
        </td>

        <td className="thin nowrap">
          <ProjectRowActions
            currentUser={this.props.currentUser}
            organization={this.props.organization}
            project={project}
          />
        </td>
      </tr>
    );
  }
}
