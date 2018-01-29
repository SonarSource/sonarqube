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
import OpenCloseIcon from '../icons-components/OpenCloseIcon';
import HelpIcon from '../icons-components/HelpIcon';
import Tooltip from '../controls/Tooltip';
import { translate, translateWithParameters } from '../../helpers/l10n';

interface Props {
  helper?: string;
  name: string;
  onClear?: () => void;
  onClick?: () => void;
  open: boolean;
  values?: string[];
}

export default class FacetHeader extends React.PureComponent<Props> {
  handleClearClick = (event: React.SyntheticEvent<HTMLButtonElement>) => {
    event.preventDefault();
    event.currentTarget.blur();
    if (this.props.onClear) {
      this.props.onClear();
    }
  };

  handleClick = (event: React.SyntheticEvent<HTMLAnchorElement>) => {
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
    const { values } = this.props;
    if (this.props.open || !values || !values.length) {
      return null;
    }
    const value =
      values.length === 1 ? values[0] : translateWithParameters('x_selected', values.length);
    return (
      <span className="badge badge-secondary is-rounded text-ellipsis" title={value}>
        {value}
      </span>
    );
  }

  render() {
    const showClearButton =
      this.props.values != null && this.props.values.length > 0 && this.props.onClear != null;

    return (
      <div className="search-navigator-facet-header-wrapper">
        {this.props.onClick ? (
          <span className="search-navigator-facet-header">
            <a href="#" onClick={this.handleClick}>
              <OpenCloseIcon className="little-spacer-right" open={this.props.open} />
              {this.props.name}
            </a>
            {this.renderHelper()}
          </span>
        ) : (
          <span className="search-navigator-facet-header">
            {this.props.name}
            {this.renderHelper()}
          </span>
        )}

        <span className="search-navigator-facet-header-value spacer-left spacer-right ">
          {this.renderValueIndicator()}
        </span>

        {showClearButton && (
          <button
            className="search-navigator-facet-header-button button-small button-red"
            onClick={this.handleClearClick}>
            {translate('clear')}
          </button>
        )}
      </div>
    );
  }
}
