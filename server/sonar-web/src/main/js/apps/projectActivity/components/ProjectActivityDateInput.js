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
import DateInput from '../../../components/controls/DateInput';
import { parseAsDate } from '../../../helpers/query';
import { translate } from '../../../helpers/l10n';
import { toShortNotSoISOString } from '../../../helpers/dates';
/*:: import type { RawQuery } from '../../../helpers/query'; */

/*::
type Props = {
  from: ?Date,
  to: ?Date,
  onChange: RawQuery => void
};
*/

export default class ProjectActivityDateInput extends React.PureComponent {
  /*:: props: Props; */

  handleFromDateChange = (from /*: string */) => this.props.onChange({ from: parseAsDate(from) });

  handleToDateChange = (to /*: string */) => this.props.onChange({ to: parseAsDate(to) });

  handleResetClick = () => this.props.onChange({ from: null, to: null });

  formatDate = (date /*: ?Date */) => (date ? toShortNotSoISOString(date) : undefined);

  render() {
    return (
      <div>
        <DateInput
          className="little-spacer-right"
          maxDate={this.formatDate(this.props.to) || '+0'}
          name="from"
          onChange={this.handleFromDateChange}
          placeholder={translate('from')}
          value={this.formatDate(this.props.from)}
        />
        {'—'}
        <DateInput
          className="little-spacer-left"
          minDate={this.formatDate(this.props.from)}
          name="to"
          onChange={this.handleToDateChange}
          placeholder={translate('to')}
          value={this.formatDate(this.props.to)}
        />
        <button
          className="spacer-left"
          onClick={this.handleResetClick}
          disabled={this.props.from == null && this.props.to == null}>
          {translate('project_activity.reset_dates')}
        </button>
      </div>
    );
  }
}
