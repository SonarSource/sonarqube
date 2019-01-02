/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import { AutoSizer } from 'react-virtualized/dist/commonjs/AutoSizer';
import { List, ListRowProps } from 'react-virtualized/dist/commonjs/List';
import { WindowScroller } from 'react-virtualized/dist/commonjs/WindowScroller';
import ProjectCard from './ProjectCard';
import NoFavoriteProjects from './NoFavoriteProjects';
import EmptyInstance from './EmptyInstance';
import EmptyFavoriteSearch from './EmptyFavoriteSearch';
import EmptySearch from '../../../components/common/EmptySearch';
import { Project } from '../types';
import { Query } from '../query';
import { OnboardingContext } from '../../../app/components/OnboardingContext';

interface Props {
  cardType?: string;
  currentUser: T.CurrentUser;
  isFavorite: boolean;
  isFiltered: boolean;
  organization: T.Organization | undefined;
  projects: Project[];
  query: Query;
}

export default class ProjectsList extends React.PureComponent<Props> {
  getCardHeight = () => {
    return this.props.cardType === 'leak' ? 159 : 143;
  };

  renderNoProjects() {
    const { currentUser, isFavorite, isFiltered, organization, query } = this.props;
    if (isFiltered) {
      return isFavorite ? <EmptyFavoriteSearch query={query} /> : <EmptySearch />;
    }
    return isFavorite ? (
      <OnboardingContext.Consumer>
        {openProjectOnboarding => (
          <NoFavoriteProjects openProjectOnboarding={openProjectOnboarding} />
        )}
      </OnboardingContext.Consumer>
    ) : (
      <OnboardingContext.Consumer>
        {openProjectOnboarding => (
          <EmptyInstance
            currentUser={currentUser}
            openProjectOnboarding={openProjectOnboarding}
            organization={organization}
          />
        )}
      </OnboardingContext.Consumer>
    );
  }

  renderRow = ({ index, key, style }: ListRowProps) => {
    const project = this.props.projects[index];
    const height = this.getCardHeight();
    return (
      <div key={key} style={{ ...style, height }}>
        <ProjectCard
          height={height}
          key={project.key}
          organization={this.props.organization}
          project={project}
          type={this.props.cardType}
        />
      </div>
    );
  };

  renderList() {
    const cardHeight = this.getCardHeight();
    return (
      <WindowScroller>
        {({ height, isScrolling, onChildScroll, scrollTop }) => (
          <AutoSizer disableHeight={true}>
            {({ width }) => (
              <div>
                <List
                  autoHeight={true}
                  height={height}
                  isScrolling={isScrolling}
                  onScroll={onChildScroll}
                  overscanRowCount={2}
                  rowCount={this.props.projects.length}
                  rowHeight={cardHeight + 20}
                  rowRenderer={this.renderRow}
                  scrollTop={scrollTop}
                  style={{ outline: 'none' }}
                  width={width}
                />
              </div>
            )}
          </AutoSizer>
        )}
      </WindowScroller>
    );
  }

  render() {
    const { projects } = this.props;

    return (
      <div className="projects-list">
        {projects.length > 0 ? this.renderList() : this.renderNoProjects()}
      </div>
    );
  }
}
