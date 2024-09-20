/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import styled from '@emotion/styled';
import { Heading, Spinner } from '@sonarsource/echoes-react';
import {
  LAYOUT_FOOTER_HEIGHT,
  LargeCenteredLayout,
  PageContentFontWrapper,
  themeBorder,
  themeColor,
} from 'design-system';
import { keyBy, mapValues, omitBy, pick } from 'lodash';
import * as React from 'react';
import { Helmet } from 'react-helmet-async';
import { useSearchParams } from 'react-router-dom';
import A11ySkipTarget from '~sonar-aligned/components/a11y/A11ySkipTarget';
import { withRouter } from '~sonar-aligned/components/hoc/withRouter';
import { ComponentQualifier } from '~sonar-aligned/types/component';
import { Location, RawQuery, Router } from '~sonar-aligned/types/router';
import { searchProjects } from '../../../api/components';
import withAppStateContext from '../../../app/components/app-state/withAppStateContext';
import withCurrentUserContext from '../../../app/components/current-user/withCurrentUserContext';
import ScreenPositionHelper from '../../../components/common/ScreenPositionHelper';
import '../../../components/search-navigator.css';
import handleRequiredAuthentication from '../../../helpers/handleRequiredAuthentication';
import { translate } from '../../../helpers/l10n';
import { get, save } from '../../../helpers/storage';
import { isDefined } from '../../../helpers/types';
import { useIsLegacyCCTMode } from '../../../queries/settings';
import { AppState } from '../../../types/appstate';
import { CurrentUser, isLoggedIn } from '../../../types/users';
import { Query, hasFilterParams, parseUrlQuery } from '../query';
import '../styles.css';
import { Facets, Project } from '../types';
import { SORTING_SWITCH, convertToQueryData, fetchProjects, parseSorting } from '../utils';
import PageHeader from './PageHeader';
import PageSidebar from './PageSidebar';
import ProjectsList from './ProjectsList';

interface Props {
  appState: AppState;
  currentUser: CurrentUser;
  isFavorite: boolean;
  isLegacy: boolean;
  location: Location;
  router: Router;
}

interface State {
  facets?: Facets;
  loading: boolean;
  pageIndex?: number;
  projects?: Omit<Project, 'measures'>[];
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

