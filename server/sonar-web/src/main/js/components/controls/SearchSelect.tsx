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
import { debounce } from 'lodash';
import Select, { Creatable } from './Select';
import { translate, translateWithParameters } from '../../helpers/l10n';

interface Props<T> {
  autofocus?: boolean;
  canCreate?: boolean;
  className?: string;
  clearable?: boolean;
  defaultOptions?: T[];
  minimumQueryLength?: number;
  multi?: boolean;
  onSearch: (query: string) => Promise<T[]>;
  onSelect?: (option: T) => void;
  onMultiSelect?: (options: T[]) => void;
  promptTextCreator?: (label: string) => string;
  renderOption?: (option: T) => JSX.Element;
  resetOnBlur?: boolean;
  value?: T | T[];
}

interface State<T> {
  loading: boolean;
  options: T[];
  query: string;
}

export default class SearchSelect<T extends { value: string }> extends React.PureComponent<
  Props<T>,
  State<T>
> {
  mounted = false;

  constructor(props: Props<T>) {
    super(props);
    this.state = { loading: false, options: props.defaultOptions || [], query: '' };
    this.handleSearch = debounce(this.handleSearch, 250);
  }

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  get autofocus() {
    return this.props.autofocus !== undefined ? this.props.autofocus : true;
  }

  get minimumQueryLength() {
    return this.props.minimumQueryLength !== undefined ? this.props.minimumQueryLength : 2;
  }

  get resetOnBlur() {
    return this.props.resetOnBlur !== undefined ? this.props.resetOnBlur : true;
  }

  handleSearch = (query: string) => {
    // Ignore the result if the query changed
    const currentQuery = query;
    this.props.onSearch(currentQuery).then(
      options => {
        if (this.mounted) {
          this.setState(state => ({
            loading: false,
            options: state.query === currentQuery ? options : state.options
          }));
        }
      },
      () => {
        if (this.mounted) {
          this.setState({ loading: false });
        }
      }
    );
  };

  handleChange = (option: T | T[]) => {
    if (Array.isArray(option)) {
      if (this.props.onMultiSelect) {
        this.props.onMultiSelect(option);
      }
    } else if (this.props.onSelect) {
      this.props.onSelect(option);
    }
  };

  handleInputChange = (query: string) => {
    if (query.length >= this.minimumQueryLength) {
      this.setState({ loading: true, query });
      this.handleSearch(query);
    } else {
      // `onInputChange` is called with an empty string after a user selects a value
      // in this case we shouldn't reset `options`, because it also resets select value :(
      const options = (query.length === 0 && this.props.defaultOptions) || [];
      this.setState({ options, query });
    }
  };

  // disable internal filtering
  handleFilterOption = () => true;

  render() {
    const Component = this.props.canCreate ? Creatable : Select;
    return (
      <Component
        autoFocus={this.autofocus}
        className={this.props.className}
        clearable={this.props.clearable}
        escapeClearsValue={false}
        filterOption={this.handleFilterOption}
        isLoading={this.state.loading}
        multi={this.props.multi}
        noResultsText={
          this.state.query.length < this.minimumQueryLength
            ? translateWithParameters('select2.tooShort', this.minimumQueryLength)
            : translate('select2.noMatches')
        }
        onBlurResetsInput={this.resetOnBlur}
        onChange={this.handleChange}
        onInputChange={this.handleInputChange}
        optionRenderer={this.props.renderOption}
        options={this.state.options}
        placeholder={translate('search_verb')}
        promptTextCreator={this.props.promptTextCreator}
        searchable={true}
        value={this.props.value}
        valueRenderer={this.props.renderOption}
      />
    );
  }
}
