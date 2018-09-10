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
import lunr, { LunrIndex } from 'lunr';

// Search component
export default class Search extends Component {
  index = null;

  constructor(props) {
    super(props);
    this.index = lunr(function() {
      this.ref('id');
      this.field('title', { boost: 10 });
      this.field('text');

      this.metadataWhitelist = ['position'];

      props.pages.forEach(page =>
        this.add({
          id: page.id,
          title: page.frontmatter.title,
          text: page.html.replace(/<(?:.|\n)*?>/gm, '')
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
          slug: page.fields.slug,
          title: page.frontmatter.title,
          text: page.html.replace(/<(?:.|\n)*?>/gm, '')
        },
        highlights,
        longestTerm
      };
    });
  };

  handleChange = event => {
    const { value } = event.currentTarget;
    if (value != '') {
      const results = this.getFormattedResults(value, this.index.search(`${value}~1 ${value}*`));
      this.props.onResultsChange(results);
    } else {
      this.props.onResultsChange([]);
    }
  };

  render() {
    return (
      <input
        aria-label="Search"
        className="search-input"
        onChange={this.handleChange}
        placeholder="Search..."
        type="search"
      />
    );
  }
}
