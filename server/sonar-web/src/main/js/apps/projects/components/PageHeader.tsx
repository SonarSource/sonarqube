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
import * as classNames from 'classnames';
import * as React from 'react';
import Tooltip from 'sonar-ui-common/components/controls/Tooltip';
import { translate } from 'sonar-ui-common/helpers/l10n';
import HomePageSelect from '../../../components/controls/HomePageSelect';
import { isSonarCloud } from '../../../helpers/system';
import { isLoggedIn } from '../../../helpers/users';
import SearchFilterContainer from '../filters/SearchFilterContainer';
import { Project } from '../types';
import PerspectiveSelect from './PerspectiveSelect';
import ProjectsSortingSelect from './ProjectsSortingSelect';

interface Props {
  currentUser: T.CurrentUser;
  isFavorite: boolean;
  loading: boolean;
  onPerspectiveChange: (x: { view: string; visualization?: string }) => void;
  onQueryChange: (change: T.RawQuery) => void;
  onSortChange: (sort: string, desc: boolean) => void;
  organization?: { key: string };
  projects?: Project[];
  query: T.RawQuery;
  selectedSort: string;
  total?: number;
  view: string;
  visualization?: string;
}

export default function PageHeader(props: Props) {
  const { loading, total, projects, currentUser, view } = props;
  const limitReached = projects != null && total != null && projects.length < total;
  const defaultOption = isLoggedIn(currentUser) ? 'name' : 'analysis_date';
  const showHomePageSelect = !isSonarCloud() || props.isFavorite;

  return (
    <header className="page-header projects-topbar-items">
      <PerspectiveSelect
        className="projects-topbar-item js-projects-perspective-select"
        onChange={props.onPerspectiveChange}
        view={props.view}
        visualization={props.visualization}
      />

      {view === 'visualizations' && !limitReached ? (
        <Tooltip overlay={translate('projects.sort.disabled')}>
          <div className="projects-topbar-item disabled">
            <ProjectsSortingSelect
              className="js-projects-sorting-select"
              defaultOption={defaultOption}
              onChange={props.onSortChange}
              selectedSort={props.selectedSort}
              view={props.view}
            />
          </div>
        </Tooltip>
      ) : (
        <ProjectsSortingSelect
          className="projects-topbar-item js-projects-sorting-select"
          defaultOption={defaultOption}
          onChange={props.onSortChange}
          selectedSort={props.selectedSort}
          view={props.view}
        />
      )}

      <SearchFilterContainer
        onQueryChange={props.onQueryChange}
        organization={props.organization}
        query={props.query}
      />

      <div
        className={classNames('projects-topbar-item', 'is-last', {
          'is-loading': loading
        })}>
        {total != null && (
          <span>
            <strong id="projects-total">{total}</strong> {translate('projects._projects')}
          </span>
        )}
      </div>

      {showHomePageSelect && (
        <HomePageSelect
          className="huge-spacer-left"
          currentPage={isSonarCloud() ? { type: 'MY_PROJECTS' } : { type: 'PROJECTS' }}
        />
      )}
    </header>
  );
}
