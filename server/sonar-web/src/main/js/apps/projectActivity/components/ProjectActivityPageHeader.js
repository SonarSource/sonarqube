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
import Select from 'react-select';
import ProjectActivityEventSelectOption from './ProjectActivityEventSelectOption';
import ProjectActivityEventSelectValue from './ProjectActivityEventSelectValue';
import { EVENT_TYPES } from '../utils';
import { translate } from '../../../helpers/l10n';
import type { RawQuery } from '../../../helpers/query';

type Props = {
  updateQuery: RawQuery => void,
  category?: string
};

export default class ProjectActivityPageHeader extends React.PureComponent {
  options: Array<{ label: string, value: string }>;
  props: Props;

  constructor(props: Props) {
    super(props);
    this.options = EVENT_TYPES.map(category => ({
      label: translate('event.category', category),
      value: category
    }));
  }

  handleCategoryChange = (option: ?{ value: string }) => {
    this.props.updateQuery({ category: option ? option.value : '' });
  };

  render() {
    return (
      <header className="page-header">
        <Select
          className="input-medium"
          placeholder={translate('project_activity.filter_events') + '...'}
          clearable={true}
          searchable={false}
          value={this.props.category}
          optionComponent={ProjectActivityEventSelectOption}
          valueComponent={ProjectActivityEventSelectValue}
          options={this.options}
          onChange={this.handleCategoryChange}
        />
      </header>
    );
  }
}
