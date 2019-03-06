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
import Helmet from 'react-helmet';
import { omitBy } from 'lodash';
import PageHeader from './PageHeader';
import ProjectsList from './ProjectsList';
import PageSidebar from './PageSidebar';
import handleRequiredAuthentication from '../../../app/utils/handleRequiredAuthentication';
import A11ySkipTarget from '../../../app/components/a11y/A11ySkipTarget';
import DeferredSpinner from '../../../components/common/DeferredSpinner';
import ListFooter from '../../../components/controls/ListFooter';
import OrganizationEmpty from '../../organizations/components/OrganizationEmpty';
import ScreenPositionHelper from '../../../components/common/ScreenPositionHelper';
import Suggestions from '../../../app/components/embed-docs-modal/Suggestions';
import Visualizations from '../visualizations/Visualizations';
import { Project, Facets } from '../types';
import { fetchProjects, parseSorting, SORTING_SWITCH } from '../utils';
import { parseUrlQuery, Query, hasFilterParams, hasVisualizationParams } from '../query';
import { translate } from '../../../helpers/l10n';
import { addSideBarClass, removeSideBarClass } from '../../../helpers/pages';
import { RawQuery } from '../../../helpers/query';
import { get, save } from '../../../helpers/storage';
import { isSonarCloud } from '../../../helpers/system';
import { isLoggedIn } from '../../../helpers/users';
import { OnboardingContext } from '../../../app/components/OnboardingContext';
import { withRouter, Location, Router } from '../../../components/hoc/withRouter';
import '../../../components/search-navigator.css';
import '../styles.css';

interface Props {
  currentUser: T.CurrentUser;
  isFavorite: boolean;
  location: Pick<Location, 'pathname' | 'query'>;
  organization: T.Organization | undefined;
  router: Pick<Router, 'push' | 'replace'>;
  storageOptionsSuffix?: string;
}

interface State {
  facets?: Facets;
  initialLoading: boolean;
  loading: boolean;
  pageIndex?: number;
  projects?: Project[];
  query: Query;
  total?: number;
}

const PROJECTS_SORT = 'sonarqube.projects.sort';
const PROJECTS_VIEW = 'sonarqube.projects.view';
const PROJECTS_VISUALIZATION = 'sonarqube.projects.visualization';

export class AllProjects extends React.PureComponent<Props, State> {
  mounted = false;

  constructor(props: Props) {
    super(props);
    this.state = { initialLoading: true, loading: true, query: {} };
  }

  componentDidMount() {
    this.mounted = true;

    if (this.props.isFavorite && !isLoggedIn(this.props.currentUser)) {
      handleRequiredAuthentication();
      return;
    }
    this.handleQueryChange(true);
    this.updateFooterClass();
  }

  componentDidUpdate(prevProps: Props) {
    if (prevProps.location.query !== this.props.location.query) {
      this.handleQueryChange(false);
    }

    if (
      prevProps.organization &&
      this.props.organization &&
      prevProps.organization.key !== this.props.organization.key
    ) {
      this.setState({ initialLoading: true });
    }

    this.updateFooterClass();
  }

  componentWillUnmount() {
    this.mounted = false;
    removeSideBarClass();
  }

