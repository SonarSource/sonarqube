/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import SelectLegacy from '../../../components/controls/SelectLegacy';
import { translate } from '../../../helpers/l10n';
import { Component } from '../../../types/types';
import { APPLICATION_EVENT_TYPES, EVENT_TYPES, Query } from '../utils';
import ProjectActivityDateInput from './ProjectActivityDateInput';
import ProjectActivityEventSelectOption from './ProjectActivityEventSelectOption';

interface Props {
  category?: string;
  from?: Date;
  project: Pick<Component, 'qualifier'>;
  to?: Date;
  updateQuery: (changes: Partial<Query>) => void;
}

export default class ProjectActivityPageHeader extends React.PureComponent<Props> {
  handleCategoryChange = (option: { value: string } | null) =>
    this.props.updateQuery({ category: option ? option.value : '' });

  render() {
    const isApp = this.props.project.qualifier === 'APP';
    const eventTypes = isApp ? APPLICATION_EVENT_TYPES : EVENT_TYPES;
    const options = eventTypes.map(category => ({
      label: translate('event.category', category),
      value: category
    }));

    return (
      <header className="page-header">
        {!['VW', 'SVW'].includes(this.props.project.qualifier) && (
          <SelectLegacy
            className={classNames('pull-left big-spacer-right', {
              'input-medium': !isApp,
              'input-large': isApp
            })}
            clearable={true}
            onChange={this.handleCategoryChange}
            optionComponent={ProjectActivityEventSelectOption}
            options={options}
            placeholder={translate('project_activity.filter_events') + '...'}
            searchable={false}
            value={this.props.category}
          />
        )}
        <ProjectActivityDateInput
          from={this.props.from}
          onChange={this.props.updateQuery}
          to={this.props.to}
        />
      </header>
    );
  }
}
