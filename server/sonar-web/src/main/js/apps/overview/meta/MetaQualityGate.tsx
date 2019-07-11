/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import { Link } from 'react-router';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { getQualityGateUrl } from '../../../helpers/urls';

interface Props {
  organization?: string;
  qualityGate: { isDefault?: boolean; key: string; name: string };
}

export default function MetaQualityGate({ qualityGate, organization }: Props) {
  return (
    <>
      <h4 className="overview-meta-header">{translate('overview.quality_gate')}</h4>

      <ul className="overview-meta-list">
        <li>
          {qualityGate.isDefault && (
            <span className="note spacer-right">{'(' + translate('default') + ')'}</span>
          )}
          <Link to={getQualityGateUrl(qualityGate.key, organization)}>{qualityGate.name}</Link>
        </li>
      </ul>
    </>
  );
}
