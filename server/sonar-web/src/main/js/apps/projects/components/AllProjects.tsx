/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import { omitBy } from 'lodash';
import * as React from 'react';
import { Helmet } from 'react-helmet-async';
import A11ySkipTarget from '../../../app/components/a11y/A11ySkipTarget';
import withAppStateContext from '../../../app/components/app-state/withAppStateContext';
import Suggestions from '../../../app/components/embed-docs-modal/Suggestions';
import ScreenPositionHelper from '../../../components/common/ScreenPositionHelper';
import ListFooter from '../../../components/controls/ListFooter';
import { Location, Router, withRouter } from '../../../components/hoc/withRouter';
import '../../../components/search-navigator.css';
import DeferredSpinner from '../../../components/ui/DeferredSpinner';
import handleRequiredAuthentication from '../../../helpers/handleRequiredAuthentication';
import { translate } from '../../../helpers/l10n';
import { addSideBarClass, removeSideBarClass } from '../../../helpers/pages';
import { get, save } from '../../../helpers/storage';
import { isLoggedIn } from '../../../helpers/users';
import { ComponentQualifier } from '../../../types/component';
import { AppState, CurrentUser, RawQuery } from '../../../types/types';
import { hasFilterParams, hasViewParams, parseUrlQuery, Query } from '../query';
import '../styles.css';
import { Facets, Project } from '../types';
import { fetchProjects, parseSorting, SORTING_SWITCH } from '../utils';
import PageHeader from './PageHeader';
import PageSidebar from './PageSidebar';
import ProjectsList from './ProjectsList';

interface Props {
  currentUser: CurrentUser;
  isFavorite: boolean;
  location: Pick<Location, 'pathname' | 'query'>;
  appState: AppState;
  router: Pick<Router, 'push' | 'replace'>;
}

interface State {
  facets?: Facets;
  loading: boolean;
  pageIndex?: number;
  projects?: Project[];
  query: Query;
  total?: number;
}

export const LS_PROJECTS_SORT = 'sonarqube.projects.sort';
export const LS_PROJECTS_VIEW = 'sonarqube.projects.view';

export class AllProjects extends React.PureComponent<Props, State> {
  mounted = false;

  constructor(props: Props) {
    super(props);
    this.state = { loading: true, query: {} };
  }

  componentDidMount() {
    this.mounted = true;

    if (this.props.isFavorite && !isLoggedIn(this.props.currentUser)) {
      handleRequiredAuthentication();
      return;
    }
    this.handleQueryChange(true);
    addSideBarClass();
  }

  componentDidUpdate(prevProps: Props) {
    if (prevProps.location.query !== this.props.location.query) {
      this.handleQueryChange(false);
    }
  }

  componentWillUnmount() {
    this.mounted = false;
    removeSideBarClass();
  }

