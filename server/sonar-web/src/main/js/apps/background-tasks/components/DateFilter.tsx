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
import { translate } from '../../../helpers/l10n';

interface Props {
  maxExecutedAt: Date | undefined;
  minSubmittedAt: Date | undefined;
  onChange: (x: { maxExecutedAt?: Date; minSubmittedAt?: Date }) => void;
}

export default class DateFilter extends React.PureComponent<Props> {
  handleFromChange = (date: Date | undefined) => {
    this.props.onChange({ minSubmittedAt: date });
  };

  handleToChange = (date: Date | undefined) => {
    this.props.onChange({ maxExecutedAt: date });
  };

  render() {
    return (
      <div className="nowrap">
        <DateInput
          name="from"
          onChange={this.handleFromChange}
          placeholder={translate('from')}
          value={this.props.minSubmittedAt}
        />
        <DateInput
          className="little-spacer-left"
          name="to"
          onChange={this.handleToChange}
          placeholder={translate('to')}
          value={this.props.maxExecutedAt}
        />
      </div>
    );
  }
}
