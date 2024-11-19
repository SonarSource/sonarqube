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
import { chunk, keyBy, last, mapValues, omitBy, pick } from 'lodash';
import { useEffect, useMemo } from 'react';
import { Helmet } from 'react-helmet-async';
import { useIntl } from 'react-intl';
import {
  LAYOUT_FOOTER_HEIGHT,
  LargeCenteredLayout,
  PageContentFontWrapper,
  themeBorder,
  themeColor,
} from '~design-system';
import A11ySkipTarget from '~sonar-aligned/components/a11y/A11ySkipTarget';
import { useLocation, useRouter } from '~sonar-aligned/components/hoc/withRouter';
import { ComponentQualifier } from '~sonar-aligned/types/component';
import { RawQuery } from '~sonar-aligned/types/router';
import { searchProjects } from '../../../api/components';
import { useAppState } from '../../../app/components/app-state/withAppStateContext';
import { useCurrentUser } from '../../../app/components/current-user/CurrentUserContext';
import EmptySearch from '../../../components/common/EmptySearch';
import ScreenPositionHelper from '../../../components/common/ScreenPositionHelper';
import '../../../components/search-navigator.css';
import handleRequiredAuthentication from '../../../helpers/handleRequiredAuthentication';
import { translate } from '../../../helpers/l10n';
import { isDefined } from '../../../helpers/types';
import useLocalStorage from '../../../hooks/useLocalStorage';
import { useMeasuresForProjectsQuery } from '../../../queries/measures';
import {
  PROJECTS_PAGE_SIZE,
  useMyScannableProjectsQuery,
  useProjectsQuery,
} from '../../../queries/projects';
import { useStandardExperienceMode } from '../../../queries/settings';
import { isLoggedIn } from '../../../types/users';
import { hasFilterParams, parseUrlQuery } from '../query';
import '../styles.css';
import {
  SORTING_SWITCH,
  convertToQueryData,
  defineMetrics,
  getFacetsMap,
  parseSorting,
} from '../utils';
import EmptyFavoriteSearch from './EmptyFavoriteSearch';
import EmptyInstance from './EmptyInstance';
import NoFavoriteProjects from './NoFavoriteProjects';
import PageHeader from './PageHeader';
import PageSidebar from './PageSidebar';
import ProjectsList from './ProjectsList';

export const LS_PROJECTS_SORT = 'sonarqube.projects.sort';
export const LS_PROJECTS_VIEW = 'sonarqube.projects.view';

