/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import { debounce, identity } from 'lodash';
import * as React from 'react';
import SelectLegacy from '../../../components/controls/SelectLegacy';
import GroupIcon from '../../../components/icons/GroupIcon';
import Avatar from '../../../components/ui/Avatar';
import { translate } from '../../../helpers/l10n';
import { UserSelected } from '../../../types/types';
import { Group } from './ProfilePermissions';

type Option = UserSelected | Group;
type OptionWithValue = Option & { value: string };

interface Props {
  onChange: (option: OptionWithValue) => void;
  onSearch: (query: string) => Promise<Option[]>;
  selected?: Option;
}

interface State {
  loading: boolean;
  query: string;
  searchResults: Option[];
}

export default class ProfilePermissionsFormSelect extends React.PureComponent<Props, State> {
  mounted = false;

  constructor(props: Props) {
    super(props);
    this.handleSearch = debounce(this.handleSearch, 250);
    this.state = { loading: false, query: '', searchResults: [] };
  }

  componentDidMount() {
    this.mounted = true;
    this.handleSearch(this.state.query);
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  handleSearch = (query: string) => {
    this.setState({ loading: true });
    this.props.onSearch(query).then(
      searchResults => {
        if (this.mounted) {
          this.setState({ loading: false, searchResults });
        }
      },
      () => {
        if (this.mounted) {
          this.setState({ loading: false });
        }
      }
    );
  };

  handleInputChange = (newQuery: string) => {
    const { query } = this.state;
    if (query !== newQuery) {
      this.setState({ query: newQuery });
      this.handleSearch(newQuery);
    }
  };

  render() {
    const noResultsText = translate('no_results');

    // create a uniq string both for users and groups
    const options = this.state.searchResults.map(r => ({ ...r, value: getStringValue(r) }));

    return (
      <SelectLegacy
        autoFocus={true}
        className="Select-big"
        clearable={false}
        // disable default react-select filtering
        filterOptions={identity}
        isLoading={this.state.loading}
        noResultsText={noResultsText}
        onChange={this.props.onChange}
        onInputChange={this.handleInputChange}
        optionRenderer={optionRenderer}
        options={options}
        placeholder=""
        searchable={true}
        value={this.props.selected && getStringValue(this.props.selected)}
        valueRenderer={optionRenderer}
      />
    );
  }
}

function isUser(option: Option): option is UserSelected {
  return (option as UserSelected).login !== undefined;
}

function getStringValue(option: Option) {
  return isUser(option) ? `user:${option.login}` : `group:${option.name}`;
}

function optionRenderer(option: OptionWithValue) {
  return isUser(option) ? (
    <>
      <Avatar hash={option.avatar} name={option.name} size={16} />
      <strong className="spacer-left">{option.name}</strong>
      <span className="note little-spacer-left">{option.login}</span>
    </>
  ) : (
    <>
      <GroupIcon size={16} />
      <strong className="spacer-left">{option.name}</strong>
    </>
  );
}
