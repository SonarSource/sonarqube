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
import { translate } from '../../../helpers/l10n';
import type { RawQuery } from '../../../helpers/query';

type Props = {
  updateQuery: RawQuery => void,
  category?: string
};

export default class ProjectActivityPageHeader extends React.PureComponent {
  props: Props;

  handleCategoryChange = (option: ?{ value: string }) => {
    this.props.updateQuery({ category: option ? option.value : '' });
  };

  render() {
    const selectOptions = ['VERSION', 'QUALITY_GATE', 'QUALITY_PROFILE', 'OTHER'].map(category => ({
      label: translate('event.category', category),
      value: category
    }));

    return (
      <header className="page-header">
        <div className="page-actions">
          <Select
            className="input-medium"
            placeholder={translate('filter_verb') + '...'}
            clearable={true}
            searchable={false}
            value={this.props.category}
            options={selectOptions}
            onChange={this.handleCategoryChange}
          />
        </div>

        <div className="page-description">
          {translate('project_activity.page.description')}
        </div>
      </header>
    );
  }
}
