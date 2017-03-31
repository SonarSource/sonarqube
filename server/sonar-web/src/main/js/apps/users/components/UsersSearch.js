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
//@flow
import React from 'react';
import { debounce } from 'lodash';
import classNames from 'classnames';
import { translate, translateWithParameters } from '../../../helpers/l10n';

type Props = {
  onSearch: (query?: string) => void,
  className?: string
};

type State = {
  query?: string
};

export default class UsersSearch extends React.PureComponent {
  props: Props;
  state: State = {
    query: ''
  };

  constructor(props: Props) {
    super(props);
    this.handleSearch = debounce(this.handleSearch, 250);
  }

  handleSearch = (query: string) => {
    this.props.onSearch(query);
  };

  handleInputChange = ({ target }: { target: HTMLInputElement }) => {
    this.setState({ query: target.value });
    if (!target.value || target.value.length >= 2) {
      this.handleSearch(target.value);
    }
  };

  render() {
    const { query } = this.state;
    const searchBoxClass = classNames('search-box', this.props.className);
    const inputClassName = classNames('search-box-input', {
      touched: query != null && query.length === 1
    });
    return (
      <div className={searchBoxClass}>
        <button className="search-box-submit button-clean">
          <i className="icon-search" />
        </button>
        <input
          type="search"
          value={query}
          className={inputClassName}
          placeholder={translate('search_verb')}
          onChange={this.handleInputChange}
          autoComplete="off"
        />
        <span className="note spacer-left text-middle">
          {translateWithParameters('select2.tooShort', 2)}
        </span>
      </div>
    );
  }
}
