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
import classNames from 'classnames';
import * as React from 'react';
import Tooltip from '../../components/controls/Tooltip';
import { ButtonPlain } from '../controls/buttons';

interface Props {
  active?: string;
  className?: string;
  item: string;
  onSelect?: (item: string) => void;
  title?: React.ReactNode;
}

interface State {
  selected: boolean;
}

export default class SelectListItem extends React.PureComponent<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      selected: false,
    };
  }

  handleSelect = () => {
    if (this.props.onSelect) {
      this.props.onSelect(this.props.item);
    }
  };

  handleHover = () => {
    this.setState({ selected: true });
  };

  handleBlur = () => {
    this.setState({ selected: false });
  };

  renderLink() {
    const children = this.props.children || this.props.item;
    const { selected } = this.state;
    return (
      <li>
        <ButtonPlain
          preventDefault={true}
          aria-selected={this.props.active === this.props.item}
          className={classNames(
            {
              active: this.props.active === this.props.item,
              hover: selected,
            },
            this.props.className
          )}
          onClick={this.handleSelect}
          onFocus={this.handleHover}
          onBlur={this.handleBlur}
          onMouseOver={this.handleHover}
          onMouseLeave={this.handleBlur}
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
