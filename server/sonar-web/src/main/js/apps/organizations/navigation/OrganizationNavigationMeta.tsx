/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import { Organization, HomePageType } from '../../../app/types';
import HomePageSelect from '../../../components/controls/HomePageSelect';
import { translate } from '../../../helpers/l10n';
import { getGlobalSettingValue } from '../../../store/rootReducer';

interface StateProps {
  onSonarCloud: boolean;
}

interface Props extends StateProps {
  organization: Organization;
}

export function OrganizationNavigationMeta({ onSonarCloud, organization }: Props) {
  return (
    <div className="navbar-context-meta">
      {organization.url != null && (
        <a
          className="spacer-right text-limited"
          href={organization.url}
          title={organization.url}
          rel="nofollow">
          {organization.url}
        </a>
      )}
      <div className="text-muted">
        <strong>{translate('organization.key')}:</strong> {organization.key}
      </div>
      {onSonarCloud && (
        <div className="navbar-context-meta-secondary">
          <HomePageSelect
            currentPage={{ type: HomePageType.Organization, parameter: organization.key }}
          />
        </div>
      )}
    </div>
  );
}

const mapStateToProps = (state: any): StateProps => {
  const sonarCloudSetting = getGlobalSettingValue(state, 'sonar.sonarcloud.enabled');

  return {
    onSonarCloud: Boolean(sonarCloudSetting && sonarCloudSetting.value === 'true')
  };
};

export default connect(mapStateToProps)(OrganizationNavigationMeta);
