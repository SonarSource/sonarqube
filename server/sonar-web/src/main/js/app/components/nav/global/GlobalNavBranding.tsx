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
import { connect } from 'react-redux';
import { Link } from 'react-router';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { getBaseUrl } from 'sonar-ui-common/helpers/urls';
import { getGlobalSettingValue, Store } from '../../../../store/rootReducer';

interface StateProps {
  customLogoUrl?: string;
  customLogoWidth?: string | number;
}

export function GlobalNavBranding({ customLogoUrl, customLogoWidth }: StateProps) {
  const title = translate('layout.sonar.slogan');
  const url = customLogoUrl || `${getBaseUrl()}/images/logo.svg?v=6.6`;
  const width = customLogoUrl ? customLogoWidth || 100 : 83;

  return (
    <Link className="navbar-brand" to="/">
      <img alt={title} height={30} src={url} title={title} width={width} />
    </Link>
  );
}

export function SonarCloudNavBranding() {
  return (
    <GlobalNavBranding
      customLogoUrl={`${getBaseUrl()}/images/sonarcloud-logo.svg`}
      customLogoWidth={105}
    />
  );
}

const mapStateToProps = (state: Store): StateProps => {
  const customLogoUrl = getGlobalSettingValue(state, 'sonar.lf.logoUrl');
  const customLogoWidth = getGlobalSettingValue(state, 'sonar.lf.logoWidthPx');
  return {
    customLogoUrl: customLogoUrl && customLogoUrl.value,
    customLogoWidth: customLogoWidth && customLogoWidth.value
  };
};

export default connect(mapStateToProps)(GlobalNavBranding);
