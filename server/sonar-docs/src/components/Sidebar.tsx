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
import * as React from 'react';
import { Link } from 'gatsby';
import CategoryBlockLink from './CategoryBlockLink';
import ExternalLink from './ExternalLink';
import PageLink from './PageLink';
import Search from './Search';
import SearchEntryResult from './SearchEntryResult';
import VersionSelect from './VersionSelect';
import DownloadIcon from './icons/DownloadIcon';
import { getNavTree, isDocsNavigationBlock, isDocsNavigationExternalLink } from './navTreeUtils';
import { MarkdownRemark } from '../@types/graphql-types';
import { SearchResult, DocVersion, DocNavigationItem } from '../@types/types';

interface Props {
  location: Location;
  pages: MarkdownRemark[];
  version: string;
}

interface State {
  loaded: boolean;
  navTree: DocNavigationItem[];
  openBlockTitle: string;
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
      openBlockTitle: this.getOpenBlockFromLocation(this.props.location, navTree),
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
        openBlockTitle: this.getOpenBlockFromLocation(this.props.location, navTree)
      }));
    }
  }

  // A block is opened if the current page is set to one of his children
  getOpenBlockFromLocation({ pathname }: Location, navTree: DocNavigationItem[]) {
    const element = navTree.find(
      leave =>
        isDocsNavigationBlock(leave) && leave.children.some(child => pathname.endsWith(child))
    );
    return isDocsNavigationBlock(element) ? element.title : '';
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
    return this.props.pages.find(p =>
      Boolean(
        (p.fields && p.fields.slug === url + '/') || (p.frontmatter && p.frontmatter.url === url)
      )
    );
  };

  handleToggle = (title: string) => {
    this.setState(state => ({ openBlockTitle: state.openBlockTitle === title ? '' : title }));
  };

  handleSearch = (results: SearchResult[], query: string) => {
    this.setState({ results, query });
  };

  renderCategories = () => {
    return (
      <nav>
        {this.state.navTree.map(leave => {
          if (isDocsNavigationBlock(leave)) {
            return (
              <CategoryBlockLink
                key={leave.title}
                location={this.props.location}
                onToggle={this.handleToggle}
                open={leave.title === this.state.openBlockTitle}
                title={leave.title}>
                {
                  leave.children
                    .map(child => this.getNodeFromUrl(child))
                    .filter(Boolean) as MarkdownRemark[]
                }
              </CategoryBlockLink>
            );
          }

          if (isDocsNavigationExternalLink(leave)) {
            return <ExternalLink external={leave.url} key={leave.title} title={leave.title} />;
          }

          return (
            <PageLink
              className="page-indexes-link"
              key={leave}
              location={this.props.location}
              node={this.getNodeFromUrl(leave)}
            />
          );
        })}
      </nav>
    );
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
            href="https://www.sonarsource.com/resources/product-news/"
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
