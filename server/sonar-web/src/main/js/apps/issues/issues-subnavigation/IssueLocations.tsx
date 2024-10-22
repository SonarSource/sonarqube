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

import { useMemo } from 'react';
import { FlowLocation, Issue } from '../../../types/types';
import IssueLocation from './IssueLocation';
import IssueLocationsCrossFile from './IssueLocationsCrossFile';

interface Props {
  concealed?: boolean;
  issue: Pick<Issue, 'component' | 'key' | 'flows' | 'secondaryLocations' | 'type'>;
  locations: FlowLocation[];
  onLocationSelect: (index: number) => void;
  selectedLocationIndex: number | undefined;
}

export default function IssueLocations(props: Props) {
  const { concealed, issue, locations, onLocationSelect, selectedLocationIndex } = props;
  const isCrossFile = useMemo(
    () => locations.some((location) => location.component !== issue.component),
    [locations, issue.component],
  );

  return isCrossFile ? (
    <IssueLocationsCrossFile
      issue={issue}
      locations={locations}
      onLocationSelect={onLocationSelect}
      selectedLocationIndex={selectedLocationIndex}
    />
  ) : (
    <div className="sw-flex sw-flex-col sw-gap-1">
      {locations.map((location, index) => (
        <IssueLocation
          concealed={concealed}
          index={index}
          key={`${location.msg}-${index}`}
          message={location.msg}
          onClick={onLocationSelect}
          selected={index === selectedLocationIndex}
        />
      ))}
    </div>
  );
}
