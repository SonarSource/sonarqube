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
import * as React from 'react';
import DateInput from '../../../components/controls/DateInput';
import { toShortNotSoISOString } from '../../../helpers/dates';
import { translate } from '../../../helpers/l10n';

interface Props {
  fromDate?: string;
  toDate?: string;
  onFromDateChange: () => void;
  onReset: () => void;
  onToDateChange: () => void;
}

export default class ChangelogSearch extends React.PureComponent<Props> {
  handleResetClick = (event: React.SyntheticEvent<HTMLElement>) => {
    event.preventDefault();
    event.currentTarget.blur();
    this.props.onReset();
  };

  formatDate = (date?: string) => (date ? toShortNotSoISOString(date) : undefined);

  render() {
    return (
      <div className="display-inline-block" id="quality-profile-changelog-form">
        <DateInput
          maxDate={this.formatDate(this.props.toDate) || '+0'}
          name="since"
          onChange={this.props.onFromDateChange}
          placeholder={translate('from')}
          value={this.formatDate(this.props.fromDate)}
        />
        {' — '}
        <DateInput
          minDate={this.formatDate(this.props.fromDate)}
          name="to"
          onChange={this.props.onToDateChange}
          placeholder={translate('to')}
          value={this.formatDate(this.props.toDate)}
        />
        <button className="spacer-left" onClick={this.handleResetClick}>
          {translate('reset_verb')}
        </button>
      </div>
    );
  }
}
