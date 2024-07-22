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
import { Button, ButtonVariety } from '@sonarsource/echoes-react';
import { Card, DarkLabel } from 'design-system';
import * as React from 'react';
import { translate } from '../../../helpers/l10n';
import { EditionKey } from '../../../types/editions';
import { RiskConsent } from '../../../types/plugins';

export interface PluginRiskConsentBoxProps {
  acknowledgeRisk: () => void;
  currentEdition?: EditionKey;
  riskConsent?: RiskConsent;
}

export default function PluginRiskConsentBox(props: Readonly<PluginRiskConsentBoxProps>) {
  const { currentEdition, riskConsent } = props;

  if (riskConsent === RiskConsent.Accepted) {
    return null;
  }

  return (
    <Card className="sw-mt-6 it__plugin_risk_consent_box">
      <DarkLabel>{translate('marketplace.risk_consent.title')}</DarkLabel>

      <p className="sw-mt-2">{translate('marketplace.risk_consent.description')}</p>
      {currentEdition === EditionKey.community && (
        <p className="sw-mt-2">{translate('marketplace.risk_consent.installation')}</p>
      )}
      <Button className="sw-mt-4" onClick={props.acknowledgeRisk} variety={ButtonVariety.Primary}>
        {translate('marketplace.risk_consent.action')}
      </Button>
    </Card>
  );
}