  fetchProjects = (query: Query) => {
    this.setState({ loading: true, query });
    fetchProjects(query, this.props.isFavorite, this.props.organization).then(response => {
      if (this.mounted) {
        this.setState({
          facets: response.facets,
          initialLoading: false,
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
      fetchProjects(query, this.props.isFavorite, this.props.organization, pageIndex + 1).then(
        response => {
          if (this.mounted) {
            this.setState({
              loading: false,
              pageIndex: pageIndex + 1,
              projects: [...projects, ...response.projects]
            });
          }
        },
        this.stopLoading
      );
    }
  };

  getSort = () => this.state.query.sort || 'name';

  getStorageOptions = () => {
    const { storageOptionsSuffix } = this.props;
    const options: {
      sort?: string;
      view?: string;
      visualization?: string;
    } = {};
    if (get(PROJECTS_SORT, storageOptionsSuffix)) {
      options.sort = get(PROJECTS_SORT, storageOptionsSuffix) || undefined;
    }
    if (get(PROJECTS_VIEW, storageOptionsSuffix)) {
      options.view = get(PROJECTS_VIEW, storageOptionsSuffix) || undefined;
    }
    if (get(PROJECTS_VISUALIZATION, storageOptionsSuffix)) {
      options.visualization = get(PROJECTS_VISUALIZATION, storageOptionsSuffix) || undefined;
    }
    return options;
  };

  getView = () => this.state.query.view || 'overall';

  getVisualization = () => this.state.query.visualization || 'risk';

  handleClearAll = () => {
    this.props.router.push({ pathname: this.props.location.pathname });
  };

  handlePerspectiveChange = ({ view, visualization }: { view: string; visualization?: string }) => {
    const { storageOptionsSuffix } = this.props;
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
      this.props.router.push({ pathname: this.props.location.pathname, query });
    } else {
      this.updateLocationQuery(query);
    }

    save(PROJECTS_SORT, query.sort, storageOptionsSuffix);
    save(PROJECTS_VIEW, query.view, storageOptionsSuffix);
    save(PROJECTS_VISUALIZATION, visualization, storageOptionsSuffix);
  };

  handleQueryChange(initialMount: boolean) {
    const query = parseUrlQuery(this.props.location.query);
    const savedOptions = this.getStorageOptions();
    const savedOptionsSet = savedOptions.sort || savedOptions.view || savedOptions.visualization;

    // if there is no visualization parameters (sort, view, visualization), but there are saved preferences in the localStorage
    if (initialMount && !hasVisualizationParams(query) && savedOptionsSet) {
      this.props.router.replace({ pathname: this.props.location.pathname, query: savedOptions });
    } else {
      this.fetchProjects(query);
    }
  }

  handleSortChange = (sort: string, desc: boolean) => {
    const asString = (desc ? '-' : '') + sort;
    this.updateLocationQuery({ sort: asString });
    save(PROJECTS_SORT, asString, this.props.storageOptionsSuffix);
  };

  stopLoading = () => {
    if (this.mounted) {
      this.setState({ initialLoading: false, loading: false });
    }
  };

  updateLocationQuery = (newQuery: RawQuery) => {
    const query = omitBy({ ...this.props.location.query, ...newQuery }, x => !x);
    this.props.router.push({ pathname: this.props.location.pathname, query });
  };

  updateFooterClass = () => {
    const { organization } = this.props;
    const { initialLoading, projects } = this.state;
    const isOrganizationContext = isSonarCloud() && organization;
    const isEmpty = projects && projects.length === 0;

    if (isOrganizationContext && (initialLoading || isEmpty)) {
      removeSideBarClass();
    } else {
      addSideBarClass();
    }
  };

  renderSide = () => (
    <ScreenPositionHelper className="layout-page-side-outer">
      {({ top }) => (
        <div className="layout-page-side projects-page-side" style={{ top }}>
          <div className="layout-page-side-inner">
            <div className="layout-page-filters">
              <A11ySkipTarget
                anchor="projects_filters"
                label={translate('projects.skip_to_filters')}
                weight={10}
              />

              <PageSidebar
                facets={this.state.facets}
                onClearAll={this.handleClearAll}
                onQueryChange={this.updateLocationQuery}
                organization={this.props.organization}
                query={this.state.query}
                showFavoriteFilter={!isSonarCloud()}
                view={this.getView()}
                visualization={this.getVisualization()}
              />
            </div>
          </div>
        </div>
      )}
    </ScreenPositionHelper>
  );

  renderHeader = () => (
    <div className="layout-page-header-panel layout-page-main-header">
      <div className="layout-page-header-panel-inner layout-page-main-header-inner">
        <div className="layout-page-main-inner">
          <PageHeader
            currentUser={this.props.currentUser}
            isFavorite={this.props.isFavorite}
            loading={this.state.loading}
            onPerspectiveChange={this.handlePerspectiveChange}
            onQueryChange={this.updateLocationQuery}
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

  renderMain = () => {
    return (
      <DeferredSpinner loading={this.state.loading}>
        {this.getView() === 'visualizations' ? (
          <div className="layout-page-main-inner">
            {this.state.projects && (
              <Visualizations
                displayOrganizations={!this.props.organization && isSonarCloud()}
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
                currentUser={this.props.currentUser}
                isFavorite={this.props.isFavorite}
                isFiltered={hasFilterParams(this.state.query)}
                organization={this.props.organization}
                projects={this.state.projects}
                query={this.state.query}
              />
            )}
            <ListFooter
              count={this.state.projects !== undefined ? this.state.projects.length : 0}
              loadMore={this.fetchMoreProjects}
              ready={!this.state.loading}
              total={this.state.total !== undefined ? this.state.total : 0}
            />
          </div>
        )}
      </DeferredSpinner>
    );
  };

  render() {
    const { organization } = this.props;
    const { projects } = this.state;
    const isOrganizationContext = isSonarCloud() && organization;
    const initialLoading = isOrganizationContext && this.state.initialLoading;
    const organizationEmpty =
      isOrganizationContext &&
      projects &&
      projects.length === 0 &&
      !this.state.loading &&
      !hasFilterParams(this.state.query);

    return (
      <div className="layout-page projects-page" id="projects-page">
        <Suggestions suggestions="projects" />
        <Helmet title={translate('projects.page')} />

        {initialLoading ? (
          <div className="display-flex-space-around width-100 huge-spacer-top">
            <DeferredSpinner />
          </div>
        ) : (
          <>
            {!organizationEmpty && this.renderSide()}

            <div className="layout-page-main">
              <A11ySkipTarget anchor="projects_main" />

              {organizationEmpty && organization ? (
                <OnboardingContext.Consumer>
                  {openProjectOnboarding => (
                    <OrganizationEmpty
                      openProjectOnboarding={openProjectOnboarding}
                      organization={organization}
                    />
                  )}
                </OnboardingContext.Consumer>
              ) : (
                <>
                  {this.renderHeader()}
                  {this.renderMain()}
                </>
              )}
            </div>
          </>
        )}
      </div>
    );
  }
}

export default withRouter(AllProjects);
