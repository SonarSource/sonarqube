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
import classNames from 'classnames';
import Tooltip from '../controls/Tooltip';

/*::
type Props = {
  active?: string,
  children?: React.Element<*>,
  item: string,
  onSelect?: string => void,
  onHover?: string => void,
  title?: string
};
*/

export default class SelectListItem extends React.PureComponent {
  /*:: props: Props; */

  handleSelect = (evt /*: SyntheticInputEvent */) => {
    evt.preventDefault();
    if (this.props.onSelect) {
      this.props.onSelect(this.props.item);
    }
  };

  handleHover = () => {
    if (this.props.onHover) {
      this.props.onHover(this.props.item);
    }
  };

  renderLink() {
    let children = this.props.item;
    if (this.props.children) {
      children = this.props.children;
    }
    return (
      <li>
        <a
          href="#"
          className={classNames({ active: this.props.active === this.props.item })}
          onClick={this.handleSelect}
          onMouseOver={this.handleHover}
          onFocus={this.handleHover}>
          {children}
        </a>
      </li>
    );
  }

  render() {
    if (this.props.title) {
      return (
        <Tooltip placement="right" overlay={this.props.title}>
          {this.renderLink()}
        </Tooltip>
      );
    } else {
      return this.renderLink();
    }
  }
}
