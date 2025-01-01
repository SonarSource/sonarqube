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
import * as React from 'react';
import { Helmet } from 'react-helmet-async';
import { Card, CenteredLayout, Title } from '~design-system';
import { withRouter } from '~sonar-aligned/components/hoc/withRouter';
import { Router } from '~sonar-aligned/types/router';
import { setSimpleSettingValue } from '../../api/settings';
import { whenLoggedIn } from '../../components/hoc/whenLoggedIn';
import { translate } from '../../helpers/l10n';
import { getBaseUrl } from '../../helpers/system';
import { hasGlobalPermission } from '../../helpers/users';
import { Permissions } from '../../types/permissions';
import { RiskConsent } from '../../types/plugins';
import { SettingsKey } from '../../types/settings';
import { LoggedInUser } from '../../types/users';

export interface PluginRiskConsentProps {
  currentUser: LoggedInUser;
  router: Router;
}

export function PluginRiskConsent(props: Readonly<PluginRiskConsentProps>) {
  const { currentUser, router } = props;

  const isAdmin = hasGlobalPermission(currentUser, Permissions.Admin);

  React.useEffect(() => {
    if (!isAdmin) {
      router.replace('/');
    }
  }, [isAdmin, router]);

  if (!isAdmin) {
    return null;
  }

  const acknowledgeRisk = async () => {
    try {
      await setSimpleSettingValue({
        key: SettingsKey.PluginRiskConsent,
        value: RiskConsent.Accepted,
      });

      // force a refresh for the backend
      window.location.href = `${getBaseUrl()}/`;
    } catch (_) {
      /* Do nothing */
    }
  };

  return (
    <CenteredLayout className="sw-h-screen sw-pt-[10vh]">
      <Helmet defer={false} title={translate('plugin_risk_consent.page')} />

      <Card
        className="sw-typo-lg sw-min-w-[500px] sw-mx-auto sw-w-[40%] sw-text-center"
        data-testid="plugin-risk-consent-page"
      >
        <Title className="sw-mb-4">{translate('plugin_risk_consent.title')}</Title>

        <p className="sw-mb-4">{translate('plugin_risk_consent.description')}</p>

        <p className="sw-mb-6">{translate('plugin_risk_consent.description2')}</p>

        <Button className="sw-my-4" onClick={acknowledgeRisk} variety={ButtonVariety.Primary}>
          {translate('plugin_risk_consent.action')}
        </Button>
      </Card>
    </CenteredLayout>
  );
}

export default whenLoggedIn(withRouter(PluginRiskConsent));
