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
import { Facets, Query, FacetKey, OpenFacets } from '../query';
import { Profile } from '../../../api/quality-profiles';

interface Props {
  facets?: Facets;
  onFacetToggle: (facet: FacetKey) => void;
  onFilterChange: (changes: Partial<Query>) => void;
  openFacets: OpenFacets;
  organization: string | undefined;
  organizationsEnabled?: boolean;
  query: Query;
  referencedProfiles: { [profile: string]: Profile };
  referencedRepositories: { [repository: string]: { key: string; language: string; name: string } };
  selectedProfile?: Profile;
}

export default function FacetsList(props: Props) {
  const inheritanceDisabled =
    props.query.compareToProfile !== undefined ||
    props.selectedProfile === undefined ||
    !props.selectedProfile.isInherited;

  const activationSeverityDisabled =
    props.query.compareToProfile !== undefined ||
    props.selectedProfile === undefined ||
    !props.query.activation;

  return (
    <div className="search-navigator-facets-list">
      <LanguageFacet
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
        organization={props.organization}
        open={!!props.openFacets.tags}
        stats={props.facets && props.facets.tags}
        values={props.query.tags}
      />
      <RepositoryFacet
        onChange={props.onFilterChange}
        onToggle={props.onFacetToggle}
        open={!!props.openFacets.repositories}
        stats={props.facets && props.facets.repositories}
        referencedRepositories={props.referencedRepositories}
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
      <AvailableSinceFacet
        onChange={props.onFilterChange}
        onToggle={props.onFacetToggle}
        open={!!props.openFacets.availableSince}
        value={props.query.availableSince}
      />
      {!props.organizationsEnabled && (
        <TemplateFacet
          onChange={props.onFilterChange}
          onToggle={props.onFacetToggle}
          open={!!props.openFacets.template}
          value={props.query.template}
        />
      )}
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
    </div>
  );
}
