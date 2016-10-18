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
import ProjectCardMeasures from './ProjectCardMeasures';
import { getComponentUrl } from '../../../helpers/urls';

export default class ProjectCard extends React.Component {
  static propTypes = {
    project: React.PropTypes.object
  };

  render () {
    const { project } = this.props;

    if (project == null) {
      return null;
    }

    return (
        <div className="boxed-group project-card">
          <h2 className="project-card-name">
            <a className="link-base-color" href={getComponentUrl(project.key)}>{project.name}</a>
          </h2>
          <div className="boxed-group-inner">
            <ProjectCardMeasures measures={this.props.measures}/>
          </div>
        </div>
    );
  }
}
