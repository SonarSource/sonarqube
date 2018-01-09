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
import ProjectCard from './ProjectCard';
import NoFavoriteProjects from './NoFavoriteProjects';
import EmptyInstance from './EmptyInstance';
import EmptyFavoriteSearch from './EmptyFavoriteSearch';
import EmptySearch from '../../../components/common/EmptySearch';
import { Project } from '../types';
import { Query } from '../query';

interface Props {
  cardType?: string;
  isFavorite: boolean;
  isFiltered: boolean;
  onSonarCloud: boolean;
  organization?: { key: string };
  projects: Project[];
  query: Query;
}

export default class ProjectsList extends React.PureComponent<Props> {
  renderNoProjects() {
    const { isFavorite, isFiltered, query } = this.props;
    if (isFiltered) {
      return isFavorite ? <EmptyFavoriteSearch query={query} /> : <EmptySearch />;
    }
    return isFavorite ? (
      <NoFavoriteProjects onSonarCloud={this.props.onSonarCloud} />
    ) : (
      <EmptyInstance />
    );
  }

  render() {
    const { projects } = this.props;

    return (
      <div className="projects-list">
        {projects.length > 0
          ? projects.map(project => (
              <ProjectCard
                key={project.key}
                project={project}
                organization={this.props.organization}
                type={this.props.cardType}
              />
            ))
          : this.renderNoProjects()}
      </div>
    );
  }
}
