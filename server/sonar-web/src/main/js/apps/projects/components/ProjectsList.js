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
//@flow
import React from 'react';
import ProjectCardContainer from './ProjectCardContainer';
import NoFavoriteProjects from './NoFavoriteProjects';
import EmptyInstance from './EmptyInstance';
import EmptySearch from '../../../components/common/EmptySearch';

type Props = {
  projects?: Array<string>,
  isFavorite: boolean,
  isFiltered: boolean,
  organization?: { key: string },
  cardType?: string
};

export default class ProjectsList extends React.PureComponent {
  props: Props;

  renderNoProjects() {
    if (this.props.isFavorite && !this.props.isFiltered) {
      return <NoFavoriteProjects />;
    } else if (!this.props.isFiltered) {
      return <EmptyInstance />;
    } else {
      return <EmptySearch />;
    }
  }

  render() {
    const { projects } = this.props;

    if (projects == null) {
      return null;
    }

    return (
      <div className="projects-list">
        {projects.length > 0
          ? projects.map(projectKey => (
              <ProjectCardContainer
                key={projectKey}
                projectKey={projectKey}
                organization={this.props.organization}
                type={this.props.cardType}
              />
            ))
          : this.renderNoProjects()}
      </div>
    );
  }
}
