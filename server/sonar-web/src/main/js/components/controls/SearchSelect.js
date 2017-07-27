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
import Select from 'react-select';
import { debounce } from 'lodash';
import { translate, translateWithParameters } from '../../helpers/l10n';

type Option = { label: string, value: string };

type Props = {|
  autofocus: boolean,
  minimumQueryLength: number,
  onSearch: (query: string) => Promise<Array<Option>>,
  onSelect: (value: string) => void,
  renderOption?: (option: Object) => React.Element<*>,
  resetOnBlur: boolean,
  value?: string
|};

type State = {
  loading: boolean,
  options: Array<Option>,
  query: string
};

export default class SearchSelect extends React.PureComponent {
  mounted: boolean;
  props: Props;
  state: State;

  static defaultProps = {
    autofocus: true,
    minimumQueryLength: 2,
    resetOnBlur: true
  };

  constructor(props: Props) {
    super(props);
    this.state = { loading: false, options: [], query: '' };
    this.search = debounce(this.search, 250);
  }

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  search = (query: string) => {
    this.props.onSearch(query).then(options => {
      if (this.mounted) {
        this.setState({ loading: false, options });
      }
    });
  };

  handleBlur = () => {
    this.setState({ options: [], query: '' });
  };

  handleChange = (option: Option) => {
    this.props.onSelect(option.value);
  };

  handleInputChange = (query: string = '') => {
    if (query.length >= this.props.minimumQueryLength) {
      this.setState({ loading: true, query });
      this.search(query);
    } else {
      this.setState({ options: [], query });
    }
  };

  // disable internal filtering
  handleFilterOption = () => true;

  render() {
    return (
      <Select
        autofocus={this.props.autofocus}
        cache={false}
        className="input-super-large"
        clearable={false}
        filterOption={this.handleFilterOption}
        isLoading={this.state.loading}
        noResultsText={
          this.state.query.length < this.props.minimumQueryLength
            ? translateWithParameters('select2.tooShort', this.props.minimumQueryLength)
            : translate('select2.noMatches')
        }
        onBlur={this.props.resetOnBlur ? this.handleBlur : undefined}
        onChange={this.handleChange}
        onInputChange={this.handleInputChange}
        onOpen={this.props.minimumQueryLength === 0 ? this.handleInputChange : undefined}
        optionRenderer={this.props.renderOption}
        options={this.state.options}
        placeholder={translate('search_verb')}
        searchable={true}
        value={this.props.value}
        valueRenderer={this.props.renderOption}
      />
    );
  }
}
