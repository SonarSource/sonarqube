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
import QGMetricsMismatchHelp from '../../apps/issues/sidebar/QGMetricsMismatchHelp';
import { SEVERITIES } from '../../helpers/constants';
import { translate } from '../../helpers/l10n';
import SoftwareImpactSeverityIcon from '../icon-mappers/SoftwareImpactSeverityIcon';
import Facet, { BasicProps } from './Facet';

export default function StandardSeverityFacet(
  props: Readonly<BasicProps & { headerName?: string }>,
) {
  const renderName = React.useCallback(
    (severity: string, disabled: boolean) => (
      <div className="sw-flex sw-items-center">
        <SoftwareImpactSeverityIcon severity={severity} disabled={disabled} />
        <span className="sw-ml-1">{translate('severity', severity)}</span>
      </div>
    ),
    [],
  );

  const renderTextName = React.useCallback(
    (severity: string) => translate('severity', severity),
    [],
  );

  return (
    <Facet
      {...props}
      help={Boolean(props.secondLine) && <QGMetricsMismatchHelp />}
      options={SEVERITIES}
      property="severities"
      renderName={renderName}
      renderTextName={renderTextName}
    />
  );
}
