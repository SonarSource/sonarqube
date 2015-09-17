import Backbone from 'backbone';
import React from 'react';
import SearchView from './search-view';

function contains (root, node) {
  while (node) {
    if (node === root) {
      return true;
    }
    node = node.parentNode;
  }
  return false;
}

export default React.createClass({
  getInitialState() {
    return { open: false };
  },

  componentDidMount() {
    key('s', () => {
      this.openSearch();
      return false;
    });
  },

  componentWillUnmount() {
    this.closeSearch();
    key.unbind('s');
  },

  openSearch() {
    window.addEventListener('click', this.onClickOutside);
    this.setState({ open: true }, this.renderSearchView);
  },

  closeSearch() {
    window.removeEventListener('click', this.onClickOutside);
    this.resetSearchView();
    this.setState({ open: false });
  },

  renderSearchView() {
    let searchContainer = React.findDOMNode(this.refs.container);
    this.searchView = new SearchView({
      model: new Backbone.Model(this.props),
      hide: this.closeSearch
    });
    this.searchView.render().$el.appendTo(searchContainer);
  },

  resetSearchView() {
    this.searchView && this.searchView.destroy();
  },

  onClick(e) {
    e.preventDefault();
    this.state.open ? this.closeSearch() : this.openSearch();
  },

  onClickOutside(e) {
    if (!contains(React.findDOMNode(this.refs.dropdown), e.target)) {
      this.closeSearch();
    }
  },

  render() {
    const dropdownClassName = 'dropdown' + (this.state.open ? ' open' : '');
    return (
        <li ref="dropdown" className={dropdownClassName}>
          <a className="navbar-search-dropdown" href="#" onClick={this.onClick}>
            <i className="icon-search navbar-icon"/>&nbsp;<i className="icon-dropdown"/>
          </a>
          <div ref="container" className="dropdown-menu dropdown-menu-right"></div>
        </li>
    );
  }
});
