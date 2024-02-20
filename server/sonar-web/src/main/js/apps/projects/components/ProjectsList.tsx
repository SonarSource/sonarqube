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

import classNames from 'classnames';
import { Spinner } from 'design-system';
import * as React from 'react';
import { AutoSizer } from 'react-virtualized/dist/commonjs/AutoSizer';
import { List, ListRowProps } from 'react-virtualized/dist/commonjs/List';
import EmptySearch from '../../../components/common/EmptySearch';
import ListFooter from '../../../components/controls/ListFooter';
import { translate } from '../../../helpers/l10n';
import { CurrentUser } from '../../../types/users';
import { Query } from '../query';
import { Project } from '../types';
import EmptyFavoriteSearch from './EmptyFavoriteSearch';
import EmptyInstance from './EmptyInstance';
import NoFavoriteProjects from './NoFavoriteProjects';
import ProjectCard from './project-card/ProjectCard';

const PROJECT_CARD_HEIGHT = 181;
const PROJECT_CARD_MARGIN = 20;
const PROJECT_LIST_FOOTER_HEIGHT = 90;

interface Props {
  cardType?: string;
  currentUser: CurrentUser;
  handleFavorite: (component: string, isFavorite: boolean) => void;
  isFavorite: boolean;
  isFiltered: boolean;
  loading: boolean;
  loadMore: () => void;
  projects: Project[];
  query: Query;
  total?: number;
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
    const { loading, projects, total } = this.props;
    if (index === projects.length) {
      return (
        <div key="footer" style={{ ...style }}>
          <ListFooter
            loadMoreAriaLabel={translate('projects.show_more')}
            count={projects !== undefined ? projects.length : 0}
            loadMore={this.props.loadMore}
            loading={loading}
            ready={!loading}
            total={total ?? 0}
          />
        </div>
      );
    }

    const project = projects[index];

    return (
      <div
        className={classNames({ 'sw-mt-4': index === 0 })}
        key={key}
        role="row"
        style={{ ...style, height: PROJECT_CARD_HEIGHT }}
      >
        <div className="sw-h-full" role="gridcell">
          <ProjectCard
            currentUser={this.props.currentUser}
            handleFavorite={this.props.handleFavorite}
            key={project.key}
            project={project}
            type={this.props.cardType}
          />
        </div>
      </div>
    );
  };

  renderList() {
    return this.props.loading ? (
      <Spinner />
    ) : (
      <AutoSizer>
        {({ height, width }) => (
          <List
            aria-label={translate('project_plural')}
            height={height}
            overscanRowCount={2}
            rowCount={this.props.projects.length + 1}
            rowHeight={({ index }) => {
              if (index === 0) {
                // first card, double top and bottom margin
                return PROJECT_CARD_HEIGHT + PROJECT_CARD_MARGIN * 2;
              }
              if (index === this.props.projects.length) {
                // Footer card, no margin
                return PROJECT_LIST_FOOTER_HEIGHT;
              }
              // all other cards, only bottom margin
              return PROJECT_CARD_HEIGHT + PROJECT_CARD_MARGIN;
            }}
            rowRenderer={this.renderRow}
            style={{ outline: 'none' }}
            tabIndex={-1}
            width={width}
          />
        )}
      </AutoSizer>
    );
  }

  render() {
    const { projects } = this.props;

    return projects.length > 0 ? this.renderList() : this.renderNoProjects();
  }
}
