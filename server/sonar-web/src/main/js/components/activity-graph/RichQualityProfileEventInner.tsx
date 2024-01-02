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
import { DiscreetLink } from 'design-system';
import React from 'react';
import { useIntl } from 'react-intl';
import { getProfileChangelogPath } from '../../apps/quality-profiles/utils';
import { AnalysisEvent, ProjectAnalysisEventCategory } from '../../types/project-activity';

type RichQualityGateEvent = AnalysisEvent &
  Required<Pick<AnalysisEvent, 'description' | 'qualityProfile'>>;

interface RichQualityProfileEventInnerProps {
  event: RichQualityGateEvent;
}

export function isRichQualityProfileEvent(event: AnalysisEvent): event is RichQualityGateEvent {
  return (
    event.category === ProjectAnalysisEventCategory.QualityProfile &&
    event.description !== undefined &&
    event.qualityProfile !== undefined
  );
}

export function RichQualityProfileEventInner({
  event,
}: Readonly<RichQualityProfileEventInnerProps>) {
  const {
    description,
    name,
    qualityProfile: { languageKey, name: qualityProfileName },
  } = event;
  const intl = useIntl();

  const truncatedName = name.split(description)[0];

  return (
    <span aria-label={name}>
      {truncatedName}
      <DiscreetLink
        aria-label={intl.formatMessage(
          { id: 'quality_profiles.page_title_changelog_x' },
          { 0: qualityProfileName },
        )}
        to={getProfileChangelogPath(qualityProfileName, languageKey)}
        // Needed to make this link work from the Activity tab
        // Because of a click handler on a parent component that is also trigerring a redirection
        onClick={(event) => event.stopPropagation()}
      >
        {description}
      </DiscreetLink>
    </span>
  );
}
