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
import { translate } from '../../helpers/l10n';

interface Props {
  displayReset: boolean;
  onReset: () => void;
}

export default class FiltersHeader extends React.PureComponent<Props> {
  handleResetClick = (event: React.SyntheticEvent<HTMLButtonElement>) => {
    event.preventDefault();
    event.currentTarget.blur();
    this.props.onReset();
  };

  render() {
    return (
      <div className="search-navigator-filters-header">
        {this.props.displayReset && (
          <div className="pull-right">
            <button
              className="button-red"
              id="coding-rules-clear-all-filters"
              onClick={this.handleResetClick}>
              {translate('clear_all_filters')}
            </button>
          </div>
        )}

        <h3>{translate('filters')}</h3>
      </div>
    );
  }
}
