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
import { NavLink } from 'react-router-dom';
import Tooltip from '../../../components/controls/Tooltip';
import AlertWarnIcon from '../../../components/icons/AlertWarnIcon';
import { translate } from '../../../helpers/l10n';
import { getQualityGateUrl } from '../../../helpers/urls';
import { CaycStatus, QualityGate } from '../../../types/types';
import BuiltInQualityGateBadge from './BuiltInQualityGateBadge';

interface Props {
  qualityGates: QualityGate[];
}

export default function List({ qualityGates }: Props) {
  return (
    <div className="list-group" role="menu">
      {qualityGates.map((qualityGate) => (
        <NavLink
          className="list-group-item display-flex-center"
          role="menuitem"
          data-id={qualityGate.id}
          key={qualityGate.id}
          to={getQualityGateUrl(String(qualityGate.id))}
        >
          <span className="flex-1 text-ellipsis" title={qualityGate.name}>
            {qualityGate.name}
          </span>
          {qualityGate.isDefault && (
            <span className="badge little-spacer-left">{translate('default')}</span>
          )}
          {qualityGate.isBuiltIn && <BuiltInQualityGateBadge className="little-spacer-left" />}

          {qualityGate.caycStatus === CaycStatus.NonCompliant && (
            <>
              {/* Adding a11y-hidden span for accessibility */}
              <span className="a11y-hidden">{translate('quality_gates.cayc.tooltip.message')}</span>
              <Tooltip overlay={translate('quality_gates.cayc.tooltip.message')} accessible={false}>
                <AlertWarnIcon className="spacer-left" />
              </Tooltip>
            </>
          )}
        </NavLink>
      ))}
    </div>
  );
}
