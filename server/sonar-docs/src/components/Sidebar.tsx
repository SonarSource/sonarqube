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
import { Link } from 'gatsby';
import * as React from 'react';
import { MarkdownRemark } from '../@types/graphql-types';
import { DocNavigationItem, DocVersion, SearchResult } from '../@types/types';
import CategoryBlockLink from './CategoryBlockLink';
import ExternalLink from './ExternalLink';
import DownloadIcon from './icons/DownloadIcon';
import {
  getNavTree,
  getOpenChainFromPath,
  isDocsNavigationBlock,
  isDocsNavigationExternalLink,
  testPathAgainstUrl
} from './navTreeUtils';
import PageLink from './PageLink';
import Search from './Search';
import SearchEntryResult from './SearchEntryResult';
import VersionSelect from './VersionSelect';

interface Props {
  location: Location;
  pages: MarkdownRemark[];
  version: string;
}

interface State {
  loaded: boolean;
  navTree: DocNavigationItem[];
  openChain?: DocNavigationItem[];
  query: string;
  results: SearchResult[];
  versions: DocVersion[];
}

export default class Sidebar extends React.PureComponent<Props, State> {
  constructor(props: Props) {
    super(props);
    const navTree = getNavTree();
    this.state = {
      loaded: false,
      navTree,
      openChain: getOpenChainFromPath(this.props.location.pathname, navTree),
      query: '',
      results: [],
      versions: []
    };
  }

  componentDidMount() {
    this.loadVersions();
  }

  componentDidUpdate(prevProps: Props) {
    if (this.props.location.pathname !== prevProps.location.pathname) {
      this.setState(({ navTree }) => ({
        openChain: getOpenChainFromPath(this.props.location.pathname, navTree)
      }));
    }
  }

  loadVersions() {
    fetch('/DocsVersions.json')
      .then(response => response.json())
      .then(json => {
        this.setState({ loaded: true, versions: json });
      })
      .catch(() => {});
  }

  getNodeFromUrl = (url: string) => {
    return this.props.pages.find(p => {
      if (p.fields && p.fields.slug && testPathAgainstUrl(p.fields.slug, url)) {
        return true;
      }

      if (p.frontmatter && p.frontmatter.url && testPathAgainstUrl(p.frontmatter.url, url)) {
        return true;
      }

      return false;
    });
  };

  handleSearch = (results: SearchResult[], query: string) => {
    this.setState({ results, query });
  };

  renderCategory = (leaf: DocNavigationItem) => {
    if (isDocsNavigationBlock(leaf)) {
      let children: (MarkdownRemark | JSX.Element)[] = [];
      leaf.children.forEach(child => {
        if (typeof child === 'string') {
          const node = this.getNodeFromUrl(child);
          if (node) {
            children.push(node);
          }
        } else {
          children = children.concat(this.renderCategory(child));
        }
      });
      return (
        <CategoryBlockLink
          key={leaf.title}
          location={this.props.location}
          openByDefault={this.state.openChain && this.state.openChain.includes(leaf)}
          title={leaf.title}>
          {children}
        </CategoryBlockLink>
      );
    }

    if (isDocsNavigationExternalLink(leaf)) {
      return <ExternalLink external={leaf.url} key={leaf.title} title={leaf.title} />;
    }

    return (
      <PageLink
        className="page-indexes-link"
        key={leaf}
        location={this.props.location}
        node={this.getNodeFromUrl(leaf)}
      />
    );
  };

  renderCategories = () => {
    return <nav>{this.state.navTree.map(this.renderCategory)}</nav>;
  };

  renderResults = () => {
    return (
      <div>
        {this.state.results.map(result => (
          <SearchEntryResult
            active={
              (this.props.location.pathname === result.page.url && result.page.url === '/') ||
              (result.page.url !== '/' && this.props.location.pathname.endsWith(result.page.url))
            }
            key={result.page.id}
            result={result}
          />
        ))}
      </div>
    );
  };

  render() {
    const { versions } = this.state;
    const currentVersion = versions.find(v => v.current);
    const selectedVersionValue =
      currentVersion && this.props.version === 'latest' ? currentVersion.value : this.props.version;
    const isOnCurrentVersion = !currentVersion || selectedVersionValue === currentVersion.value;
    return (
      <div className="page-sidebar">
        <div className="sidebar-header">
          <Link to="/">
            <img
              alt="Continuous Code Quality"
              className="sidebar-logo"
              src={`/${this.props.version}/images/SonarQubeIcon.svg`}
              title="Continuous Code Quality"
              width="160"
            />
          </Link>
          <VersionSelect
            isOnCurrentVersion={isOnCurrentVersion}
            selectedVersionValue={selectedVersionValue}
            versions={versions}
          />
          {this.state.loaded && !isOnCurrentVersion && (
            <div className="alert alert-warning">
              This is an archived version of the doc for{' '}
              <b>SonarQube version {this.props.version}</b>. <a href="/">See Documentation</a> for
              current functionnality.
            </div>
          )}
        </div>
        <div className="page-indexes">
          <Search
            navigation={this.state.navTree}
            onResultsChange={this.handleSearch}
            pages={this.props.pages}
          />
          {this.state.query !== '' ? this.renderResults() : this.renderCategories()}
        </div>
        <div className="sidebar-footer">
          <a href="https://www.sonarqube.org/" rel="noopener noreferrer" target="_blank">
            <DownloadIcon /> SonarQube
          </a>
          <a href="https://community.sonarsource.com/" rel="noopener noreferrer" target="_blank">
            <img alt="Community" src={`/${this.props.version}/images/community.svg`} /> Community
          </a>
          <a
            className="icon-only"
            href="https://twitter.com/SonarQube"
            rel="noopener noreferrer"
            target="_blank">
            <img alt="Twitter" src={`/${this.props.version}/images/twitter.svg`} />
          </a>
          <a
            className="icon-only"
            href="https://www.sonarqube.org/whats-new/"
            rel="noopener noreferrer"
            target="_blank">
            <img alt="Product News" src={`/${this.props.version}/images/newspaper.svg`} />
            <span className="tooltip">Product News</span>
          </a>
        </div>
      </div>
    );
  }
}
