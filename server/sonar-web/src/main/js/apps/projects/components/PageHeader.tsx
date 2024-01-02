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
import { InputSearch, LightLabel, LightPrimary } from 'design-system';
import * as React from 'react';
import HomePageSelect from '../../../components/controls/HomePageSelect';
import Tooltip from '../../../components/controls/Tooltip';
import { translate } from '../../../helpers/l10n';
import { RawQuery } from '../../../types/types';
import { CurrentUser, isLoggedIn } from '../../../types/users';
import ApplicationCreation from './ApplicationCreation';
import PerspectiveSelect from './PerspectiveSelect';
import ProjectCreationMenu from './ProjectCreationMenu';
import ProjectsSortingSelect from './ProjectsSortingSelect';

interface Props {
  currentUser: CurrentUser;
  onPerspectiveChange: (x: { view: string }) => void;
  onQueryChange: (change: RawQuery) => void;
  onSortChange: (sort: string, desc: boolean) => void;
  query: RawQuery;
  selectedSort: string;
  total?: number;
  view: string;
}

const MIN_SEARCH_QUERY_LENGTH = 2;

export default function PageHeader(props: Props) {
  const { query, total, currentUser, view } = props;
  const defaultOption = isLoggedIn(currentUser) ? 'name' : 'analysis_date';

  const handleSearch = (search?: string) => {
    props.onQueryChange({ search });
  };

  return (
    <div className="it__page-header sw-flex sw-flex-col">
      <div className="sw-flex sw-justify-end sw-mb-4">
        <ProjectCreationMenu />
        <ApplicationCreation className="sw-ml-2" />
      </div>
      <div className="sw-flex sw-justify-between">
        <div className="sw-flex sw-flex-1">
          <Tooltip overlay={translate('projects.search')}>
            <InputSearch
              className="sw-mr-4 it__page-header-search sw-max-w-abs-300 sw-flex-1"
              minLength={MIN_SEARCH_QUERY_LENGTH}
              onChange={handleSearch}
              size="auto"
              placeholder={translate('search.search_for_projects')}
              value={query.search ?? ''}
              searchInputAriaLabel={translate('search_verb')}
            />
          </Tooltip>
          <PerspectiveSelect onChange={props.onPerspectiveChange} view={view} />
          <ProjectsSortingSelect
            defaultOption={defaultOption}
            onChange={props.onSortChange}
            selectedSort={props.selectedSort}
            view={view}
          />
        </div>
        <div className="sw-flex sw-items-center">
          {total != null && (
            <>
              <LightPrimary id="projects-total" className="sw-body-sm-highlight sw-mr-1">
                {total}
              </LightPrimary>
              <LightLabel className="sw-body-sm">{translate('projects_')}</LightLabel>
            </>
          )}
          <HomePageSelect currentPage={{ type: 'PROJECTS' }} />
        </div>
      </div>
    </div>
  );
}
