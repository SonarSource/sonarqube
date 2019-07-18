/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import { Button } from 'sonar-ui-common/components/controls/buttons';
import { translate } from 'sonar-ui-common/helpers/l10n';
import DateRangeInput from '../../../components/controls/DateRangeInput';

interface Props {
  dateRange: { from?: Date; to?: Date } | undefined;
  onDateRangeChange: (range: { from?: Date; to?: Date }) => void;
  onReset: () => void;
}

export default class ChangelogSearch extends React.PureComponent<Props> {
  render() {
    return (
      <div className="display-inline-block" id="quality-profile-changelog-form">
        <DateRangeInput onChange={this.props.onDateRangeChange} value={this.props.dateRange} />
        <Button className="spacer-left text-top" onClick={this.props.onReset}>
          {translate('reset_verb')}
        </Button>
      </div>
    );
  }
}
