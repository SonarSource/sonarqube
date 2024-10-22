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

import { useStandardExperienceMode } from '../../../queries/settings';
import { ProfileChangelogEvent } from '../types';
import CleanCodeAttributeChange from './CleanCodeAttributeChange';
import ParameterChange from './ParameterChange';
import SeverityChange from './SeverityChange';
import SoftwareImpactChange from './SoftwareImpactChange';

interface Props {
  changes: ProfileChangelogEvent['params'];
}

export default function ChangesList({ changes }: Readonly<Props>) {
  const {
    severity,
    oldCleanCodeAttribute,
    oldCleanCodeAttributeCategory,
    newCleanCodeAttribute,
    newCleanCodeAttributeCategory,
    impactChanges,
    ...rest
  } = changes ?? {};

  const { data: isStandardMode } = useStandardExperienceMode();

  return (
    <ul className="sw-w-full sw-flex sw-flex-col sw-gap-1">
      {severity && isStandardMode && (
        <li>
          <SeverityChange severity={severity} />
        </li>
      )}

      {!isStandardMode &&
        oldCleanCodeAttribute &&
        oldCleanCodeAttributeCategory &&
        newCleanCodeAttribute &&
        newCleanCodeAttributeCategory && (
          <li>
            <CleanCodeAttributeChange
              oldCleanCodeAttribute={oldCleanCodeAttribute}
              oldCleanCodeAttributeCategory={oldCleanCodeAttributeCategory}
              newCleanCodeAttribute={newCleanCodeAttribute}
              newCleanCodeAttributeCategory={newCleanCodeAttributeCategory}
            />
          </li>
        )}

      {!isStandardMode &&
        impactChanges?.map((impactChange, index) => (
          <li key={index}>
            <SoftwareImpactChange impactChange={impactChange} />
          </li>
        ))}

      {Object.keys(rest).map((key) => (
        <li key={key}>
          <ParameterChange name={key} value={rest[key] as string | null} />
        </li>
      ))}
    </ul>
  );
}
