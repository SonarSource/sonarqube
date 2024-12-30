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

import { Profile as BaseProfile } from '../../api/quality-profiles';
import {
  CleanCodeAttribute,
  CleanCodeAttributeCategory,
  SoftwareImpactSeverity,
  SoftwareQuality,
} from '../../types/clean-code-taxonomy';
import { IssueSeverity } from '../../types/issues';
import { Dict } from '../../types/types';

export interface Profile extends BaseProfile {
  childrenCount: number;
  depth: number;
}

export interface Exporter {
  key: string;
  languages: string[];
  name: string;
}

export interface ProfileChangelogEventImpactChange {
  newSeverity?: SoftwareImpactSeverity;
  newSoftwareQuality?: SoftwareQuality;
  oldSeverity?: SoftwareImpactSeverity;
  oldSoftwareQuality?: SoftwareQuality;
}

export interface ProfileChangelogEvent {
  action: string;
  authorName?: string;
  cleanCodeAttributeCategory?: CleanCodeAttributeCategory;
  date: string;
  // impacts should be always set in the wild. But Next currently has a specific database state for which this field is undefined. May be possible to make this field required in the future.
  impacts?: {
    severity: SoftwareImpactSeverity;
    softwareQuality: SoftwareQuality;
  }[];
  params?: {
    impactChanges?: ProfileChangelogEventImpactChange[];
    newCleanCodeAttribute?: CleanCodeAttribute;
    newCleanCodeAttributeCategory?: CleanCodeAttributeCategory;
    oldCleanCodeAttribute?: CleanCodeAttribute;
    oldCleanCodeAttributeCategory?: CleanCodeAttributeCategory;
    severity?: IssueSeverity;
  } & Dict<string | ProfileChangelogEventImpactChange[] | null>;
  ruleKey: string;
  ruleName: string;
  sonarQubeVersion: string;
}

export enum ProfileActionModals {
  Copy = 'COPY',
  Extend = 'EXTEND',
  Rename = 'RENAME',
  Delete = 'DELETE',
}
