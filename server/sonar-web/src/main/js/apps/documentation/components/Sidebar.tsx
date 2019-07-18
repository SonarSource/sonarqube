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
import { DocNavigationItem } from 'Docs/@types/types';
import * as React from 'react';
import SearchBox from 'sonar-ui-common/components/controls/SearchBox';
import { DocumentationEntry } from '../utils';
import Menu from './Menu';
import SearchResults from './SearchResults';

interface Props {
  navigation: DocNavigationItem[];
  pages: DocumentationEntry[];
  splat: string;
}

interface State {
  query: string;
}

export default class Sidebar extends React.PureComponent<Props, State> {
  state: State = { query: '' };

  handleSearch = (query: string) => {
    this.setState({ query: query.trim() });
  };

  render() {
    return (
      <>
        <SearchBox
          className="big-spacer-top spacer-bottom"
          minLength={2}
          onChange={this.handleSearch}
          placeholder="Search for pages or keywords"
          value={this.state.query}
        />
        <div className="documentation-results panel">
          <div className="list-group">
            {this.state.query ? (
              <SearchResults
                navigation={this.props.navigation}
                pages={this.props.pages}
                query={this.state.query}
                splat={this.props.splat}
              />
            ) : (
              <Menu
                navigation={this.props.navigation}
                pages={this.props.pages}
                splat={this.props.splat}
              />
            )}
          </div>
        </div>
      </>
    );
  }
}
