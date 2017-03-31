/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import React from 'react';
import { Link } from 'react-router';
import { getComponentPermissionsUrl } from '../../helpers/urls';
import ApplyTemplateView from '../permissions/project/views/ApplyTemplateView';
import Checkbox from '../../components/controls/Checkbox';
import QualifierIcon from '../../components/shared/qualifier-icon';
import { translate } from '../../helpers/l10n';

export default class Projects extends React.Component {
  static propTypes = {
    projects: React.PropTypes.array.isRequired,
    selection: React.PropTypes.array.isRequired,
    organization: React.PropTypes.object
  };

  componentWillMount() {
    this.renderProject = this.renderProject.bind(this);
  }

  onProjectCheck(project, checked) {
    if (checked) {
      this.props.onProjectSelected(project);
    } else {
      this.props.onProjectDeselected(project);
    }
  }

  onApplyTemplateClick(project, e) {
    e.preventDefault();
    e.target.blur();
    new ApplyTemplateView({
      project,
      organization: this.props.organization
    }).render();
  }

  isProjectSelected(project) {
    return this.props.selection.indexOf(project.key) !== -1;
  }

  renderProject(project) {
    const permissionsUrl = getComponentPermissionsUrl(project.key);

    return (
      <tr key={project.key}>
        <td className="thin">
          <Checkbox
            checked={this.isProjectSelected(project)}
            onCheck={this.onProjectCheck.bind(this, project)}
          />
        </td>
        <td className="nowrap">
          <Link
            to={{ pathname: '/dashboard', query: { id: project.key } }}
            className="link-with-icon">
            <QualifierIcon qualifier={project.qualifier} />
            {' '}
            <span>{project.name}</span>
          </Link>
        </td>
        <td className="nowrap">
          <span className="note">{project.key}</span>
        </td>
        <td className="thin nowrap">
          <div className="dropdown">
            <button className="dropdown-toggle" data-toggle="dropdown">
              {translate('actions')}
              {' '}
              <i className="icon-dropdown" />
            </button>
            <ul className="dropdown-menu dropdown-menu-right">
              <li>
                <Link to={permissionsUrl}>
                  {translate('edit_permissions')}
                </Link>
              </li>
              <li>
                <a href="#" onClick={this.onApplyTemplateClick.bind(this, project)}>
                  {translate('projects_role.apply_template')}
                </a>
              </li>
            </ul>
          </div>
        </td>
      </tr>
    );
  }

  render() {
    const className = classNames('data', 'zebra', { 'new-loading': !this.props.ready });

    return (
      <table className={className}>
        <tbody>{this.props.projects.map(this.renderProject)}</tbody>
      </table>
    );
  }
}
