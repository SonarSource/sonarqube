/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
