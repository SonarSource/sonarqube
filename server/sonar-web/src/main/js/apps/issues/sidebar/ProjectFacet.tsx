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
import ListStyleFacet from '../../../components/facet/ListStyleFacet';
import { Query, ReferencedComponent } from '../utils';
import { searchProjects, getTree } from '../../../api/components';
import { Component, Paging } from '../../../app/types';
import Organization from '../../../components/shared/Organization';
import QualifierIcon from '../../../components/icons-components/QualifierIcon';
import { translate } from '../../../helpers/l10n';
import { highlightTerm } from '../../../helpers/search';

interface Props {
  component: Component | undefined;
  loading?: boolean;
  fetching: boolean;
  onChange: (changes: Partial<Query>) => void;
  onToggle: (property: string) => void;
  open: boolean;
  organization: { key: string } | undefined;
  projects: string[];
  referencedComponents: { [componentKey: string]: ReferencedComponent };
  stats: { [x: string]: number } | undefined;
}

interface SearchedProject {
  id: string;
  name: string;
  organization: string;
}

export default class ProjectFacet extends React.PureComponent<Props> {
  handleSearch = (
    query: string,
    page = 1
  ): Promise<{ results: SearchedProject[]; paging: Paging }> => {
    const { component, organization } = this.props;
    if (component && ['VW', 'SVW', 'APP'].includes(component.qualifier)) {
      return getTree(component.key, { p: page, ps: 30, q: query, qualifiers: 'TRK' }).then(
        ({ components, paging }) => ({
          paging,
          results: components.map(component => ({
            id: component.refId || component.id,
            key: component.key,
            name: component.name,
            organization: component.organization
          }))
        })
      );
    }

    return searchProjects({
      p: page,
      ps: 30,
      filter: query ? `query = "${query}"` : '',
      organization: organization && organization.key
    }).then(({ components, paging }) => ({
      paging,
      results: components.map(component => ({
        id: component.id,
        key: component.key,
        name: component.name,
        organization: component.organization
      }))
    }));
  };

  getProjectName = (project: string) => {
    const { referencedComponents } = this.props;
    return referencedComponents[project] ? referencedComponents[project].name : project;
  };

  renderFacetItem = (project: string) => {
    const { referencedComponents } = this.props;
    return referencedComponents[project] ? (
      this.renderProject(referencedComponents[project])
    ) : (
      <span>
        <QualifierIcon className="little-spacer-right" qualifier="TRK" />
        {project}
      </span>
    );
  };

  renderProject = (project: Pick<SearchedProject, 'name' | 'organization'>) => (
    <span>
      <QualifierIcon className="little-spacer-right" qualifier="TRK" />
      {!this.props.organization && (
        <Organization link={false} organizationKey={project.organization} />
      )}
      {project.name}
    </span>
  );

  renderSearchResult = (project: Pick<SearchedProject, 'name' | 'organization'>, term: string) => (
    <>
      <QualifierIcon className="little-spacer-right" qualifier="TRK" />
      {!this.props.organization && (
        <Organization link={false} organizationKey={project.organization} />
      )}
      {highlightTerm(project.name, term)}
    </>
  );

  render() {
    return (
      <ListStyleFacet
        facetHeader={translate('issues.facet.projects')}
        fetching={this.props.fetching}
        getFacetItemText={this.getProjectName}
        getSearchResultKey={(project: SearchedProject) => project.id}
        getSearchResultText={(project: SearchedProject) => project.name}
        onChange={this.props.onChange}
        onSearch={this.handleSearch}
        onToggle={this.props.onToggle}
        open={this.props.open}
        property="projects"
        renderFacetItem={this.renderFacetItem}
        renderSearchResult={this.renderSearchResult}
        searchPlaceholder={translate('search.search_for_projects')}
        stats={this.props.stats}
        values={this.props.projects}
      />
    );
  }
}
