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

export interface SelectListItem {
  key: string | number;
  name: string;
  selected: boolean;
}

interface Props {
  element: SelectListItem;
  active?: boolean;
  onSelectChange: (element: SelectListItem) => void;
}

export default class SelectListListElement extends React.PureComponent<Props, {}> {
  handleSelect = (evt: React.SyntheticEvent<HTMLAnchorElement>) => {
    evt.stopPropagation();
    evt.preventDefault();
    evt.currentTarget.blur();
    this.props.onSelectChange(this.props.element);
  };

  render() {
    const linkClasses = classNames({ active: this.props.active });
    const checkboxClasses = classNames('spacer-right', 'icon-checkbox', {
      'icon-checkbox-checked': this.props.element.selected
    });

    return (
      <li>
        <a className={linkClasses} href="#" onClick={this.handleSelect}>
          <i className={checkboxClasses} />
          {this.props.element.name}
        </a>
      </li>
    );
  }
}
