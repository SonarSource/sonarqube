/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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
import HomePageSelect from '../../../components/controls/HomePageSelect';
import Tooltip from '../../../components/controls/Tooltip';
import { translate } from '../../../helpers/l10n';
import { isLoggedIn } from '../../../helpers/users';
import SearchFilterContainer from '../filters/SearchFilterContainer';
import { Project } from '../types';
import ApplicationCreation from './ApplicationCreation';
import PerspectiveSelect from './PerspectiveSelect';
import ProjectCreationMenu from './ProjectCreationMenu';
import ProjectsSortingSelect from './ProjectsSortingSelect';

interface Props {
  currentUser: T.CurrentUser;
  loading: boolean;
  onPerspectiveChange: (x: { view: string; visualization?: string }) => void;
  onQueryChange: (change: T.RawQuery) => void;
  onSortChange: (sort: string, desc: boolean) => void;
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

  const sortingDisabled = view === 'visualizations' && !limitReached;

  return (
    <div className="page-header">
      <div className="display-flex-space-between spacer-top">
        <SearchFilterContainer onQueryChange={props.onQueryChange} query={props.query} />
        <div className="display-flex-center">
          <ProjectCreationMenu className="little-spacer-right" />
          <ApplicationCreation className="little-spacer-right" />
          <HomePageSelect
            className="spacer-left little-spacer-right"
            currentPage={{ type: 'PROJECTS' }}
          />
        </div>
      </div>
      <div className="big-spacer-top display-flex-space-between">
        <div
          className={classNames('display-flex-center', {
            'is-loading': loading
          })}>
          {total != null && (
            <span className="projects-total-label">
              <strong id="projects-total">{total}</strong> {translate('projects._projects')}
            </span>
          )}
        </div>

        <div className="display-flex-center">
          <PerspectiveSelect
            className="projects-topbar-item js-projects-perspective-select"
            onChange={props.onPerspectiveChange}
            view={props.view}
            visualization={props.visualization}
          />

          <Tooltip overlay={sortingDisabled ? translate('projects.sort.disabled') : undefined}>
            <div className={classNames('projects-topbar-item', { disabled: sortingDisabled })}>
              <ProjectsSortingSelect
                className="js-projects-sorting-select"
                defaultOption={defaultOption}
                onChange={props.onSortChange}
                selectedSort={props.selectedSort}
                view={props.view}
              />
            </div>
          </Tooltip>
        </div>
      </div>
    </div>
  );
}
