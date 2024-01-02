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
import * as React from 'react';
import { AutoSizer } from 'react-virtualized/dist/commonjs/AutoSizer';
import { List, ListRowProps } from 'react-virtualized/dist/commonjs/List';
import { WindowScroller } from 'react-virtualized/dist/commonjs/WindowScroller';
import EmptySearch from '../../../components/common/EmptySearch';
import { translate } from '../../../helpers/l10n';
import { CurrentUser } from '../../../types/users';
import { Query } from '../query';
import { Project } from '../types';
import EmptyFavoriteSearch from './EmptyFavoriteSearch';
import EmptyInstance from './EmptyInstance';
import NoFavoriteProjects from './NoFavoriteProjects';
import ProjectCard from './project-card/ProjectCard';

const PROJECT_CARD_HEIGHT = 145;
const PROJECT_CARD_MARGIN = 20;

interface Props {
  cardType?: string;
  currentUser: CurrentUser;
  handleFavorite: (component: string, isFavorite: boolean) => void;
  isFavorite: boolean;
  isFiltered: boolean;
  projects: Project[];
  query: Query;
}

export default class ProjectsList extends React.PureComponent<Props> {
  renderNoProjects() {
    const { currentUser, isFavorite, isFiltered, query } = this.props;
    if (isFiltered) {
      return isFavorite ? <EmptyFavoriteSearch query={query} /> : <EmptySearch />;
    }
    return isFavorite ? <NoFavoriteProjects /> : <EmptyInstance currentUser={currentUser} />;
  }

  renderRow = ({ index, key, style }: ListRowProps) => {
    const project = this.props.projects[index];

    return (
      <div key={key} role="row" style={{ ...style, height: PROJECT_CARD_HEIGHT }}>
        <div role="gridcell">
          <ProjectCard
            currentUser={this.props.currentUser}
            handleFavorite={this.props.handleFavorite}
            height={PROJECT_CARD_HEIGHT}
            key={project.key}
            project={project}
            type={this.props.cardType}
          />
        </div>
      </div>
    );
  };

  renderList() {
    return (
      <WindowScroller>
        {({ height, isScrolling, onChildScroll, scrollTop }) => (
          <AutoSizer disableHeight={true}>
            {({ width }) => (
              <div>
                <List
                  aria-label={translate('project_plural')}
                  autoHeight={true}
                  height={height}
                  isScrolling={isScrolling}
                  onScroll={onChildScroll}
                  overscanRowCount={2}
                  rowCount={this.props.projects.length}
                  rowHeight={PROJECT_CARD_HEIGHT + PROJECT_CARD_MARGIN}
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
