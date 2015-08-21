import React from 'react';
import Controls from './controls';
import List from './list';
import Footer from './footer';

export default React.createClass({
  propTypes: {
    loadItems: React.PropTypes.func.isRequired,
    renderItem: React.PropTypes.func.isRequired,
    getItemKey: React.PropTypes.func.isRequired,
    selectItem: React.PropTypes.func.isRequired,
    deselectItem: React.PropTypes.func.isRequired
  },


  getInitialState() {
    return { items: [], total: 0, selection: 'selected', query: null };
  },

  componentDidMount() {
    this.loadItems();
  },

  loadItems() {
    let options = {
      selection: this.state.selection,
      query: this.state.query,
      page: 1
    };
    this.props.loadItems(options, (items, paging) => {
      this.setState({ items: items, total: paging.total, page: paging.pageIndex });
    });
  },

  loadMoreItems() {
    let options = {
      selection: this.state.selection,
      query: this.state.query,
      page: this.state.page + 1
    };
    this.props.loadItems(options, (items, paging) => {
      let newItems = [].concat(this.state.items, items);
      this.setState({ items: newItems, total: paging.total, page: paging.pageIndex });
    });
  },

  loadSelected() {
    this.setState({ selection: 'selected', query: null }, this.loadItems);
  },

  loadDeselected() {
    this.setState({ selection: 'deselected', query: null }, this.loadItems);
  },

  loadAll() {
    this.setState({ selection: 'all', query: null }, this.loadItems);
  },

  search(query) {
    this.setState({ query: query }, this.loadItems);
  },

  render() {
    return (
        <div className="select-list-container">
          <Controls
              selection={this.state.selection}
              query={this.state.query}
              loadSelected={this.loadSelected}
              loadDeselected={this.loadDeselected}
              loadAll={this.loadAll}
              search={this.search}/>

          <div className="select-list-wrapper">
            <List {...this.props} items={this.state.items}/>
          </div>

          <Footer count={this.state.items.length} total={this.state.total} loadMore={this.loadMoreItems}/>
        </div>
    );
  }
});
