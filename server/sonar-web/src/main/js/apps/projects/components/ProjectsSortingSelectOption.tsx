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
import * as classNames from 'classnames';

export interface Option {
  label: string;
  value: string;
  class?: string;
  short?: string;
}

interface Props {
  option: Option;
  children?: React.ReactNode;
  className?: string;
  isFocused?: boolean;
  onFocus: (option: Option, event: React.SyntheticEvent<HTMLElement>) => void;
  onSelect: (option: Option, event: React.SyntheticEvent<HTMLElement>) => void;
}

export default class ProjectsSortingSelectOption extends React.PureComponent<Props> {
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
        className={classNames(this.props.className, option.class)}
        onMouseDown={this.handleMouseDown}
        onMouseEnter={this.handleMouseEnter}
        onMouseMove={this.handleMouseMove}
        title={option.label}>
        {option.short ? option.short : this.props.children}
      </div>
    );
  }
}
