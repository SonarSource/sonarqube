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
// @flow
import React from 'react';
import classNames from 'classnames';
import SearchFilterContainer from '../filters/SearchFilterContainer';
import Tooltip from '../../../components/controls/Tooltip';
import PerspectiveSelect from './PerspectiveSelect';
import ProjectsSortingSelect from './ProjectsSortingSelect';
import { translate } from '../../../helpers/l10n';
import type { RawQuery } from '../../../helpers/query';

type Props = {|
  currentUser?: { isLoggedIn: boolean },
  isFavorite?: boolean,
  onPerspectiveChange: ({ view: string, visualization?: string }) => void,
  organization?: { key: string },
  projects: Array<*>,
  projectsAppState: { loading: boolean, total?: number },
  query: RawQuery,
  onSortChange: (sort: string, desc: boolean) => void,
  selectedSort: string,
  view: string,
  visualization?: string
|};

export default function PageHeader(props: Props) {
  const renderSortingSelect = () => {
    const { projectsAppState, projects, currentUser, view } = props;
    const limitReached =
      projects != null &&
      projectsAppState.total != null &&
      projects.length < projectsAppState.total;
    const defaultOption = currentUser && currentUser.isLoggedIn ? 'name' : 'analysis_date';
    if (view === 'visualizations' && !limitReached) {
      return (
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
      );
    }
    return (
      <ProjectsSortingSelect
        className="projects-topbar-item js-projects-sorting-select"
        defaultOption={defaultOption}
        onChange={props.onSortChange}
        selectedSort={props.selectedSort}
        view={props.view}
      />
    );
  };

  return (
    <header className="page-header projects-topbar-items">
      <PerspectiveSelect
        className="projects-topbar-item js-projects-perspective-select"
        onChange={props.onPerspectiveChange}
        view={props.view}
        visualization={props.visualization}
      />

      {renderSortingSelect()}

      <SearchFilterContainer
        className="projects-topbar-item projects-topbar-item-search"
        isFavorite={props.isFavorite}
        organization={props.organization}
        query={props.query}
      />

      <div
        className={classNames('projects-topbar-item', 'is-last', {
          'is-loading': props.projectsAppState.loading
        })}>
        {!!props.projectsAppState.loading && <i className="spinner spacer-right" />}

        {props.projectsAppState.total != null &&
          <span>
            <strong id="projects-total">{props.projectsAppState.total}</strong>
            {' '}
            {translate('projects._projects')}
          </span>}
      </div>
    </header>
  );
}
