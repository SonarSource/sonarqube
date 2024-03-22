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
import { HelperHintIcon, LightGreyCardTitle, PageTitle } from 'design-system';
import React from 'react';
import HelpTooltip from '../../../components/controls/HelpTooltip';
import { translate } from '../../../helpers/l10n';

export function QualityGateStatusTitle() {
  return (
    <LightGreyCardTitle>
      <div className="sw-flex sw-items-center">
        <PageTitle as="h2" text={translate('overview.quality_gate.status')} />
        <HelpTooltip
          className="sw-ml-2"
          overlay={<div className="sw-my-4">{translate('overview.quality_gate.help')}</div>}
        >
          <HelperHintIcon aria-label="help-tooltip" />
        </HelpTooltip>
      </div>
    </LightGreyCardTitle>
  );
}
