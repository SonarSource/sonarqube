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

import { IconProject, Spinner } from '@sonarsource/echoes-react';
import { omit } from 'lodash';
import * as React from 'react';
import { ComponentQualifier } from '~sonar-aligned/types/component';
import { MetricKey } from '~sonar-aligned/types/metrics';
import { getTree, searchProjects } from '../../../api/components';
import { translate } from '../../../helpers/l10n';
import { highlightTerm } from '../../../helpers/search';
import { useProjectQuery } from '../../../queries/projects';
import { Facet, ReferencedComponent } from '../../../types/issues';
import { Component, Dict, Paging } from '../../../types/types';
import { Query } from '../utils';
import { ListStyleFacet } from './ListStyleFacet';

interface Props {
  component: Component | undefined;
  loadSearchResultCount: (property: string, changes: Partial<Query>) => Promise<Facet>;
  fetching: boolean;
  onChange: (changes: Partial<Query>) => void;
  onToggle: (property: string) => void;
  open: boolean;
  projects: string[];
  query: Query;
  referencedComponents: Dict<ReferencedComponent>;
  stats: Dict<number> | undefined;
}

interface SearchedProject {
  key: string;
  name: string;
}

export function ProjectFacet(props: Readonly<Props>) {
  const {
    component,
    fetching,
    onChange,
    onToggle,
    open,
    projects,
    query,
    referencedComponents,
    stats,
  } = props;

  const handleSearch = (
    query: string,
    page = 1,
  ): Promise<{ results: SearchedProject[]; paging: Paging }> => {
    if (
      component &&
      [
        ComponentQualifier.Portfolio,
        ComponentQualifier.SubPortfolio,
        ComponentQualifier.Application,
      ].includes(component.qualifier as ComponentQualifier)
    ) {
      return getTree({
        component: component.key,
        p: page,
        ps: 30,
        q: query,
        qualifiers: ComponentQualifier.Project,
      }).then(({ components, paging }) => ({
        paging,
        results: components.map((component) => ({
          key: component.refKey || component.key,
          name: component.name,
        })),
      }));
    }

    return searchProjects({
      p: page,
      ps: 30,
      filter: query ? `query = "${query}"` : '',
    }).then(({ components, paging }) => ({
      paging,
      results: components.map((component) => ({
        key: component.key,
        name: component.name,
      })),
    }));
  };

  const getProjectName = (project: string) => {
    return referencedComponents[project] ? referencedComponents[project].name : project;
  };

  const loadSearchResultCount = (projects: SearchedProject[]) => {
    return props.loadSearchResultCount(MetricKey.projects, {
      projects: projects.map((project) => project.key),
    });
  };

  const renderFacetItem = (projectKey: string) => {
    const projectName = getProjectName(projectKey);
    return (
      <ProjectItem
        projectKey={projectKey}
        projectName={projectName === projectKey ? undefined : projectName}
      />
    );
  };

  const renderSearchResult = (project: Pick<SearchedProject, 'name'>, term: string) => (
    <>
      <IconProject className="sw-mr-1" />

      {highlightTerm(project.name, term)}
    </>
  );

  return (
    <ListStyleFacet<SearchedProject>
      facetHeader={translate('issues.facet.projects')}
      fetching={fetching}
      getFacetItemText={getProjectName}
      getSearchResultKey={(project) => project.key}
      getSearchResultText={(project) => project.name}
      loadSearchResultCount={loadSearchResultCount}
      onChange={onChange}
      onSearch={handleSearch}
      onToggle={onToggle}
      open={open}
      property={MetricKey.projects}
      query={omit(query, MetricKey.projects)}
      renderFacetItem={renderFacetItem}
      renderSearchResult={renderSearchResult}
      searchPlaceholder={translate('search.search_for_projects')}
      stats={stats}
      values={projects}
    />
  );
}

function ProjectItem({
  projectKey,
  projectName,
}: Readonly<{
  projectKey: string;
  projectName?: string;
}>) {
  const { data, isLoading } = useProjectQuery(projectKey, {
    enabled: projectName === undefined,
    select: (data) => data.components.find((el) => el.key === projectKey),
  });

  const label = projectName ?? (isLoading ? '' : data?.name ?? projectKey);

  return (
    <div className="sw-flex sw-items-center">
      <IconProject className="sw-mr-1" />

      <Spinner isLoading={projectName === undefined && isLoading} />

      <span className="sw-min-w-0 sw-truncate" title={label}>
        {label}
      </span>
    </div>
  );
}