function AllProjects({ isFavorite }: Readonly<{ isFavorite: boolean }>) {
  const appState = useAppState();
  const { currentUser } = useCurrentUser();
  const router = useRouter();
  const intl = useIntl();
  const { query, pathname } = useLocation();
  const parsedQuery = parseUrlQuery(query);
  const querySort = parsedQuery.sort ?? 'name';
  const queryView = parsedQuery.view ?? 'overall';
  const [projectsSort, setProjectsSort] = useLocalStorage(LS_PROJECTS_SORT);
  const [projectsView, setProjectsView] = useLocalStorage(LS_PROJECTS_VIEW);
  const { data: isStandardMode = false, isLoading: loadingMode } = useStandardExperienceMode();

  const {
    data: projectPages,
    isLoading: loadingProjects,
    isFetchingNextPage,
    fetchNextPage,
  } = useProjectsQuery(
    {
      isFavorite,
      query: parsedQuery,
      isStandardMode,
    },
    { refetchOnMount: 'always' },
  );
  const { data: { projects: scannableProjects = [] } = {}, isLoading: loadingScannableProjects } =
    useMyScannableProjectsQuery();
  const { projects, facets, paging } = useMemo(
    () => ({
      projects:
        projectPages?.pages
          .flatMap((page) => page.components)
          .map((project) => ({
            ...project,
            isScannable: scannableProjects.find((p) => p.key === project.key) !== undefined,
          })) ?? [],
      facets: getFacetsMap(
        projectPages?.pages[projectPages?.pages.length - 1]?.facets ?? [],
        isStandardMode,
      ),
      paging: projectPages?.pages[projectPages?.pages.length - 1]?.paging,
    }),
    [projectPages, scannableProjects, isStandardMode],
  );

  // Fetch measures by using chunks of 50
  const measureQueries = useMeasuresForProjectsQuery({
    projectKeys: projects.map((p) => p.key),
    metricKeys: defineMetrics(parsedQuery),
  });
  const measuresForLastChunkAreLoading = Boolean(last(measureQueries)?.isLoading);
  const measures = measureQueries
    .map((q) => q.data)
    .flat()
    .filter(isDefined);

  // When measures for latest page are loading, we don't want to show them
  const readyProjects = useMemo(() => {
    if (measuresForLastChunkAreLoading) {
      return chunk(projects, PROJECTS_PAGE_SIZE).slice(0, -1).flat();
    }

    return projects;
  }, [projects, measuresForLastChunkAreLoading]);

  const isLoading =
    loadingMode ||
    loadingProjects ||
    loadingScannableProjects ||
    Boolean(measureQueries[0]?.isLoading);

  // Set sort and view from LS if not present in URL
  useEffect(() => {
    const hasViewParams = parsedQuery.view ?? parsedQuery.sort;
    const hasSavedOptions = projectsSort ?? projectsView;

    if (hasViewParams === undefined && hasSavedOptions) {
      router.replace({ pathname, query: { ...query, sort: projectsSort, view: projectsView } });
    }
  }, [projectsSort, projectsView, router, parsedQuery, query, pathname]);

  /*
   * Needs refactoring to query
   */
  const loadSearchResultCount = (property: string, values: string[]) => {
    const data = convertToQueryData(
      { ...parsedQuery, [property]: values },
      isFavorite,
      isStandardMode,
      {
        ps: 1,
        facets: property,
      },
    );

    return searchProjects(data).then(({ facets }) => {
      const values = facets.find((facet) => facet.property === property)?.values ?? [];

      return mapValues(keyBy(values, 'val'), 'count');
    });
  };

  const updateLocationQuery = (newQuery: RawQuery) => {
    const nextQuery = omitBy({ ...query, ...newQuery }, (x) => !x);
    router.push({ pathname, query: nextQuery });
  };

  const handleClearAll = () => {
    const queryWithoutFilters = pick(query, ['view', 'sort']);
    router.push({ pathname, query: queryWithoutFilters });
  };

  const handleSortChange = (sort: string, desc: boolean) => {
    const asString = (desc ? '-' : '') + sort;
    updateLocationQuery({ sort: asString });
    setProjectsSort(asString);
  };

  const handlePerspectiveChange = ({ view }: { view?: string }) => {
    const query: {
      sort?: string;
      view: string | undefined;
    } = {
      view: view === 'overall' ? undefined : view,
    };

    if (isDefined(parsedQuery.sort)) {
      const sort = parseSorting(parsedQuery.sort);

      if (isDefined(SORTING_SWITCH[sort.sortValue])) {
        query.sort = (sort.sortDesc ? '-' : '') + SORTING_SWITCH[sort.sortValue];
      }
    }

    router.push({ pathname, query });

    setProjectsSort(query.sort);
    setProjectsView(query.view);
  };

  const renderSide = () => (
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
                applicationsEnabled={appState.qualifiers.includes(ComponentQualifier.Application)}
                facets={facets}
                loadSearchResultCount={loadSearchResultCount}
                onClearAll={handleClearAll}
                onQueryChange={updateLocationQuery}
                query={parsedQuery}
                view={queryView}
              />
            </div>
          </section>
        )}
      </ScreenPositionHelper>
    </SideBarStyle>
  );

  const renderHeader = () => (
    <PageHeaderWrapper className="sw-w-full">
      <PageHeader
        currentUser={currentUser}
        onPerspectiveChange={handlePerspectiveChange}
        onQueryChange={updateLocationQuery}
        onSortChange={handleSortChange}
        query={parsedQuery}
        selectedSort={querySort}
        total={paging?.total}
        view={queryView}
      />
    </PageHeaderWrapper>
  );

  const renderMain = () => {
    const isFiltered = hasFilterParams(parsedQuery);
    return (
      <div className="it__layout-page-main-inner it__projects-list sw-h-full">
        <div aria-live="polite" aria-busy={isLoading}>
          <Spinner isLoading={isLoading}>
            {readyProjects.length === 0 && isFiltered && isFavorite && (
              <EmptyFavoriteSearch query={parsedQuery} />
            )}
            {readyProjects.length === 0 && isFiltered && !isFavorite && <EmptySearch />}
            {readyProjects.length === 0 && !isFiltered && isFavorite && <NoFavoriteProjects />}
            {readyProjects.length === 0 && !isFiltered && !isFavorite && <EmptyInstance />}
            {readyProjects.length > 0 && (
              <span className="sw-sr-only">
                {intl.formatMessage({ id: 'projects.x_projects_found' }, { count: paging?.total })}
              </span>
            )}
          </Spinner>
        </div>
        {readyProjects.length > 0 && (
          <ProjectsList
            cardType={queryView}
            isFavorite={isFavorite}
            isFiltered={hasFilterParams(parsedQuery)}
            loading={isFetchingNextPage || measuresForLastChunkAreLoading}
            loadMore={fetchNextPage}
            measures={measures}
            projects={readyProjects}
            query={parsedQuery}
            total={paging?.total}
          />
        )}
      </div>
    );
  };

  return (
    <StyledWrapper id="projects-page">
      <Helmet defer={false} title={translate('projects.page')} />

      <Heading as="h1" className="sw-sr-only">
        {translate('projects.page')}
      </Heading>

      <LargeCenteredLayout>
        <PageContentFontWrapper className="sw-flex sw-w-full sw-typo-lg">
          {renderSide()}

          <main className="sw-flex sw-flex-col sw-box-border sw-min-w-0 sw-pl-12 sw-pt-6 sw-flex-1">
            <A11ySkipTarget anchor="projects_main" />

            <Heading as="h2" className="sw-sr-only">
              {translate('list_of_projects')}
            </Heading>

            {renderHeader()}

            {renderMain()}
          </main>
        </PageContentFontWrapper>
      </LargeCenteredLayout>
    </StyledWrapper>
  );
}

function withRedirectWrapper(Component: React.ComponentType<{ isFavorite: boolean }>) {
  return function Wrapper(props: Readonly<{ isFavorite: boolean }>) {
    const { currentUser } = useCurrentUser();
    if (props.isFavorite && !isLoggedIn(currentUser)) {
      handleRequiredAuthentication();
      return null;
    }

    return <Component {...props} />;
  };
}

export default withRedirectWrapper(AllProjects);

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
