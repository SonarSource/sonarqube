/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import ProjectActivityEventSelectOption from './ProjectActivityEventSelectOption';
import ProjectActivityEventSelectValue from './ProjectActivityEventSelectValue';
import ProjectActivityDateInput from './ProjectActivityDateInput';
import { EVENT_TYPES, APPLICATION_EVENT_TYPES } from '../utils';
import Select from '../../../components/controls/Select';
import { translate } from '../../../helpers/l10n';
/*:: import type { RawQuery } from '../../../helpers/query'; */

/*::
type Props = {
  category?: string,
  from: ?Date,
  project: { qualifier: string },
  to: ?Date,
  updateQuery: RawQuery => void
};
*/

export default class ProjectActivityPageHeader extends React.PureComponent {
  /*:: options: Array<{ label: string, value: string }>; */
  /*:: props: Props; */

  handleCategoryChange = (option /*: ?{ value: string } */) =>
    this.props.updateQuery({ category: option ? option.value : '' });

  render() {
    const eventTypes =
      this.props.project.qualifier === 'APP' ? APPLICATION_EVENT_TYPES : EVENT_TYPES;
    this.options = eventTypes.map(category => ({
      label: translate('event.category', category),
      value: category
    }));

    return (
      <header className="page-header">
        {!['VW', 'SVW'].includes(this.props.project.qualifier) && (
          <Select
            className="input-medium pull-left big-spacer-right"
            placeholder={translate('project_activity.filter_events') + '...'}
            clearable={true}
            searchable={false}
            value={this.props.category}
            optionComponent={ProjectActivityEventSelectOption}
            valueComponent={ProjectActivityEventSelectValue}
            options={this.options}
            onChange={this.handleCategoryChange}
          />
        )}
        <ProjectActivityDateInput
          className="pull-left"
          from={this.props.from}
          to={this.props.to}
          onChange={this.props.updateQuery}
        />
      </header>
    );
  }
}
