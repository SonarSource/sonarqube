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
import SelectListListElement, { SelectListItem } from './SelectListListElement';

interface Props {
  elements: SelectListItem[];
  filter: string;
  onSelect: (id: number) => void;
  onUnselect: (id: number) => void;
}

interface State {}

export default class SelectListListContainer extends React.PureComponent<Props, State> {
  handleSelectChange = (element: SelectListItem) => {
    if (element.selected) {
      this.props.onUnselect(element.id);
    } else {
      this.props.onSelect(element.id);
    }
  };

  render() {
    const { elements, filter } = this.props;
    const filteredElements = elements.filter(element => {
      if (filter === 'all') {
        return true;
      }
      return filter === 'selected' ? element.selected : !element.selected;
    });

    return (
      <div className="select-list-list-container spacer-top">
        <ul className="menu">
          {filteredElements.map(element => (
            <SelectListListElement
              element={element}
              key={element.id}
              onSelectChange={this.handleSelectChange}
            />
          ))}
        </ul>
      </div>
    );
  }
}
