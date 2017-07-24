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
import React from 'react';
import PropTypes from 'prop-types';
import Controls from './controls';
import List from './list';
import Footer from './footer';

export default class Main extends React.PureComponent {
  static propTypes = {
    loadItems: PropTypes.func.isRequired,
    renderItem: PropTypes.func.isRequired,
    getItemKey: PropTypes.func.isRequired,
    selectItem: PropTypes.func.isRequired,
    deselectItem: PropTypes.func.isRequired
  };

  state = {
    items: [],
    total: 0,
    selection: 'selected',
    query: null
  };

  componentDidMount() {
    this.loadItems();
  }

  loadItems = () => {
    const options = {
      selection: this.state.selection,
      query: this.state.query,
      page: 1
    };
    this.props.loadItems(options, (items, paging) => {
      this.setState({ items, total: paging.total, page: paging.pageIndex });
    });
  };

  loadMoreItems = () => {
    const options = {
      selection: this.state.selection,
      query: this.state.query,
      page: this.state.page + 1
    };
    this.props.loadItems(options, (items, paging) => {
      const newItems = [].concat(this.state.items, items);
      this.setState({ items: newItems, total: paging.total, page: paging.pageIndex });
    });
  };

  loadSelected = () => {
    this.setState({ selection: 'selected', query: null }, this.loadItems);
  };

  loadDeselected = () => {
    this.setState({ selection: 'deselected', query: null }, this.loadItems);
  };

  loadAll = () => {
    this.setState({ selection: 'all', query: null }, this.loadItems);
  };

  search = query => {
    this.setState({ query }, this.loadItems);
  };

  render() {
    return (
      <div className="select-list-container">
        <Controls
          selection={this.state.selection}
          query={this.state.query}
          loadSelected={this.loadSelected}
          loadDeselected={this.loadDeselected}
          loadAll={this.loadAll}
          search={this.search}
        />

        <div className="select-list-wrapper">
          <List {...this.props} items={this.state.items} />
        </div>

        <Footer
          count={this.state.items.length}
          total={this.state.total}
          loadMore={this.loadMoreItems}
        />
      </div>
    );
  }
}
