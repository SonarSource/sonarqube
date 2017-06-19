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
import { translate, translateWithParameters } from '../../../helpers/l10n';

type Props = {
  className?: string,
  handleSearch: (userString?: string) => void,
  query: { search?: string }
};

type State = {
  userQuery?: string
};

export default class SearchFilter extends React.PureComponent {
  props: Props;
  state: State;

  constructor(props: Props) {
    super(props);
    this.state = {
      userQuery: props.query.search
    };
  }

  componentWillReceiveProps(nextProps: Props) {
    if (
      this.props.query.search === this.state.userQuery &&
      nextProps.query.search !== this.props.query.search
    ) {
      this.setState({
        userQuery: nextProps.query.search || ''
      });
    }
  }

  handleQueryChange = ({ target }: { target: HTMLInputElement }) => {
    this.setState({ userQuery: target.value });
    if (!target.value || target.value.length >= 2) {
      this.props.handleSearch(target.value);
    }
  };

  render() {
    const { userQuery } = this.state;
    const shortQuery = userQuery != null && userQuery.length === 1;
    return (
      <div className={this.props.className}>
        <input
          type="search"
          value={userQuery || ''}
          placeholder={translate('projects.search')}
          onChange={this.handleQueryChange}
          autoComplete="off"
        />
        {shortQuery &&
          <span className="note spacer-left">
            {translateWithParameters('select2.tooShort', 2)}
          </span>}
      </div>
    );
  }
}
