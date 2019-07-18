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
import classNames from 'classnames';
import * as React from 'react';
import Tooltip from 'sonar-ui-common/components/controls/Tooltip';

interface Props {
  active?: string;
  item: string;
  onHover?: (item: string) => void;
  onSelect?: (item: string) => void;
  title?: string;
}

export default class SelectListItem extends React.PureComponent<Props> {
  handleSelect = (event: React.MouseEvent<HTMLAnchorElement>) => {
    event.preventDefault();
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
    const children = this.props.children || this.props.item;
    return (
      <li>
        <a
          className={classNames({ active: this.props.active === this.props.item })}
          href="#"
          onClick={this.handleSelect}
          onFocus={this.handleHover}
          onMouseOver={this.handleHover}>
          {children}
        </a>
      </li>
    );
  }

  render() {
    return (
      <Tooltip overlay={this.props.title || undefined} placement="right">
        {this.renderLink()}
      </Tooltip>
    );
  }
}
