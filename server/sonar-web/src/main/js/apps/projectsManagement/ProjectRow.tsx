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
import * as React from 'react';
import { Link } from 'react-router';
import { Project, Visibility } from './utils';
import PrivateBadge from '../../components/common/PrivateBadge';
import Checkbox from '../../components/controls/Checkbox';
import QualifierIcon from '../../components/shared/QualifierIcon';
import { translate } from '../../helpers/l10n';
import { getComponentPermissionsUrl } from '../../helpers/urls';

interface Props {
  onApplyTemplateClick: (project: Project) => void;
  onProjectCheck: (project: Project, checked: boolean) => void;
  project: Project;
  selected: boolean;
}

export default class ProjectRow extends React.PureComponent<Props> {
  handleProjectCheck = (checked: boolean) => {
    this.props.onProjectCheck(this.props.project, checked);
  };

  handleApplyTemplateClick = (event: React.SyntheticEvent<HTMLAnchorElement>) => {
    event.preventDefault();
    event.currentTarget.blur();
    this.props.onApplyTemplateClick(this.props.project);
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
            to={{ pathname: '/dashboard', query: { id: project.key } }}
            className="link-with-icon">
            <QualifierIcon qualifier={project.qualifier} /> <span>{project.name}</span>
          </Link>
        </td>

        <td className="nowrap">
          <span className="note">
            {project.key}
          </span>
        </td>

        <td className="width-20">
          {project.visibility === Visibility.Private && <PrivateBadge />}
        </td>

        <td className="thin nowrap">
          <div className="dropdown">
            <button className="dropdown-toggle" data-toggle="dropdown">
              {translate('actions')} <i className="icon-dropdown" />
            </button>
            <ul className="dropdown-menu dropdown-menu-right">
              <li>
                <Link to={getComponentPermissionsUrl(project.key)}>
                  {translate('edit_permissions')}
                </Link>
              </li>
              <li>
                <a className="js-apply-template" href="#" onClick={this.handleApplyTemplateClick}>
                  {translate('projects_role.apply_template')}
                </a>
              </li>
            </ul>
          </div>
        </td>
      </tr>
    );
  }
}