    this.handleQueryChange();
  }

  componentDidUpdate(prevProps: Props) {
    if (prevProps.location.query !== this.props.location.query) {
      this.handleQueryChange();
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchMoreProjects = () => {
    const { isFavorite, isLegacy } = this.props;
    const { pageIndex, projects, query } = this.state;

    if (isDefined(pageIndex) && pageIndex !== 0 && projects && Object.keys(query).length !== 0) {
      this.setState({ loading: true });

      fetchProjects({ isFavorite, query, pageIndex: pageIndex + 1, isLegacy }).then((response) => {
        if (this.mounted) {
          this.setState({
            loading: false,
            pageIndex: pageIndex + 1,
            projects: [...projects, ...response.projects],
          });
        }
      }, this.stopLoading);
    }
  };

  getSort = () => this.state.query.sort ?? 'name';

  getView = () => this.state.query.view ?? 'overall';

  handleClearAll = () => {
    const { pathname, query } = this.props.location;

    const queryWithoutFilters = pick(query, ['view', 'sort']);

    this.props.router.push({ pathname, query: queryWithoutFilters });
  };

  handleFavorite = (key: string, isFavorite: boolean) => {
    this.setState(({ projects }) => {
      if (!projects) {
        return null;
      }

      return {
        projects: projects.map((p) => (p.key === key ? { ...p, isFavorite } : p)),
      };
    });
  };

  handlePerspectiveChange = ({ view }: { view?: string }) => {
    const query: {
      sort?: string;
      view: string | undefined;
    } = {
      view: view === 'overall' ? undefined : view,
    };

    if (this.state.query.view === 'leak' || view === 'leak') {
      if (isDefined(this.state.query.sort)) {
        const sort = parseSorting(this.state.query.sort);

        if (isDefined(SORTING_SWITCH[sort.sortValue])) {
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

  handleQueryChange() {
    const { isFavorite, isLegacy } = this.props;

    const queryRaw = this.props.location.query;
    const query = parseUrlQuery(queryRaw);

    this.setState({ loading: true, query });

    fetchProjects({ isFavorite, query, isLegacy }).then((response) => {
      // We ignore the request if the query changed since the time it was initiated
      // If that happened, another query will be initiated anyway
      if (this.mounted && queryRaw === this.props.location.query) {
        this.setState({
          facets: response.facets,
          loading: false,
          pageIndex: 1,
          projects: response.projects,
          total: response.total,
        });
      }
    }, this.stopLoading);
  }

  handleSortChange = (sort: string, desc: boolean) => {
    const asString = (desc ? '-' : '') + sort;
    this.updateLocationQuery({ sort: asString });
    save(LS_PROJECTS_SORT, asString);
  };

  loadSearchResultCount = (property: string, values: string[]) => {
    const { isFavorite, isLegacy } = this.props;
    const { query = {} } = this.state;

    const data = convertToQueryData({ ...query, [property]: values }, isFavorite, isLegacy, {
      ps: 1,
      facets: property,
    });

    return searchProjects(data).then(({ facets }) => {
      const values = facets.find((facet) => facet.property === property)?.values ?? [];

      return mapValues(keyBy(values, 'val'), 'count');
    });
  };

  stopLoading = () => {
    if (this.mounted) {
      this.setState({ loading: false });
    }
  };

  updateLocationQuery = (newQuery: RawQuery) => {
    const query = omitBy({ ...this.props.location.query, ...newQuery }, (x) => !x);
    this.props.router.push({ pathname: this.props.location.pathname, query });
  };

  renderSide = () => (
    <SideBarStyle>
      <ScreenPositionHelper className="sw-z-filterbar">
        {({ top }) => (
          <section
            aria-label={translate('filters')}
            className="sw-overflow-y-auto project-filters-list"
            style={{ height: `calc((100vh - ${top}px) - ${LAYOUT_FOOTER_HEIGHT}px)` }}
          >
            <div className="sw-w-[300px] lg:sw-w-[390px]">
              <A11ySkipTarget
                anchor="projects_filters"
                label={translate('projects.skip_to_filters')}
                weight={10}
              />

              <PageSidebar
                applicationsEnabled={this.props.appState.qualifiers.includes(
                  ComponentQualifier.Application,
                )}
                facets={this.state.facets}
                loadSearchResultCount={this.loadSearchResultCount}
                onClearAll={this.handleClearAll}
                onQueryChange={this.updateLocationQuery}
                query={this.state.query}
                view={this.getView()}
              />
            </div>
          </section>
        )}
      </ScreenPositionHelper>
    </SideBarStyle>
  );

  renderHeader = () => (
    <PageHeaderWrapper className="sw-w-full">
      <PageHeader
        currentUser={this.props.currentUser}
        onPerspectiveChange={this.handlePerspectiveChange}
        onQueryChange={this.updateLocationQuery}
        onSortChange={this.handleSortChange}
        query={this.state.query}
        selectedSort={this.getSort()}
        total={this.state.total}
        view={this.getView()}
      />
    </PageHeaderWrapper>
  );

  renderMain = () => {
    if (this.state.loading && this.state.projects === undefined) {
      return <Spinner />;
    }

    return (
      <div className="it__layout-page-main-inner it__projects-list sw-h-full">
        {this.state.projects && (
          <ProjectsList
            cardType={this.getView()}
            currentUser={this.props.currentUser}
            handleFavorite={this.handleFavorite}
            isFavorite={this.props.isFavorite}
            isFiltered={hasFilterParams(this.state.query)}
            loading={this.state.loading}
            loadMore={this.fetchMoreProjects}
            projects={this.state.projects}
            query={this.state.query}
            total={this.state.total}
          />
        )}
      </div>
    );
  };

  render() {
    return (
      <StyledWrapper id="projects-page">
        <Helmet defer={false} title={translate('projects.page')} />

        <Heading as="h1" className="sw-sr-only">
          {translate('projects.page')}
        </Heading>

        <LargeCenteredLayout>
          <PageContentFontWrapper className="sw-flex sw-w-full sw-typo-lg">
            {this.renderSide()}

            <main className="sw-flex sw-flex-col sw-box-border sw-min-w-0 sw-pl-12 sw-pt-6 sw-flex-1">
              <A11ySkipTarget anchor="projects_main" />

              <Heading as="h2" className="sw-sr-only">
                {translate('list_of_projects')}
              </Heading>

              {this.renderHeader()}

              {this.renderMain()}
            </main>
          </PageContentFontWrapper>
        </LargeCenteredLayout>
      </StyledWrapper>
    );
  }
}

function getStorageOptions() {
  const options: {
    sort?: string;
    view?: string;
  } = {};

  if (get(LS_PROJECTS_SORT) !== null) {
    options.sort = get(LS_PROJECTS_SORT) ?? undefined;
  }

  if (get(LS_PROJECTS_VIEW) !== null) {
    options.view = get(LS_PROJECTS_VIEW) ?? undefined;
  }

  return options;
}

function AllProjectsWrapper(props: Readonly<Omit<Props, 'isLegacy'>>) {
  const [searchParams, setSearchParams] = useSearchParams();
  const savedOptions = getStorageOptions();
  const { data: isLegacy, isLoading } = useIsLegacyCCTMode();

  React.useEffect(
    () => {
      const hasViewParams = searchParams.get('sort') ?? searchParams.get('view');
      const hasSavedOptions = savedOptions.sort ?? savedOptions.view;

      if (!isDefined(hasViewParams) && isDefined(hasSavedOptions)) {
        setSearchParams(savedOptions);
      }
    },
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [
      /* Run once on mount only */
    ],
  );

  return (
    <Spinner isLoading={isLoading}>
      <AllProjects {...props} isLegacy={isLegacy ?? false} />
    </Spinner>
  );
}

export default withRouter(withCurrentUserContext(withAppStateContext(AllProjectsWrapper)));

const StyledWrapper = styled.div`
  background-color: ${themeColor('backgroundPrimary')};
`;

const SideBarStyle = styled.div`
  border-left: ${themeBorder('default', 'filterbarBorder')};
  border-right: ${themeBorder('default', 'filterbarBorder')};
  background-color: ${themeColor('backgroundSecondary')};
`;

const PageHeaderWrapper = styled.div`
  height: 7.5rem;
  border-bottom: ${themeBorder('default', 'filterbarBorder')};
`;