  fetchProjects = (query: Query) => {
    this.setState({ loading: true, query });
    fetchProjects(query, this.props.isFavorite).then(response => {
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
      fetchProjects(query, this.props.isFavorite, pageIndex + 1).then(response => {
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

  getSort = () => this.state.query.sort || 'name';

  getStorageOptions = () => {
    const options: {
      sort?: string;
      view?: string;
    } = {};
    if (get(LS_PROJECTS_SORT)) {
      options.sort = get(LS_PROJECTS_SORT) || undefined;
    }
    if (get(LS_PROJECTS_VIEW)) {
      options.view = get(LS_PROJECTS_VIEW) || undefined;
    }
    return options;
  };

  getView = () => this.state.query.view || 'overall';

  handleClearAll = () => {
    this.props.router.push({ pathname: this.props.location.pathname });
  };

  handleFavorite = (key: string, isFavorite: boolean) => {
    this.setState(({ projects }) => {
      if (!projects) {
        return null;
      }

      return {
        projects: projects.map(p => (p.key === key ? { ...p, isFavorite } : p))
      };
    });
  };

  handlePerspectiveChange = ({ view }: { view: string }) => {
    const query: {
      view: string | undefined;
      sort?: string | undefined;
    } = {
      view: view === 'overall' ? undefined : view
    };

    if (this.state.query.view === 'leak' || view === 'leak') {
      if (this.state.query.sort) {
        const sort = parseSorting(this.state.query.sort);
        if (SORTING_SWITCH[sort.sortValue]) {
          query.sort = (sort.sortDesc ? '-' : '') + SORTING_SWITCH[sort.sortValue];
        }
      }
      this.props.router.push({ pathname: this.props.location.pathname, query });
    } else {
      this.updateLocationQuery(query);
    }

    save(LS_PROJECTS_SORT, query.sort);
    save(LS_PROJECTS_VIEW, query.view);
  };

  handleQueryChange(initialMount: boolean) {
    const query = parseUrlQuery(this.props.location.query);
    const savedOptions = this.getStorageOptions();
    const savedOptionsSet = savedOptions.sort || savedOptions.view;

    if (initialMount && !hasViewParams(query) && savedOptionsSet) {
      this.props.router.replace({ pathname: this.props.location.pathname, query: savedOptions });
    } else {
      this.fetchProjects(query);
    }
  }

  handleSortChange = (sort: string, desc: boolean) => {
    const asString = (desc ? '-' : '') + sort;
    this.updateLocationQuery({ sort: asString });
    save(LS_PROJECTS_SORT, asString);
  };

  stopLoading = () => {
    if (this.mounted) {
      this.setState({ loading: false });
    }
  };

  updateLocationQuery = (newQuery: RawQuery) => {
    const query = omitBy({ ...this.props.location.query, ...newQuery }, x => !x);
    this.props.router.push({ pathname: this.props.location.pathname, query });
  };

  renderSide = () => (
    <ScreenPositionHelper className="layout-page-side-outer">
      {({ top }) => (
        <section
          aria-label={translate('filters')}
          className="layout-page-side projects-page-side"
          style={{ top }}>
          <div className="layout-page-side-inner">
            <div className="layout-page-filters">
              <A11ySkipTarget
                anchor="projects_filters"
                label={translate('projects.skip_to_filters')}
                weight={10}
              />

              <PageSidebar
                applicationsEnabled={this.props.appState.qualifiers.includes(
                  ComponentQualifier.Application
                )}
                facets={this.state.facets}
                onClearAll={this.handleClearAll}
                onQueryChange={this.updateLocationQuery}
                query={this.state.query}
                view={this.getView()}
              />
            </div>
          </div>
        </section>
      )}
    </ScreenPositionHelper>
  );

  renderHeader = () => (
    <div className="layout-page-header-panel layout-page-main-header">
      <div className="layout-page-header-panel-inner layout-page-main-header-inner">
        <div className="layout-page-main-inner">
          <PageHeader
            currentUser={this.props.currentUser}
            loading={this.state.loading}
            onPerspectiveChange={this.handlePerspectiveChange}
            onQueryChange={this.updateLocationQuery}
            onSortChange={this.handleSortChange}
            query={this.state.query}
            selectedSort={this.getSort()}
            total={this.state.total}
            view={this.getView()}
          />
        </div>
      </div>
    </div>
  );

  renderMain = () => {
    if (this.state.loading && this.state.projects === undefined) {
      return <DeferredSpinner />;
    }

    return (
      <div className="layout-page-main-inner">
        {this.state.projects && (
          <ProjectsList
            cardType={this.getView()}
            currentUser={this.props.currentUser}
            handleFavorite={this.handleFavorite}
            isFavorite={this.props.isFavorite}
            isFiltered={hasFilterParams(this.state.query)}
            projects={this.state.projects}
            query={this.state.query}
          />
        )}
        <ListFooter
          count={this.state.projects !== undefined ? this.state.projects.length : 0}
          loading={this.state.loading}
          loadMore={this.fetchMoreProjects}
          ready={!this.state.loading}
          total={this.state.total !== undefined ? this.state.total : 0}
        />
      </div>
    );
  };

  render() {
    return (
      <div className="layout-page projects-page" id="projects-page">
        <Suggestions suggestions="projects" />
        <Helmet defer={false} title={translate('projects.page')} />

        <h1 className="a11y-hidden">{translate('projects.page')}</h1>

        {this.renderSide()}

        <div className="layout-page-main">
          <A11ySkipTarget anchor="projects_main" />

          <div role="main">
            <h2 className="a11y-hidden">{translate('list_of_projects')}</h2>
            {this.renderHeader()}
            {this.renderMain()}
          </div>
        </div>
      </div>
    );
  }
}

export default withRouter(withAppStateContext(AllProjects));
