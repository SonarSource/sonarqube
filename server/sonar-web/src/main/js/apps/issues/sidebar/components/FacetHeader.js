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

type Props = {
  hasValue: boolean,
  name: string,
  onClick?: () => void,
  open: boolean
};

export default class FacetHeader extends React.PureComponent {
  props: Props;

  static defaultProps = {
    hasValue: false,
    open: true
  };

  handleClick = (e: Event & { currentTarget: HTMLElement }) => {
    e.preventDefault();
    e.currentTarget.blur();
    if (this.props.onClick) {
      this.props.onClick();
    }
  };

  renderCheckbox() {
    return (
      <svg viewBox="0 0 1792 1792" width="10" height="10" style={{ paddingTop: 3 }}>
        {this.props.open
          ? <path
              style={{ fill: 'currentColor ' }}
              d="M1683 808l-742 741q-19 19-45 19t-45-19l-742-741q-19-19-19-45.5t19-45.5l166-165q19-19 45-19t45 19l531 531 531-531q19-19 45-19t45 19l166 165q19 19 19 45.5t-19 45.5z"
            />
          : <path
              style={{ fill: 'currentColor ' }}
              d="M1363 877l-742 742q-19 19-45 19t-45-19l-166-166q-19-19-19-45t19-45l531-531-531-531q-19-19-19-45t19-45l166-166q19-19 45-19t45 19l742 742q19 19 19 45t-19 45z"
            />}
      </svg>
    );
  }

  renderValueIndicator() {
    return this.props.hasValue && !this.props.open
      ? <svg viewBox="0 0 1792 1792" width="8" height="8" style={{ paddingTop: 5, paddingLeft: 8 }}>
          <path
            d="M1664 896q0 209-103 385.5t-279.5 279.5-385.5 103-385.5-103-279.5-279.5-103-385.5 103-385.5 279.5-279.5 385.5-103 385.5 103 279.5 279.5 103 385.5z"
            fill="#4b9fd5"
          />
        </svg>
      : null;
  }

  render() {
    return this.props.onClick
      ? <a className="search-navigator-facet-header" href="#" onClick={this.handleClick}>
          {this.renderCheckbox()}{' '}{this.props.name}{' '}{this.renderValueIndicator()}
        </a>
      : <span className="search-navigator-facet-header">
          {this.props.name}
        </span>;
  }
}
