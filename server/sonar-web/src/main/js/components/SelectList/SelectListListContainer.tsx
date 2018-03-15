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
import { Filter } from './SelectList';
import SelectListListElement from './SelectListListElement';

interface Props {
  elements: string[];
  filter: Filter;
  onSelect: (element: string) => void;
  onUnselect: (element: string) => void;
  renderElement: (element: string) => React.ReactNode;
  selectedElements: string[];
}

export default class SelectListListContainer extends React.PureComponent<Props> {
  handleSelectChange = (element: string) => {
    if (this.isSelected(element)) {
      this.props.onUnselect(element);
    } else {
      this.props.onSelect(element);
    }
  };

  isSelected = (element: string): boolean => {
    return this.props.selectedElements.includes(element);
  };

  render() {
    const { elements, filter } = this.props;
    const filteredElements = elements.filter(element => {
      if (filter === Filter.All) {
        return true;
      }
      const isSelected = this.isSelected(element);
      return filter === Filter.Selected ? isSelected : !isSelected;
    });

    return (
      <div className="select-list-list-container spacer-top">
        <ul className="menu">
          {filteredElements.map(element => (
            <SelectListListElement
              element={element}
              key={element}
              onSelectChange={this.handleSelectChange}
              renderElement={this.props.renderElement}
              selected={this.isSelected(element)}
            />
          ))}
        </ul>
      </div>
    );
  }
}
