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
import * as React from 'react';
import { Profile } from '../../../api/quality-profiles';
import { Dict } from '../../../types/types';
import StandardFacet from '../../issues/sidebar/StandardFacet';
import { Facets, OpenFacets, Query } from '../query';
import ActivationSeverityFacet from './ActivationSeverityFacet';
import AvailableSinceFacet from './AvailableSinceFacet';
import DefaultSeverityFacet from './DefaultSeverityFacet';
import InheritanceFacet from './InheritanceFacet';
import LanguageFacet from './LanguageFacet';
import ProfileFacet from './ProfileFacet';
import RepositoryFacet from './RepositoryFacet';
import StatusFacet from './StatusFacet';
import TagFacet from './TagFacet';
import TemplateFacet from './TemplateFacet';
import TypeFacet from './TypeFacet';

export interface FacetsListProps {
  facets?: Facets;
  hideProfileFacet?: boolean;
  onFacetToggle: (facet: string) => void;
  onFilterChange: (changes: Partial<Query>) => void;
  openFacets: OpenFacets;
  query: Query;
  referencedProfiles: Dict<Profile>;
  referencedRepositories: Dict<{ key: string; language: string; name: string }>;
  selectedProfile?: Profile;
}

export default function FacetsList(props: FacetsListProps) {
  const languageDisabled = !props.hideProfileFacet && props.query.profile !== undefined;

  const inheritanceDisabled =
    props.query.compareToProfile !== undefined ||
    props.selectedProfile === undefined ||
    !props.selectedProfile.isInherited;

  const activationSeverityDisabled =
    props.query.compareToProfile !== undefined ||
    props.selectedProfile === undefined ||
    !props.query.activation;
  return (
    <>
      <LanguageFacet
        disabled={languageDisabled}
        onChange={props.onFilterChange}
        onToggle={props.onFacetToggle}
        open={!!props.openFacets.languages}
        stats={props.facets && props.facets.languages}
        values={props.query.languages}
      />
      <TypeFacet
        onChange={props.onFilterChange}
        onToggle={props.onFacetToggle}
        open={!!props.openFacets.types}
        stats={props.facets && props.facets.types}
        values={props.query.types}
      />
      <TagFacet
        onChange={props.onFilterChange}
        onToggle={props.onFacetToggle}
        open={!!props.openFacets.tags}
        stats={props.facets && props.facets.tags}
        values={props.query.tags}
      />
      <RepositoryFacet
        onChange={props.onFilterChange}
        onToggle={props.onFacetToggle}
        open={!!props.openFacets.repositories}
        referencedRepositories={props.referencedRepositories}
        stats={props.facets && props.facets.repositories}
        values={props.query.repositories}
      />
      <DefaultSeverityFacet
        onChange={props.onFilterChange}
        onToggle={props.onFacetToggle}
        open={!!props.openFacets.severities}
        stats={props.facets && props.facets.severities}
        values={props.query.severities}
      />
      <StatusFacet
        onChange={props.onFilterChange}
        onToggle={props.onFacetToggle}
        open={!!props.openFacets.statuses}
        stats={props.facets && props.facets.statuses}
        values={props.query.statuses}
      />
      <StandardFacet
        cwe={props.query.cwe}
        cweOpen={!!props.openFacets.cwe}
        cweStats={props.facets && props.facets.cwe}
        fetchingCwe={false}
        fetchingOwaspTop10={false}
        fetchingOwaspTop10-2021={false}
        fetchingSansTop25={false}
        fetchingSonarSourceSecurity={false}
        onChange={props.onFilterChange}
        onToggle={props.onFacetToggle}
        open={!!props.openFacets.standards}
        owaspTop10={props.query.owaspTop10}
        owaspTop10Open={!!props.openFacets.owaspTop10}
        owaspTop10Stats={props.facets && props.facets.owaspTop10}
        owaspTop10-2021={props.query['owaspTop10-2021']}
        owaspTop10-2021Open={!!props.openFacets['owaspTop10-2021']}
        owaspTop10-2021Stats={props.facets && props.facets['owaspTop10-2021']}
        query={props.query}
        sansTop25={props.query.sansTop25}
        sansTop25Open={!!props.openFacets.sansTop25}
        sansTop25Stats={props.facets && props.facets.sansTop25}
        sonarsourceSecurity={props.query.sonarsourceSecurity}
        sonarsourceSecurityOpen={!!props.openFacets.sonarsourceSecurity}
        sonarsourceSecurityStats={props.facets && props.facets.sonarsourceSecurity}
      />
      <AvailableSinceFacet
        onChange={props.onFilterChange}
        onToggle={props.onFacetToggle}
        open={!!props.openFacets.availableSince}
        value={props.query.availableSince}
      />
      <TemplateFacet
        onChange={props.onFilterChange}
        onToggle={props.onFacetToggle}
        open={!!props.openFacets.template}
        value={props.query.template}
      />
      {!props.hideProfileFacet && (
        <>
          <ProfileFacet
            activation={props.query.activation}
            compareToProfile={props.query.compareToProfile}
            languages={props.query.languages}
            onChange={props.onFilterChange}
            onToggle={props.onFacetToggle}
            open={!!props.openFacets.profile}
            referencedProfiles={props.referencedProfiles}
            value={props.query.profile}
          />
          <InheritanceFacet
            disabled={inheritanceDisabled}
            onChange={props.onFilterChange}
            onToggle={props.onFacetToggle}
            open={!!props.openFacets.inheritance}
            value={props.query.inheritance}
          />
          <ActivationSeverityFacet
            disabled={activationSeverityDisabled}
            onChange={props.onFilterChange}
            onToggle={props.onFacetToggle}
            open={!!props.openFacets.activationSeverities}
            stats={props.facets && props.facets.activationSeverities}
            values={props.query.activationSeverities}
          />
        </>
      )}
    </>
  );
}
