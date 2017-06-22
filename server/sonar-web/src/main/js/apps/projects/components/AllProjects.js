/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
//@flow
import React from 'react';
import Helmet from 'react-helmet';
import PageHeaderContainer from './PageHeaderContainer';
import ProjectsListContainer from './ProjectsListContainer';
import ProjectsListFooterContainer from './ProjectsListFooterContainer';
import PageSidebar from './PageSidebar';
import VisualizationsContainer from '../visualizations/VisualizationsContainer';
import { parseUrlQuery } from '../store/utils';
import { translate } from '../../../helpers/l10n';
import * as utils from '../utils';
import * as storage from '../../../helpers/storage';
import type { RawQuery } from '../../../helpers/query';
import '../styles.css';

type Props = {|
  isFavorite: boolean,
  location: { pathname: string, query: RawQuery },
  fetchProjects: (query: string, isFavorite: boolean, organization?: {}) => Promise<*>,
  organization?: { key: string },
  router: {
    push: ({ pathname: string, query?: {} }) => void,
    replace: ({ pathname: string, query?: {} }) => void
  },
  currentUser?: { isLoggedIn: boolean }
|};

type State = {
  query: RawQuery
};

export default class AllProjects extends React.PureComponent {
  props: Props;
  state: State = { query: {} };

  componentDidMount() {
    this.handleQueryChange(true);
    const footer = document.getElementById('footer');
    footer && footer.classList.add('search-navigator-footer');
  }

  componentDidUpdate(prevProps: Props) {
    if (prevProps.location.query !== this.props.location.query) {
      this.handleQueryChange(false);
    }
  }

  componentWillUnmount() {
    const footer = document.getElementById('footer');
    footer && footer.classList.remove('search-navigator-footer');
  }

  getView = () => this.state.query.view || 'overall';

  getVisualization = () => this.state.query.visualization || 'risk';

  getSort = () => this.state.query.sort || 'name';

  isFiltered = () => Object.keys(this.state.query).some(key => this.state.query[key] != null);

  getSavedOptions = () => {
    const options = {};
    if (storage.getSort()) {
      options.sort = storage.getSort();
    }
    if (storage.getView()) {
      options.view = storage.getView();
    }
    if (storage.getVisualization()) {
      options.visualization = storage.getVisualization();
    }
    return options;
  };

  handlePerspectiveChange = ({ view, visualization }: { view: string, visualization?: string }) => {
    const query: { view: ?string, visualization: ?string, sort?: ?string } = {
      view: view === 'overall' ? undefined : view,
      visualization
    };

    if (this.state.query.view === 'leak' || view === 'leak') {
      if (this.state.query.sort) {
        const sort = utils.parseSorting(this.state.query.sort);
        if (utils.SORTING_SWITCH[sort.sortValue]) {
          query.sort = (sort.sortDesc ? '-' : '') + utils.SORTING_SWITCH[sort.sortValue];
        }
      }
      this.props.router.push({ pathname: this.props.location.pathname, query });
    } else {
      this.updateLocationQuery(query);
    }

    storage.saveSort(query.sort);
    storage.saveView(query.view);
    storage.saveVisualization(visualization);
  };

  handleSortChange = (sort: string, desc: boolean) => {
    const asString = (desc ? '-' : '') + sort;
    this.updateLocationQuery({ sort: asString });
    storage.saveSort(asString);
  };

  handleQueryChange(initialMount: boolean) {
    const query = parseUrlQuery(this.props.location.query);
    const savedOptions = this.getSavedOptions();
    const savedOptionsSet = savedOptions.sort || savedOptions.view || savedOptions.visualization;

    // if there is no filter, but there are saved preferences in the localStorage
    if (initialMount && !this.isFiltered() && savedOptionsSet) {
      this.props.router.replace({ pathname: this.props.location.pathname, query: savedOptions });
    } else {
      this.setState({ query });
      this.props.fetchProjects(query, this.props.isFavorite, this.props.organization);
    }
  }

  updateLocationQuery = (newQuery: { [string]: ?string }) => {
    this.props.router.push({
      pathname: this.props.location.pathname,
      query: {
        ...this.props.location.query,
        ...newQuery
      }
    });
  };

  renderSide = () => (
    <div className="layout-page-side-outer">
      <div
        className="layout-page-side projects-page-side"
        style={{ top: this.props.organization ? 95 : 30 }}>
        <div className="layout-page-side-inner">
          <div className="layout-page-filters">
            <PageSidebar
              isFavorite={this.props.isFavorite}
              organization={this.props.organization}
              query={this.state.query}
              view={this.getView()}
              visualization={this.getVisualization()}
            />
          </div>
        </div>
      </div>
    </div>
  );

  renderHeader = () => (
    <div className="layout-page-header-panel layout-page-main-header">
      <div className="layout-page-header-panel-inner layout-page-main-header-inner">
        <div className="layout-page-main-inner">
          <PageHeaderContainer
            query={this.state.query}
            isFavorite={this.props.isFavorite}
            organization={this.props.organization}
            onPerspectiveChange={this.handlePerspectiveChange}
            onSortChange={this.handleSortChange}
            selectedSort={this.getSort()}
            currentUser={this.props.currentUser}
            view={this.getView()}
            visualization={this.getVisualization()}
          />
        </div>
      </div>
    </div>
  );

  renderMain = () =>
    (this.getView() === 'visualizations'
      ? <div className="layout-page-main-inner">
          <VisualizationsContainer
            sort={this.state.query.sort}
            visualization={this.getVisualization()}
          />
        </div>
      : <div className="layout-page-main-inner">
          <ProjectsListContainer
            isFavorite={this.props.isFavorite}
            isFiltered={this.isFiltered()}
            organization={this.props.organization}
            cardType={this.getView()}
          />
          <ProjectsListFooterContainer
            query={this.state.query}
            isFavorite={this.props.isFavorite}
            organization={this.props.organization}
          />
        </div>);

  render() {
    return (
      <div className="layout-page projects-page">
        <Helmet title={translate('projects.page')} />

        {this.renderSide()}

        <div className="layout-page-main projects-page-content">
          {this.renderHeader()}
          {this.renderMain()}
        </div>
      </div>
    );
  }
}
