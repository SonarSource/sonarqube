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
import classNames from 'classnames';
import React from 'react';
import { translate } from '../../helpers/l10n';
import {
  SoftwareImpact,
  SoftwareImpactSeverity,
  SoftwareQuality,
} from '../../types/clean-code-taxonomy';
import SoftwareImpactPill from './SoftwareImpactPill';

interface SoftwareImpactPillListProps extends React.HTMLAttributes<HTMLUListElement> {
  softwareImpacts: SoftwareImpact[];
  className?: string;
  type?: Parameters<typeof SoftwareImpactPill>[0]['type'];
}

const severityMap = {
  [SoftwareImpactSeverity.High]: 2,
  [SoftwareImpactSeverity.Medium]: 1,
  [SoftwareImpactSeverity.Low]: 0,
};

export default function SoftwareImpactPillList({
  softwareImpacts,
  type,
  className,
  ...props
}: Readonly<SoftwareImpactPillListProps>) {
  const getQualityLabel = (quality: SoftwareQuality) => translate('software_quality', quality);
  const sortingFn = (a: SoftwareImpact, b: SoftwareImpact) => {
    if (a.severity !== b.severity) {
      return severityMap[b.severity] - severityMap[a.severity];
    }
    return getQualityLabel(a.softwareQuality).localeCompare(getQualityLabel(b.softwareQuality));
  };

  return (
    <ul className={classNames('sw-flex sw-gap-2', className)} {...props}>
      {softwareImpacts
        .slice()
        .sort(sortingFn)
        .map(({ severity, softwareQuality }) => (
          <li key={softwareQuality}>
            <SoftwareImpactPill
              severity={severity}
              quality={getQualityLabel(softwareQuality)}
              type={type}
            />
          </li>
        ))}
    </ul>
  );
}
