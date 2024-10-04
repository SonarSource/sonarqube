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
import { useIntl } from 'react-intl';
import SoftwareImpactSeverityIcon from '../../../components/icon-mappers/SoftwareImpactSeverityIcon';
import { ProfileChangelogEventImpactChange } from '../types';

interface Props {
  impactChange: ProfileChangelogEventImpactChange;
}

export default function SoftwareImpactChange({ impactChange }: Readonly<Props>) {
  const { oldSeverity, oldSoftwareQuality, newSeverity, newSoftwareQuality } = impactChange;

  const intl = useIntl();

  const labels = {
    oldSeverity: (
      <>
        <SoftwareImpactSeverityIcon severity={oldSeverity} />{' '}
        {intl.formatMessage({ id: `severity_impact.${oldSeverity}` })}
      </>
    ),
    oldSoftwareQuality: intl.formatMessage({ id: `software_quality.${oldSoftwareQuality}` }),
    newSeverity: (
      <>
        <SoftwareImpactSeverityIcon severity={newSeverity} />{' '}
        {intl.formatMessage({ id: `severity_impact.${newSeverity}` })}
      </>
    ),
    newSoftwareQuality: intl.formatMessage({ id: `software_quality.${newSoftwareQuality}` }),
  };

  const isChanged = oldSeverity && oldSoftwareQuality && newSeverity && newSoftwareQuality;
  const isAdded = !oldSeverity && !oldSoftwareQuality && newSeverity && newSoftwareQuality;
  const isRemoved = oldSeverity && oldSoftwareQuality && !newSeverity && !newSoftwareQuality;

  if (isChanged) {
    return (
      <div>{intl.formatMessage({ id: 'quality_profiles.changelog.impact_changed' }, labels)}</div>
    );
  }

  if (isAdded) {
    return (
      <div>{intl.formatMessage({ id: 'quality_profiles.changelog.impact_added' }, labels)}</div>
    );
  }

  if (isRemoved) {
    return (
      <div>{intl.formatMessage({ id: 'quality_profiles.changelog.impact_removed' }, labels)}</div>
    );
  }

  return null;
}
