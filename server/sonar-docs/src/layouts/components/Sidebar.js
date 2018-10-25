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
import CategoryLink from './CategoryLink';
import VersionSelect from './VersionSelect';
import Search from './Search';
import SearchEntryResult from './SearchEntryResult';
import NavigationTree from '../../../static/StaticNavigationTree.json';
import { ExternalLink } from './ExternalLink';
import DownloadIcon from './icons/DownloadIcon';

export default class Sidebar extends React.PureComponent {
  constructor(props) {
    super(props);
    this.state = {
      loaded: false,
      openBlockTitle: this.getOpenBlockFromLocation(this.props.location),
      query: '',
      results: [],
      versions: []
    };
  }

  componentDidMount() {
    this.loadVersions();
  }

  componentDidUpdate(prevProps) {
    if (this.props.location.pathname !== prevProps.location.pathname) {
      this.setState({ openBlockTitle: this.getOpenBlockFromLocation(this.props.location) });
    }
  }

  // A block is opened if the current page is set to one of his children
  getOpenBlockFromLocation({ pathname }) {
    const element = NavigationTree.find(
      item =>
        typeof item === 'object' &&
        item.children &&
        item.children.some(child => pathname.endsWith(child))
    );
    return element ? element.title : '';
  }

  loadVersions() {
    fetch('/DocsVersions.json').then(response =>
      response.json().then(json => {
        this.setState({ loaded: true, versions: json });
      })
    );
  }

  getNodeFromUrl = url => {
    return this.props.pages.find(p => p.fields.slug === url + '/' || p.frontmatter.url === url);
  };

  handleToggle = title => {
    this.setState(state => ({ openBlockTitle: state.openBlockTitle === title ? '' : title }));
  };

  renderCategories = tree => {
    const items = tree.map(item => {
      if (typeof item === 'object') {
        if (item.children) {
          return (
            <CategoryLink
              children={item.children.map(child => this.getNodeFromUrl(child))}
              key={item.title}
              location={this.props.location}
              onToggle={this.handleToggle}
              open={item.title === this.state.openBlockTitle}
              title={item.title}
            />
          );
        } else {
          return <ExternalLink external={item.url} key={item.title} title={item.title} />;
        }
      }
      return (
        <CategoryLink key={item} location={this.props.location} node={this.getNodeFromUrl(item)} />
      );
    });
    return <nav>{items}</nav>;
  };

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

  handleSearch = (results, query) => {
    this.setState({ results, query });
  };

  render() {
    const isOnCurrentVersion =
      this.props.version === 'latest' ||
      this.state.versions.find(v => v.value === this.props.version) !== undefined;
    return (
      <div className="page-sidebar">
        <div className="sidebar-header">
          <Link to="/">
            <img
              alt="Continuous Code Quality"
              css={{ verticalAlign: 'top', margin: 0 }}
              src={`/${this.props.version}/images/SonarQubeIcon.svg`}
              title="Continuous Code Quality"
              width="160"
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
          <Search
            navigation={NavigationTree}
            onResultsChange={this.handleSearch}
            pages={this.props.pages}
          />
          {this.state.query !== '' && this.renderResults()}
          {this.state.query === '' && this.renderCategories(NavigationTree)}
        </div>
        <div className="sidebar-footer">
          <a href="https://www.sonarqube.org/" rel="noopener noreferrer" target="_blank">
            <DownloadIcon /> SonarQube
          </a>
          <a href="https://community.sonarsource.com/" rel="noopener noreferrer" target="_blank">
            <img src={`/${this.props.version}/images/community.svg`} alt="Community" /> Community
          </a>
          <a
            className="icon-only"
            href="https://twitter.com/SonarQube"
            rel="noopener noreferrer"
            target="_blank">
            <img src={`/${this.props.version}/images/twitter.svg`} alt="Twitter" />
          </a>
          <a
            className="icon-only"
            href="https://www.sonarsource.com/resources/product-news/"
            rel="noopener noreferrer"
            target="_blank">
            <img src={`/${this.props.version}/images/newspaper.svg`} alt="Product News" />
            <span className="tooltip">Product News</span>
          </a>
        </div>
      </div>
    );
  }
}
