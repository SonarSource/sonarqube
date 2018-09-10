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
import React from 'react';
import Link from 'gatsby-link';
import { fromPairs } from 'lodash';
import { sortNodes } from '../utils';
import CategoryLink from './CategoryLink';
import VersionSelect from './VersionSelect';
import Search from './Search';
import SearchEntryResult from './SearchEntryResult';

export default class Sidebar extends React.PureComponent {
  state = { loaded: false, results: [], versions: [] };

  componentDidMount() {
    this.loadVersions();
  }

  loadVersions() {
    fetch('/DocsVersions.json').then(response =>
      response.json().then(json => {
        this.setState({ loaded: true, versions: json });
      })
    );
  }

  getPagesHierarchy() {
    const categories = sortNodes(
      this.props.pages.filter(p => p.fields.slug.split('/').length === 3)
    );
    const pages = this.props.pages.filter(p => p.fields.slug.split('/').length > 3);
    const categoriesObject = fromPairs(categories.map(c => [c.fields.slug, { ...c, pages: [] }]));
    pages.forEach(page => {
      const parentSlug = page.fields.slug
        .split('/')
        .slice(0, 2)
        .join('/');
      categoriesObject[parentSlug + '/'].pages.push(page);
    });
    return categoriesObject;
  }

  renderResults = () => {
    return (
      <div>
        {this.state.results.map(result => (
          <SearchEntryResult
            active={
              (this.props.location.pathname === result.page.slug && result.page.slug === '/') ||
              (result.page.slug !== '/' && this.props.location.pathname.endsWith(result.page.slug))
            }
            key={result.page.id}
            result={result}
          />
        ))}
      </div>
    );
  };

  handleSearch = results => {
    this.setState({ results });
  };

  render() {
    const nodes = this.getPagesHierarchy();
    const isOnCurrentVersion =
      this.state.versions.find(v => v.value === this.props.version) !== undefined;
    return (
      <div className="page-sidebar">
        <div className="sidebar-header">
          <Link to="/">
            <img
              alt="Continuous Code Quality"
              css={{ verticalAlign: 'top', margin: 0 }}
              width="160"
              src="/images/SonarQubeIcon.svg"
              title="Continuous Code Quality"
            />
          </Link>
          <VersionSelect
            location={this.props.location}
            version={this.props.version}
            versions={this.state.versions}
          />

          {this.state.loaded &&
            !isOnCurrentVersion && (
              <div className="alert alert-warning">
                This is an archived version of the doc for{' '}
                <b>SonarQube version {this.props.version}</b>. <a href="/">See Documentation</a> for
                current functionnality.
              </div>
            )}
        </div>
        <div className="page-indexes">
          <Search pages={this.props.pages} onResultsChange={this.handleSearch} />
          {this.state.results.length > 0 && this.renderResults()}
          {this.state.results.length === 0 &&
            Object.keys(nodes).map(key => (
              <CategoryLink
                key={key}
                headers={this.props.headers}
                node={nodes[key]}
                location={this.props.location}
              />
            ))}
        </div>
      </div>
    );
  }
}
