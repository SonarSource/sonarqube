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
import SelectListListContainer from './SelectListListContainer';
import { translate } from '../../helpers/l10n';
import SearchBox from '../controls/SearchBox';

interface Props {
  onSearch: (query: string, selected: string) => Promise<void>;
  onSelect: (id: number) => Promise<void>;
  onUnselect: (id: number) => Promise<void>;
  elements: Array<{ id: number; name: string; selected: boolean }>;
}

interface State {
  activeTab: number;
  query: string;
}

export default class SelectList extends React.PureComponent<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      activeTab: 0,
      query: ''
    };
  }

  getSelectedValueFromTabIndex(tabIndex: number) {
    if (tabIndex === 0) {
      return 'selected';
    }
    return tabIndex === 1 ? 'deselected' : 'all';
  }

  changeTab(tabIndex: number) {
    this.setState({ activeTab: tabIndex });
    this.props.onSearch(this.state.query, this.getSelectedValueFromTabIndex(tabIndex));
  }

  handleQueryChange = (query: string) => {
    this.setState({ query });
    this.props.onSearch(query, this.getSelectedValueFromTabIndex(this.state.activeTab));
  };

  handleSelect = (id: number) => {
    this.props.onSelect(id);
  };

  handleUnselect = (id: number) => {
    this.props.onUnselect(id);
  };

  render() {
    const { activeTab } = this.state;

    return (
      <div>
        <div className="button-group pull-left spacer-right">
          <button
            className={activeTab === 0 ? 'button-active' : undefined}
            disabled={this.state.query !== ''}
            onClick={() => this.changeTab(0)}
            type="button">
            {translate('quality_gates.projects.with')}
          </button>
          <button
            className={activeTab === 1 ? 'button-active' : undefined}
            disabled={this.state.query !== ''}
            onClick={() => this.changeTab(1)}
            type="button">
            {translate('quality_gates.projects.without')}
          </button>
          <button
            className={activeTab === 2 ? 'button-active' : undefined}
            disabled={this.state.query !== ''}
            onClick={() => this.changeTab(2)}
            type="button">
            {translate('quality_gates.projects.all')}
          </button>
        </div>
        <SearchBox
          autoFocus={true}
          loading={false}
          onChange={this.handleQueryChange}
          placeholder={translate('search_verb')}
          value={this.state.query}
        />
        <SelectListListContainer
          elements={this.props.elements}
          filter={this.getSelectedValueFromTabIndex(this.state.activeTab)}
          onSelect={this.handleSelect}
          onUnselect={this.handleUnselect}
        />
      </div>
    );
  }
}
