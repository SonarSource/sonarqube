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
import * as classNames from 'classnames';
import * as React from 'react';

interface Props {
  active?: boolean;
  custom?: boolean;
  disabled?: boolean;
  element: string;
  onHover: (elem: string) => void;
  onSelectChange: (elem: string, selected: boolean) => void;
  renderLabel: (element: string) => React.ReactNode;
  selected?: boolean;
}

export default class MultiSelectOption extends React.PureComponent<Props> {
  handleSelect = (evt: React.SyntheticEvent<HTMLAnchorElement>) => {
    evt.stopPropagation();
    evt.preventDefault();
    evt.currentTarget.blur();

    if (!this.props.disabled) {
      this.props.onSelectChange(this.props.element, !this.props.selected);
    }
  };

  handleHover = () => this.props.onHover(this.props.element);

  render() {
    const { selected, disabled } = this.props;
    const className = classNames('icon-checkbox', {
      'icon-checkbox-checked': selected,
      'icon-checkbox-invisible': disabled
    });
    const activeClass = classNames({ active: this.props.active, disabled });

    return (
      <li>
        <a
          className={activeClass}
          href="#"
          onClick={this.handleSelect}
          onFocus={this.handleHover}
          onMouseOver={this.handleHover}>
          <i className={className} /> {this.props.custom && '+ '}
          {this.props.renderLabel(this.props.element)}
        </a>
      </li>
    );
  }
}
