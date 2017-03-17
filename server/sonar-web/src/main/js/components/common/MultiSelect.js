/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
// @flow
import React from 'react';
import difference from 'lodash/difference';
import MultiSelectOption from './MultiSelectOption';
import { translate } from '../../helpers/l10n';

type Props = {
  selectedElements: Array<string>,
  elements: Array<string>,
  onSearch: string => void,
  onSelect: string => void,
  onUnselect: string => void
};

type State = {
  query: string
};

export default class MultiSelect extends React.PureComponent {
  props: Props;
  state: State = {
    query: ''
  };

  handleSelectChange = (item: string, selected: boolean) => {
    if (selected) {
      this.props.onSelect(item);
    } else {
      this.props.onUnselect(item);
    }
  }

  handleSearchChange = ({ target }: { target: HTMLInputElement }) => {
    this.setState({ query: target.value });
    this.props.onSearch(target.value);
  };

  render() {
    const { selectedElements, elements } = this.props;
    const { query } = this.state;

    return (
      <div className="multi-select">
        <div className="search-box menu-search">
          <button className="search-box-submit button-clean">
            <i className="icon-search-new" />
          </button>
          <input
            type="search"
            value={query}
            className="search-box-input"
            placeholder={translate('search_verb')}
            onChange={this.handleSearchChange}
            autoComplete="off"
          />
        </div>
        <ul className="menu">
          {selectedElements.length > 0 &&
            selectedElements.map(element => (
              <MultiSelectOption
                key={element}
                element={element}
                selected={true}
                onSelectChange={this.handleSelectChange}
              />
            ))}
          {elements.length > 0 &&
            difference(elements, selectedElements).map(element => (
              <MultiSelectOption
                key={element}
                element={element}
                onSelectChange={this.handleSelectChange}
              />
            ))}
          {query &&
            selectedElements.indexOf(query) === -1 &&
            elements.indexOf(query) === -1 &&
            <MultiSelectOption
              key={query}
              element={query}
              custom={true}
              onSelectChange={this.handleSelectChange}
            />}
        </ul>
      </div>
    );
  }
}
