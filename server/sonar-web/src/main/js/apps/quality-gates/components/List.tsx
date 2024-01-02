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
import {
  Badge,
  BareButton,
  FlagWarningIcon,
  SubnavigationGroup,
  SubnavigationItem,
} from 'design-system';
import * as React from 'react';
import { useNavigate } from 'react-router-dom';
import Tooltip from '../../../components/controls/Tooltip';

import { translate } from '../../../helpers/l10n';
import { getQualityGateUrl } from '../../../helpers/urls';
import { CaycStatus, QualityGate } from '../../../types/types';
import BuiltInQualityGateBadge from './BuiltInQualityGateBadge';

interface Props {
  qualityGates: QualityGate[];
  currentQualityGate?: string;
}

export default function List({ qualityGates, currentQualityGate }: Props) {
  const navigateTo = useNavigate();

  function redirectQualityGate(qualityGateName: string) {
    navigateTo(getQualityGateUrl(qualityGateName));
  }

  return (
    <SubnavigationGroup>
      {qualityGates.map((qualityGate) => {
        const isDefaultTitle = qualityGate.isDefault ? ` ${translate('default')}` : '';
        const isBuiltInTitle = qualityGate.isBuiltIn
          ? ` ${translate('quality_gates.built_in')}`
          : '';

        return (
          <SubnavigationItem
            className="it__list-group-item"
            active={currentQualityGate === qualityGate.name}
            key={qualityGate.name}
            onClick={() => {
              redirectQualityGate(qualityGate.name);
            }}
          >
            <div className="sw-flex sw-flex-col sw-min-w-0">
              <BareButton
                aria-current={currentQualityGate === qualityGate.name && 'page'}
                title={`${qualityGate.name}${isDefaultTitle}${isBuiltInTitle}`}
                className="sw-flex-1 sw-text-ellipsis sw-overflow-hidden sw-max-w-abs-250 sw-whitespace-nowrap"
              >
                {qualityGate.name}
              </BareButton>

              {(qualityGate.isDefault || qualityGate.isBuiltIn) && (
                <div className="sw-mt-2">
                  {qualityGate.isDefault && (
                    <Badge className="sw-mr-2">{translate('default')}</Badge>
                  )}
                  {qualityGate.isBuiltIn && <BuiltInQualityGateBadge />}
                </div>
              )}
            </div>
            {qualityGate.caycStatus === CaycStatus.NonCompliant &&
              qualityGate.actions?.manageConditions && (
                <Tooltip overlay={translate('quality_gates.cayc.tooltip.message')}>
                  <FlagWarningIcon description={translate('quality_gates.cayc.tooltip.message')} />
                </Tooltip>
              )}
          </SubnavigationItem>
        );
      })}
    </SubnavigationGroup>
  );
}
