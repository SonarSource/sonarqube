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
/* eslint-disable max-len */
import React from 'react';
import OpenCloseIcon from '../icons-components/OpenCloseIcon';
import HelpIcon from '../icons-components/HelpIcon';
import Tooltip from '../controls/Tooltip';
import { translate } from '../../helpers/l10n';

/*::
type Props = {|
  helper?: string,
  name: string,
  onClear?: () => void,
  onClick?: () => void,
  open: boolean,
  values?: number
|};
*/

export default class FacetHeader extends React.PureComponent {
  /*:: props: Props; */

  static defaultProps = {
    open: true
  };

  handleClearClick = (event /*: Event & { currentTarget: HTMLElement } */) => {
    event.preventDefault();
    event.currentTarget.blur();
    if (this.props.onClear) {
      this.props.onClear();
    }
  };

  handleClick = (event /*: Event & { currentTarget: HTMLElement } */) => {
    event.preventDefault();
    event.currentTarget.blur();
    if (this.props.onClick) {
      this.props.onClick();
    }
  };

  renderHelper() {
    if (!this.props.helper) {
      return null;
    }
    return (
      <Tooltip overlay={this.props.helper} placement="right">
        <span>
          <HelpIcon className="spacer-left text-info" />
        </span>
      </Tooltip>
    );
  }

  renderValueIndicator() {
    if (this.props.open || !this.props.values) {
      return null;
    }
    return (
      <span className="spacer-left badge badge-secondary is-rounded">{this.props.values}</span>
    );
  }

  render() {
    const showClearButton /*: boolean */ = !!this.props.values && this.props.onClear != null;

    return (
      <div>
        {showClearButton && (
          <button
            className="search-navigator-facet-header-button button-small button-red"
            onClick={this.handleClearClick}>
            {translate('clear')}
          </button>
        )}

        {this.props.onClick ? (
          <span className="search-navigator-facet-header">
            <a href="#" onClick={this.handleClick}>
              <OpenCloseIcon className="little-spacer-right" open={this.props.open} />
              {this.props.name}
            </a>
            {this.renderHelper()}
            {this.renderValueIndicator()}
          </span>
        ) : (
          <span className="search-navigator-facet-header">
            {this.props.name}
            {this.renderHelper()}
          </span>
        )}
      </div>
    );
  }
}
