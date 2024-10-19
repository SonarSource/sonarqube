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
import { Button } from '@sonarsource/echoes-react';
import { DateRangePicker, PopupZLevel } from 'design-system';
import * as React from 'react';
import { translate } from '../../../helpers/l10n';
import { Query } from '../utils';

interface Props {
  from?: Date;
  onChange: (changes: Partial<Query>) => void;
  to?: Date;
}

export default class ProjectActivityDateInput extends React.PureComponent<Props> {
  handleChange = ({ from, to }: { from?: Date; to?: Date }) => {
    this.props.onChange({ from, to });
  };

  handleResetClick = () => {
    this.props.onChange({ from: undefined, to: undefined });
  };

  render() {
    return (
      <div className="sw-flex">
        <DateRangePicker
          className="sw-w-abs-350"
          startClearButtonLabel={translate('clear.start')}
          endClearButtonLabel={translate('clear.end')}
          fromLabel={translate('start_date')}
          onChange={this.handleChange}
          separatorText={translate('to_')}
          toLabel={translate('end_date')}
          value={{ from: this.props.from, to: this.props.to }}
          zLevel={PopupZLevel.Content}
        />
        <Button
          className="sw-ml-2"
          isDisabled={this.props.from === undefined && this.props.to === undefined}
          onClick={this.handleResetClick}
        >
          {translate('project_activity.reset_dates')}
        </Button>
      </div>
    );
  }
}
