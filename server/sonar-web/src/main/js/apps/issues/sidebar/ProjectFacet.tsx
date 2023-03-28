/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import { omit } from 'lodash';
import * as React from 'react';
import { getTree, searchProjects } from '../../../api/components';
import ListStyleFacet from '../../../components/facet/ListStyleFacet';
import QualifierIcon from '../../../components/icons/QualifierIcon';
import { translate } from '../../../helpers/l10n';
import { highlightTerm } from '../../../helpers/search';
import { ComponentQualifier } from '../../../types/component';
import { Facet, ReferencedComponent } from '../../../types/issues';
import { Component, Dict, Organization, Paging } from '../../../types/types';
import { Query } from '../utils';

interface Props {
  component: Component | undefined;
  organization?: Organization;
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

export default class ProjectFacet extends React.PureComponent<Props> {
  handleSearch = (
    query: string,
    page = 1
  ): Promise<{ results: SearchedProject[]; paging: Paging }> => {
    const { component, organization } = this.props;
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
      organization: organization && organization.kee,
    }).then(({ components, paging }) => ({
      paging,
      results: components.map((component) => ({
        key: component.key,
        name: component.name,
      })),
    }));
  };

  getProjectName = (project: string) => {
    const { referencedComponents } = this.props;
    return referencedComponents[project] ? referencedComponents[project].name : project;
  };

  loadSearchResultCount = (projects: SearchedProject[]) => {
    return this.props.loadSearchResultCount('projects', {
      projects: projects.map((project) => project.key),
    });
  };

  renderFacetItem = (projectKey: string) => {
    return (
      <span>
        <QualifierIcon className="little-spacer-right" qualifier={ComponentQualifier.Project} />
        {this.getProjectName(projectKey)}
      </span>
    );
  };

  renderSearchResult = (project: Pick<SearchedProject, 'name'>, term: string) => (
    <>
      <QualifierIcon className="little-spacer-right" qualifier={ComponentQualifier.Project} />
      {highlightTerm(project.name, term)}
    </>
  );

  render() {
    return (
      <ListStyleFacet<SearchedProject>
        facetHeader={translate('issues.facet.projects')}
        fetching={this.props.fetching}
        getFacetItemText={this.getProjectName}
        getSearchResultKey={(project) => project.key}
        getSearchResultText={(project) => project.name}
        loadSearchResultCount={this.loadSearchResultCount}
        onChange={this.props.onChange}
        onSearch={this.handleSearch}
        onToggle={this.props.onToggle}
        open={this.props.open}
        property="projects"
        query={omit(this.props.query, 'projects')}
        renderFacetItem={this.renderFacetItem}
        renderSearchResult={this.renderSearchResult}
        searchPlaceholder={translate('search.search_for_projects')}
        stats={this.props.stats}
        values={this.props.projects}
      />
    );
  }
}
