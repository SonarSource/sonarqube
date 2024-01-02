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
import * as React from 'react';
import HomePageSelect from '../../../components/controls/HomePageSelect';
import { translate } from '../../../helpers/l10n';
import { RawQuery } from '../../../types/types';
import { CurrentUser, isLoggedIn } from '../../../types/users';
import SearchFilterContainer from '../filters/SearchFilterContainer';
import ApplicationCreation from './ApplicationCreation';
import PerspectiveSelect from './PerspectiveSelect';
import ProjectCreationMenu from './ProjectCreationMenu';
import ProjectsSortingSelect from './ProjectsSortingSelect';

interface Props {
  currentUser: CurrentUser;
  loading: boolean;
  onPerspectiveChange: (x: { view: string }) => void;
  onQueryChange: (change: RawQuery) => void;
  onSortChange: (sort: string, desc: boolean) => void;
  query: RawQuery;
  selectedSort: string;
  total?: number;
  view: string;
}

export default function PageHeader(props: Props) {
  const { loading, total, currentUser, view } = props;
  const defaultOption = isLoggedIn(currentUser) ? 'name' : 'analysis_date';

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
            'is-loading': loading,
          })}
        >
          {total != null && (
            <span className="projects-total-label">
              <strong id="projects-total">{total}</strong> {translate('projects_')}
            </span>
          )}
        </div>

        <div className="display-flex-center">
          <PerspectiveSelect
            className="projects-topbar-item"
            onChange={props.onPerspectiveChange}
            view={view}
          />

          <div className={classNames('projects-topbar-item')}>
            <ProjectsSortingSelect
              defaultOption={defaultOption}
              onChange={props.onSortChange}
              selectedSort={props.selectedSort}
              view={view}
            />
          </div>
        </div>
      </div>
    </div>
  );
}
