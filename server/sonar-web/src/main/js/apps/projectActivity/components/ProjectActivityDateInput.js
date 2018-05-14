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
import DateRangeInput from '../../../components/controls/DateRangeInput';
import { translate } from '../../../helpers/l10n';
import { Button } from '../../../components/ui/buttons';
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

  handleChange = ({ from, to } /*: { from?: Date, to?: Date } */) => {
    this.props.onChange({ from, to });
  };

  handleResetClick = () => {
    this.props.onChange({ from: null, to: null });
  };

  render() {
    return (
      <div>
        <DateRangeInput
          onChange={this.handleChange}
          value={{ from: this.props.from, to: this.props.to }}
        />
        <Button
          className="spacer-left"
          disabled={this.props.from == null && this.props.to == null}
          onClick={this.handleResetClick}>
          {translate('project_activity.reset_dates')}
        </Button>
      </div>
    );
  }
}
