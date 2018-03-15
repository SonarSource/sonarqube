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
import SelectListListElement from './SelectListListElement';

interface Props {
  elements: Array<string | number>;
  filter: string;
  onSelect: (element: string | number) => void;
  onUnselect: (element: string | number) => void;
  renderElement: (element: string | number) => React.ReactNode;
  selectedElements: Array<string | number>;
}

export default class SelectListListContainer extends React.PureComponent<Props> {
  handleSelectChange = (element: string) => {
    if (this.isSelected(element)) {
      this.props.onUnselect(element);
    } else {
      this.props.onSelect(element);
    }
  };

  isSelected = (element: string | number): boolean => {
    return this.props.selectedElements.indexOf(element) > -1;
  };

  render() {
    const { elements, filter } = this.props;
    const filteredElements = elements.filter((element: string | number) => {
      if (filter === 'all') {
        return true;
      }
      const isSelected: boolean = this.isSelected(element);
      return filter === 'selected' ? isSelected : !isSelected;
    });

    return (
      <div className="select-list-list-container spacer-top">
        <ul className="menu">
          {filteredElements.map((element: string | number) => (
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
