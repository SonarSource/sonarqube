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
import { Helmet } from 'react-helmet-async';
import { setSimpleSettingValue } from '../../api/settings';
import { Button } from '../../components/controls/buttons';
import { whenLoggedIn } from '../../components/hoc/whenLoggedIn';
import { Router, withRouter } from '../../components/hoc/withRouter';
import { translate } from '../../helpers/l10n';
import { getBaseUrl } from '../../helpers/system';
import { hasGlobalPermission } from '../../helpers/users';
import { Permissions } from '../../types/permissions';
import { RiskConsent } from '../../types/plugins';
import { SettingsKey } from '../../types/settings';
import { LoggedInUser } from '../../types/users';
import './PluginRiskConsent.css';

export interface PluginRiskConsentProps {
  currentUser: LoggedInUser;
  router: Router;
}

export function PluginRiskConsent(props: PluginRiskConsentProps) {
  const { router, currentUser } = props;

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
    <div className="plugin-risk-consent-page">
      <Helmet defer={false} title={translate('plugin_risk_consent.page')} />
      <div className="plugin-risk-consent-content boxed-group">
        <div className="boxed-group-inner text-center">
          <h1 className="big-spacer-bottom">{translate('plugin_risk_consent.title')}</h1>
          <p className="big big-spacer-bottom">{translate('plugin_risk_consent.description')}</p>
          <p className="big huge-spacer-bottom">{translate('plugin_risk_consent.description2')}</p>

          <Button className="big-spacer-bottom" onClick={acknowledgeRisk}>
            {translate('plugin_risk_consent.action')}
          </Button>
        </div>
      </div>
    </div>
  );
}

export default whenLoggedIn(withRouter(PluginRiskConsent));
