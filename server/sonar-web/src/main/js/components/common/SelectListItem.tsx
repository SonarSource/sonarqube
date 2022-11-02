/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import Tooltip from '../../components/controls/Tooltip';
import { ButtonPlain } from '../controls/buttons';

interface Props {
  active?: string;
  className?: string;
  item: string;
  onHover?: (item: string) => void;
  onSelect?: (item: string) => void;
  selected?: string;
  title?: React.ReactNode;
}

export default class SelectListItem extends React.PureComponent<Props> {
  handleSelect = () => {
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
        <ButtonPlain
          preventDefault={true}
          aria-selected={this.props.active === this.props.item}
          className={classNames(
            {
              active: this.props.active === this.props.item,
              hover: this.props.selected === this.props.item,
            },
            this.props.className
          )}
          onClick={this.handleSelect}
          onFocus={this.handleHover}
          onMouseOver={this.handleHover}
        >
          {children}
        </ButtonPlain>
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
