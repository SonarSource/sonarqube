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

interface Props {
  checked: boolean;
  children?: React.ReactNode;
  className?: string;
  id?: string;
  onCheck: (checked: boolean, id?: string) => void;
  thirdState?: boolean;
}

export default class Checkbox extends React.PureComponent<Props> {
  static defaultProps = {
    thirdState: false
  };

  handleClick = (e: React.SyntheticEvent<HTMLElement>) => {
    e.preventDefault();
    e.currentTarget.blur();
    this.props.onCheck(!this.props.checked, this.props.id);
  };

  render() {
    const className = classNames('icon-checkbox', {
      'icon-checkbox-checked': this.props.checked,
      'icon-checkbox-single': this.props.thirdState
    });

    if (this.props.children) {
      return (
        <a
          id={this.props.id}
          className={classNames('link-checkbox', this.props.className)}
          href="#"
          onClick={this.handleClick}>
          <i className={className} />
          {this.props.children}
        </a>
      );
    }

    return (
      <a
        id={this.props.id}
        className={classNames(className, this.props.className)}
        href="#"
        onClick={this.handleClick}
      />
    );
  }
}
