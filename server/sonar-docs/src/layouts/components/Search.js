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
import React, { Component } from 'react';
import lunr from 'lunr';
import ClearIcon from './icons/ClearIcon';
import { getUrlsList } from '../utils';

// Search component
export default class Search extends Component {
  index = null;
  input = null;

  constructor(props) {
    super(props);
    this.state = { value: '' };
    this.index = lunr(function() {
      this.ref('id');
      this.field('title', { boost: 10 });
      this.field('text');

      this.metadataWhitelist = ['position'];

      props.pages
        .filter(page =>
          getUrlsList(props.navigation).includes(page.frontmatter.url || page.fields.slug)
        )
        .forEach(page =>
          this.add({
            id: page.id,
            text: page.html.replace(/<(?:.|\n)*?>/gm, '').replace(/&#x3C;(?:.|\n)*?>/gm, ''),
            title: page.frontmatter.title
          })
        );
    });
  }

  getFormattedResults = (query, results) => {
    return results.map(match => {
      const page = this.props.pages.find(page => page.id === match.ref);
      const highlights = {};
      let longestTerm = '';

      // remember the longest term that matches the query *exactly*
      Object.keys(match.matchData.metadata).forEach(term => {
        if (query.toLowerCase().includes(term.toLowerCase()) && longestTerm.length < term.length) {
          longestTerm = term;
        }

        Object.keys(match.matchData.metadata[term]).forEach(fieldName => {
          const { position: positions } = match.matchData.metadata[term][fieldName];
          highlights[fieldName] = [...(highlights[fieldName] || []), ...positions];
        });
      });

      return {
        page: {
          id: page.id,
          text: page.html.replace(/<(?:.|\n)*?>/gm, '').replace(/&#x3C;(?:.|\n)*?>/gm, ''),
          title: page.frontmatter.title,
          url: page.frontmatter.url || page.fields.slug
        },
        highlights,
        longestTerm
      };
    });
  };

  handleClear = event => {
    this.setState({ value: '' });
    this.props.onResultsChange([], '');
    if (this.input) {
      this.input.focus();
    }
  };

  handleChange = event => {
    const { value } = event.currentTarget;
    this.setState({ value });
    if (value != '') {
      const results = this.getFormattedResults(value, this.index.search(`${value}~1 ${value}*`));
      this.props.onResultsChange(results, value);
    } else {
      this.props.onResultsChange([], value);
    }
  };

  render() {
    return (
      <div className="search-container">
        <input
          aria-label="Search"
          className="search-input"
          onChange={this.handleChange}
          placeholder="Search..."
          ref={node => (this.input = node)}
          type="search"
          value={this.state.value}
        />
        {this.state.value && (
          <button onClick={this.handleClear}>
            <ClearIcon size="8" />
          </button>
        )}
      </div>
    );
  }
}
