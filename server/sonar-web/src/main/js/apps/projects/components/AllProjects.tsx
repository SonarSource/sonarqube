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
import * as React from 'react';
import * as PropTypes from 'prop-types';
import Helmet from 'react-helmet';
import PageHeader from './PageHeader';
import ProjectsList from './ProjectsList';
import PageSidebar from './PageSidebar';
import Visualizations from '../visualizations/Visualizations';
import handleRequiredAuthentication from '../../../app/utils/handleRequiredAuthentication';
import ListFooter from '../../../components/controls/ListFooter';
import { translate } from '../../../helpers/l10n';
import * as storage from '../../../helpers/storage';
import { RawQuery } from '../../../helpers/query';
import '../styles.css';
import { Project, Facets } from '../types';
import { fetchProjects, parseSorting, SORTING_SWITCH } from '../utils';
import { parseUrlQuery, Query } from '../query';

interface Props {
  isFavorite: boolean;
  location: { pathname: string; query: { [x: string]: string } };
  organization?: { key: string };
}

interface State {
  facets?: Facets;
  loading: boolean;
  pageIndex?: number;
  projects?: Project[];
  query: Query;
  total?: number;
}

export default class AllProjects extends React.PureComponent<Props, State> {
  mounted: boolean;
  state: State = { loading: true, query: {} };

  static contextTypes = {
    currentUser: PropTypes.object.isRequired,
    organizationsEnabled: PropTypes.bool,
    router: PropTypes.object.isRequired
  };

  componentDidMount() {
    this.mounted = true;
    if (this.props.isFavorite && !this.context.currentUser.isLoggedIn) {
      handleRequiredAuthentication();
      return;
    }
    this.handleQueryChange(true);
    const footer = document.getElementById('footer');
    footer && footer.classList.add('page-footer-with-sidebar');
  }

  componentDidUpdate(prevProps: Props) {
    if (prevProps.location.query !== this.props.location.query) {
      this.handleQueryChange(false);
    }
  }

  componentWillUnmount() {
    this.mounted = false;
    const footer = document.getElementById('footer');
    footer && footer.classList.remove('page-footer-with-sidebar');
  }

  getView = () => this.state.query.view || 'overall';

  getVisualization = () => this.state.query.visualization || 'risk';

  getSort = () => this.state.query.sort || 'name';

  isFiltered = () => Object.values(this.state.query).some(value => value != undefined);

  stopLoading = () => {
    if (this.mounted) {
      this.setState({ loading: false });
    }
  };

  fetchProjects = (query: any) => {
    this.setState({ loading: true, query });
    fetchProjects(
      query,
      this.props.isFavorite,
      this.props.organization && this.props.organization.key
    ).then(response => {
      if (this.mounted) {
        this.setState({
          facets: response.facets,
          loading: false,
          pageIndex: 1,
          projects: response.projects,
          total: response.total
        });
      }
    }, this.stopLoading);
  };

  fetchMoreProjects = () => {
    const { pageIndex, projects, query } = this.state;
    if (pageIndex && projects && query) {
      this.setState({ loading: true });
      fetchProjects(
        query,
        this.props.isFavorite,
        this.props.organization && this.props.organization.key,
        pageIndex + 1
      ).then(response => {
        if (this.mounted) {
          this.setState({
            loading: false,
            pageIndex: pageIndex + 1,
            projects: [...projects, ...response.projects]
          });
        }
      }, this.stopLoading);
    }
  };

  getSavedOptions = () => {
    const options: {
      sort?: string;
      view?: string;
      visualization?: string;
    } = {};
    if (storage.getSort()) {
      options.sort = storage.getSort() || undefined;
    }
    if (storage.getView()) {
      options.view = storage.getView() || undefined;
    }
    if (storage.getVisualization()) {
      options.visualization = storage.getVisualization() || undefined;
    }
    return options;
  };

  handlePerspectiveChange = ({ view, visualization }: { view: string; visualization?: string }) => {
    const query: {
      view: string | undefined;
      visualization: string | undefined;
      sort?: string | undefined;
    } = {
      view: view === 'overall' ? undefined : view,
      visualization
    };

    if (this.state.query.view === 'leak' || view === 'leak') {
      if (this.state.query.sort) {
        const sort = parseSorting(this.state.query.sort);
        if (SORTING_SWITCH[sort.sortValue]) {
          query.sort = (sort.sortDesc ? '-' : '') + SORTING_SWITCH[sort.sortValue];
        }
      }
      this.context.router.push({ pathname: this.props.location.pathname, query });
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
      this.context.router.replace({ pathname: this.props.location.pathname, query: savedOptions });
    } else {
      this.fetchProjects(query);
    }
  }

  updateLocationQuery = (newQuery: RawQuery) => {
    this.context.router.push({
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
              facets={this.state.facets}
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
          <PageHeader
            currentUser={this.context.currentUser}
            isFavorite={this.props.isFavorite}
            loading={this.state.loading}
            onPerspectiveChange={this.handlePerspectiveChange}
            onSortChange={this.handleSortChange}
            organization={this.props.organization}
            projects={this.state.projects}
            query={this.state.query}
            selectedSort={this.getSort()}
            total={this.state.total}
            view={this.getView()}
            visualization={this.getVisualization()}
          />
        </div>
      </div>
    </div>
  );

  renderMain = () =>
    this.getView() === 'visualizations' ? (
      <div className="layout-page-main-inner">
        {this.state.projects && (
          <Visualizations
            displayOrganizations={!this.props.organization && !!this.context.organizationsEnabled}
            projects={this.state.projects}
            sort={this.state.query.sort}
            total={this.state.total}
            visualization={this.getVisualization()}
          />
        )}
      </div>
    ) : (
      <div className="layout-page-main-inner">
        {this.state.projects && (
          <ProjectsList
            cardType={this.getView()}
            isFavorite={this.props.isFavorite}
            isFiltered={this.isFiltered()}
            organization={this.props.organization}
            projects={this.state.projects}
            query={this.state.query}
          />
        )}
        <ListFooter
          count={this.state.projects != undefined ? this.state.projects.length : 0}
          loadMore={this.fetchMoreProjects}
          ready={!this.state.loading}
          total={this.state.total != undefined ? this.state.total : 0}
        />
      </div>
    );

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
