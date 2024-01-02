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
import Select from '../../../components/controls/Select';
import { translate } from '../../../helpers/l10n';
import { ComponentQualifier } from '../../../types/component';
import { Component } from '../../../types/types';
import { APPLICATION_EVENT_TYPES, EVENT_TYPES, Query } from '../utils';
import ProjectActivityDateInput from './ProjectActivityDateInput';

interface ProjectActivityPageFiltersProps {
  category?: string;
  from?: Date;
  project: Pick<Component, 'qualifier'>;
  to?: Date;
  updateQuery: (changes: Partial<Query>) => void;
}

export default function ProjectActivityPageFilters(props: ProjectActivityPageFiltersProps) {
  const { project, category, from, to, updateQuery } = props;

  const isApp = project.qualifier === ComponentQualifier.Application;
  const eventTypes = isApp ? APPLICATION_EVENT_TYPES : EVENT_TYPES;
  const options = eventTypes.map((category) => ({
    label: translate('event.category', category),
    value: category,
  }));

  const handleCategoryChange = React.useCallback(
    (option: { value: string } | null) => {
      updateQuery({ category: option ? option.value : '' });
    },
    [updateQuery]
  );

  return (
    <div className="page-header display-flex-start">
      {!([ComponentQualifier.Portfolio, ComponentQualifier.SubPortfolio] as string[]).includes(
        project.qualifier
      ) && (
        <div className="display-flex-column big-spacer-right">
          <label className="text-bold little-spacer-bottom" htmlFor="filter-events">
            {translate('project_activity.filter_events')}
          </label>
          <Select
            className={isApp ? 'input-large' : 'input-medium'}
            id="filter-events"
            isClearable={true}
            isSearchable={false}
            onChange={handleCategoryChange}
            options={options}
            value={options.filter((o) => o.value === category)}
          />
        </div>
      )}
      <ProjectActivityDateInput from={from} onChange={props.updateQuery} to={to} />
    </div>
  );
}
