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
import { debounce, identity } from 'lodash';
import { User, Group } from './ProfilePermissions';
import Select from '../../../components/controls/Select';
import Avatar from '../../../components/ui/Avatar';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import GroupIcon from '../../../components/icons-components/GroupIcon';

type Option = User | Group;
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

  handleInputChange = (query: string) => {
    this.setState({ query });
    if (query.length > 1) {
      this.handleSearch(query);
    }
  };

  render() {
    const noResultsText =
      this.state.query.length === 1
        ? translateWithParameters('select2.tooShort', 2)
        : translate('no_results');

    // create a uniq string both for users and groups
    const options = this.state.searchResults.map(r => ({ ...r, value: getStringValue(r) }));

    return (
      <Select
        autofocus={true}
        className="Select-big"
        clearable={false}
        isLoading={this.state.loading}
        // disable default react-select filtering
        filterOptions={identity}
        noResultsText={noResultsText}
        optionRenderer={optionRenderer}
        options={options}
        onChange={this.props.onChange}
        onInputChange={this.handleInputChange}
        placeholder=""
        searchable={true}
        value={this.props.selected && getStringValue(this.props.selected)}
        valueRenderer={optionRenderer}
      />
    );
  }
}

function isUser(option: Option): option is User {
  return (option as User).login !== undefined;
}

function getStringValue(option: Option) {
  return isUser(option) ? `user:${option.login}` : `group:${option.name}`;
}

function optionRenderer(option: OptionWithValue) {
  return isUser(option) ? (
    <div>
      <Avatar hash={option.avatar} name={option.name} size={16} />
      <strong className="spacer-left">{option.name}</strong>
      <span className="note little-spacer-left">{option.login}</span>
    </div>
  ) : (
    <div>
      <GroupIcon size={16} />
      <strong className="spacer-left">{option.name}</strong>
    </div>
  );
}
