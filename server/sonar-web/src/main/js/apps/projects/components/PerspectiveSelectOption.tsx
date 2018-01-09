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
import BubblesIcon from '../../../components/icons-components/BubblesIcon';
import ListIcon from '../../../components/icons-components/ListIcon';

export interface Option {
  label: string;
  type: string;
  value: string;
}

interface Props {
  option: Option;
  children?: React.ReactNode;
  className?: string;
  isFocused?: boolean;
  onFocus: (option: Option, event: React.SyntheticEvent<HTMLElement>) => void;
  onSelect: (option: Option, event: React.SyntheticEvent<HTMLElement>) => void;
}

export default class PerspectiveSelectOption extends React.PureComponent<Props> {
  handleMouseDown = (event: React.SyntheticEvent<HTMLElement>) => {
    event.preventDefault();
    event.stopPropagation();
    this.props.onSelect(this.props.option, event);
  };

  handleMouseEnter = (event: React.SyntheticEvent<HTMLElement>) => {
    this.props.onFocus(this.props.option, event);
  };

  handleMouseMove = (event: React.SyntheticEvent<HTMLElement>) => {
    if (this.props.isFocused) {
      return;
    }
    this.props.onFocus(this.props.option, event);
  };

  render() {
    const { option } = this.props;
    return (
      <div
        className={this.props.className}
        onMouseDown={this.handleMouseDown}
        onMouseEnter={this.handleMouseEnter}
        onMouseMove={this.handleMouseMove}
        title={option.label}>
        <div>
          {option.type === 'view' && <ListIcon className="little-spacer-right" />}
          {option.type === 'visualization' && <BubblesIcon className="little-spacer-right" />}
          {this.props.children}
        </div>
      </div>
    );
  }
}
