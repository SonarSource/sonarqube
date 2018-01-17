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
  element: string;
  selected?: boolean;
  custom?: boolean;
  active?: boolean;
  onSelectChange: (elem: string, selected: boolean) => void;
  onHover: (elem: string) => void;
}

export default class MultiSelectOption extends React.PureComponent<Props> {
  handleSelect = (evt: React.SyntheticEvent<HTMLAnchorElement>) => {
    evt.stopPropagation();
    evt.preventDefault();
    evt.currentTarget.blur();
    this.props.onSelectChange(this.props.element, !this.props.selected);
  };

  handleHover = () => this.props.onHover(this.props.element);

  render() {
    const className = classNames('icon-checkbox', {
      'icon-checkbox-checked': this.props.selected
    });
    const activeClass = classNames({ active: this.props.active });

    return (
      <li>
        <a
          href="#"
          className={activeClass}
          onClick={this.handleSelect}
          onMouseOver={this.handleHover}
          onFocus={this.handleHover}>
          <i className={className} /> {this.props.custom && '+ '}
          {this.props.element}
        </a>
      </li>
    );
  }
}
