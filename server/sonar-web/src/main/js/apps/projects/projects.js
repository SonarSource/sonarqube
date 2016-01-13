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
import classNames from 'classnames';
import React from 'react';
import { getComponentUrl } from '../../helpers/urls';
import Checkbox from '../../components/shared/checkbox';
import QualifierIcon from '../../components/shared/qualifier-icon';

export default React.createClass({
  propTypes: {
    projects: React.PropTypes.array.isRequired,
    selection: React.PropTypes.array.isRequired,
    refresh: React.PropTypes.func.isRequired
  },

  onProjectCheck(project, checked) {
    if (checked) {
      this.props.onProjectSelected(project);
    } else {
      this.props.onProjectDeselected(project);
    }
  },

  isProjectSelected(project) {
    return this.props.selection.indexOf(project.id) !== -1;
  },

  renderProject(project) {
    return (
        <tr key={project.id}>
          <td className="thin">
            <Checkbox onCheck={this.onProjectCheck.bind(this, project)}
                      initiallyChecked={this.isProjectSelected(project)}/>
          </td>
          <td className="thin">
            <QualifierIcon qualifier={project.qualifier}/>
          </td>
          <td className="nowrap">
            <a href={getComponentUrl(project.key)}>{project.name}</a>
          </td>
          <td className="nowrap">
            <span className="note">{project.key}</span>
          </td>
        </tr>
    );
  },

  render() {
    let className = classNames('data', 'zebra', { 'new-loading': !this.props.ready });
    return (
        <table className={className}>
          <tbody>{this.props.projects.map(this.renderProject)}</tbody>
        </table>
    );
  }
});
