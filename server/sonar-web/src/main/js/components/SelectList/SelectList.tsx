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
import * as React from 'react';
import SelectListListContainer from './SelectListListContainer';
import { translate } from '../../helpers/l10n';
import SearchBox from '../controls/SearchBox';
import RadioToggle from '../controls/RadioToggle';
import './styles.css';

export enum Filter {
  All = 'all',
  Selected = 'selected',
  Unselected = 'deselected'
}

interface Props {
  allowBulkSelection?: boolean;
  elements: string[];
  disabledElements?: string[];
  labelSelected?: string;
  labelUnselected?: string;
  labelAll?: string;
  onSearch: (query: string, tab: Filter) => Promise<void>;
  onSelect: (element: string) => Promise<void>;
  onUnselect: (element: string) => Promise<void>;
  readOnly?: boolean;
  renderElement: (element: string) => React.ReactNode;
  selectedElements: string[];
}

interface State {
  filter: Filter;
  loading: boolean;
  query: string;
}

export default class SelectList extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { filter: Filter.Selected, loading: false, query: '' };

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  stopLoading = () => {
    if (this.mounted) {
      this.setState({ loading: false });
    }
  };

  changeFilter = (filter: Filter) => {
    this.setState({ filter, loading: true });
    this.props.onSearch(this.state.query, filter).then(this.stopLoading, this.stopLoading);
  };

  handleQueryChange = (query: string) => {
    this.setState({ loading: true, query });
    this.props.onSearch(query, this.getFilter()).then(this.stopLoading, this.stopLoading);
  };

  getFilter = () => {
    return this.state.query === '' ? this.state.filter : Filter.All;
  };

  render() {
    const {
      labelSelected = translate('selected'),
      labelUnselected = translate('unselected'),
      labelAll = translate('all')
    } = this.props;
    const { filter } = this.state;

    const disabled = this.state.query !== '';

    return (
      <div className="select-list">
        <div className="display-flex-center">
          <RadioToggle
            className="spacer-right"
            name="filter"
            onCheck={this.changeFilter}
            options={[
              { disabled, label: labelSelected, value: Filter.Selected },
              { disabled, label: labelUnselected, value: Filter.Unselected },
              { disabled, label: labelAll, value: Filter.All }
            ]}
            value={filter}
          />
          <SearchBox
            autoFocus={true}
            loading={this.state.loading}
            onChange={this.handleQueryChange}
            placeholder={translate('search_verb')}
            value={this.state.query}
          />
        </div>
        <SelectListListContainer
          allowBulkSelection={this.props.allowBulkSelection}
          disabledElements={this.props.disabledElements || []}
          elements={this.props.elements}
          filter={this.getFilter()}
          onSelect={this.props.onSelect}
          onUnselect={this.props.onUnselect}
          readOnly={this.props.readOnly}
          renderElement={this.props.renderElement}
          selectedElements={this.props.selectedElements}
        />
      </div>
    );
  }
}
