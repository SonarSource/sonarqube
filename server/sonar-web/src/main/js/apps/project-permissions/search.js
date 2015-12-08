import _ from 'underscore';
import React from 'react';

import { QualifierFilter } from './qualifier-filter';


export default React.createClass({
  propTypes: {
    search: React.PropTypes.func.isRequired
  },

  componentWillMount: function () {
    this.search = _.debounce(this.search, 250);
  },

  onSubmit(e) {
    e.preventDefault();
    this.search();
  },

  search() {
    let q = this.refs.input.value;
    this.props.search(q);
  },

  render() {
    if (this.props.componentId) {
      return null;
    }
    return (
        <div className="panel panel-vertical bordered-bottom spacer-bottom">

          {this.props.rootQualifiers.length > 1 && <QualifierFilter filter={this.props.filter}
                                                                    rootQualifiers={this.props.rootQualifiers}
                                                                    onFilter={this.props.onFilter}/>}

          <form onSubmit={this.onSubmit} className="search-box display-inline-block text-top">
            <button className="search-box-submit button-clean">
              <i className="icon-search"></i>
            </button>
            <input onChange={this.search}
                   ref="input"
                   className="search-box-input"
                   type="search"
                   placeholder="Search"/>
          </form>
        </div>
    );
  }
});
