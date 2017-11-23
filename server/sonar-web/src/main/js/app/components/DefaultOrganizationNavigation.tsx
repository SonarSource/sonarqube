/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import { Link, IndexLink } from 'react-router';
import * as classNames from 'classnames';
import SettingsEditionsNotifContainer from './nav/settings/SettingsEditionsNotifContainer';
import { EditionStatus, getEditionStatus } from '../../api/marketplace';
import { Organization } from '../../app/types';
import ContextNavBar from '../../components/nav/ContextNavBar';
import NavBarTabs from '../../components/nav/NavBarTabs';
import OrganizationIcon from '../../components/icons-components/OrganizationIcon';
import CommonNavigation from '../../components/common/CommonNavigation';
import { translate } from '../../helpers/l10n';
import { AppState } from '../../store/appState/duck';
import { fetchEditions, setEditionStatus } from '../../store/marketplace/actions';
import {
  getAppState,
  getMarketplaceEditionStatus,
  getGlobalSettingValue
} from '../../store/rootReducer';

const PORTFOLIOS = 'governance/portfolios';
const SUPPORT = 'license/support';

interface StateProps {
  appState: AppState;
  editionStatus?: EditionStatus;
  editionsUrl: string;
}

interface DispatchProps {
  fetchEditions: (url: string, version: string) => void;
  setEditionStatus: (editionStatus: EditionStatus) => void;
}

interface Props extends StateProps, DispatchProps {
  location: { pathname: string };
  organization: Organization;
}

class DefaultOrganizationNavigation extends React.PureComponent<Props> {
  componentDidMount() {
    if (this.props.appState.canAdmin && this.props.appState.version) {
      this.props.fetchEditions(this.props.editionsUrl, this.props.appState.version);
      getEditionStatus().then(this.props.setEditionStatus, () => {});
    }
  }

  renderHeader() {
    const { organization } = this.props;
    return (
      <div className="navbar-context-header">
        <h1 className="display-inline-block">
          <OrganizationIcon className="little-spacer-right" />
          <Link to="/projects" className="link-base-color link-no-underline">
            <strong>{organization.name}</strong>
          </Link>
        </h1>
        {organization.description != null && (
          <div className="navbar-context-description">
            <p className="text-limited text-top" title={organization.description}>
              {organization.description}
            </p>
          </div>
        )}
      </div>
    );
  }

  renderMeta() {
    const { organization } = this.props;
    return (
      <div className="navbar-context-meta">
        {!!organization.avatar && (
          <img src={organization.avatar} height={30} alt={organization.name} />
        )}
        {organization.url != null && (
          <div>
            <p className="text-limited text-top">
              <a
                className="link-underline"
                href={organization.url}
                title={organization.url}
                rel="nofollow">
                {organization.url}
              </a>
            </p>
          </div>
        )}
      </div>
    );
  }

  renderExtensions() {
    const { globalPages: extensions } = this.props.appState;
    const { pathname } = this.props.location;
    const withoutPortfolios = extensions.filter(({ key }) => key !== PORTFOLIOS);
    if (withoutPortfolios.length === 0) {
      return null;
    }
    const isDropdownActive = withoutPortfolios.some(({ key }) => pathname === `extension/${key}`);
    return (
      <li className="dropdown">
        <a
          className={classNames('dropdown-toggle', { active: isDropdownActive })}
          data-toggle="dropdown"
          id="global-navigation-more"
          href="#">
          {translate('more')}
          <span className="icon-dropdown little-spacer-left" />
        </a>
        <ul className="dropdown-menu">
          {withoutPortfolios.map(({ key, name }) => (
            <li key={key}>
              <Link to={`/extension/${key}`} activeClassName="active">
                {name}
              </Link>
            </li>
          ))}
        </ul>
      </li>
    );
  }

  renderLink(path: string, label: string) {
    return (
      <li>
        <IndexLink to={path} activeClassName="active">
          {translate(label)}
        </IndexLink>
      </li>
    );
  }

  renderAdministration() {
    const { adminPages: extensions = [] } = this.props.appState;
    const { pathname } = this.props.location;
    const supportExtension = extensions.find(({ key }) => key === SUPPORT);
    const withoutSupport = extensions.filter(({ key }) => key !== SUPPORT);
    const adminActive = pathname.startsWith('admin');

    return (
      <li className="dropdown">
        <a
          className={classNames('dropdown-toggle', { active: adminActive })}
          data-toggle="dropdown"
          href="#">
          {translate('layout.settings')}
          <i className="icon-dropdown little-spacer-left" />
        </a>
        <ul className="dropdown-menu">
          <li className="dropdown-header">{translate('sidebar.project_settings')}</li>
          {this.renderLink('/admin/settings', 'settings.page')}
          {this.renderLink('/admin/settings/encryption', 'property.category.security.encryption')}
          {this.renderLink('/admin/custom_metrics', 'custom_metrics.page')}
          {withoutSupport.map(({ key, name }) => (
            <li key={key}>
              <Link to={`/admin/extension/${key}`} activeClassName="active">
                {name}
              </Link>
            </li>
          ))}

          <li className="divider" />
          <li className="dropdown-header">{translate('sidebar.security')}</li>
          {this.renderLink('/admin/users', 'users.page')}
          {this.renderLink('/admin/groups', 'user_groups.page')}
          {this.renderLink('/admin/permissions', 'global_permissions.page')}
          {this.renderLink('/admin/permission_templates', 'permission_templates')}

          <li className="divider" />
          {this.renderLink('/admin/projects_management', 'projects_management')}
          {this.renderLink('/admin/background_tasks', 'background_tasks.page')}

          <li className="divider" />
          {this.renderLink('/admin/system', 'sidebar.system')}
          {this.renderLink('/admin/marketplace', 'marketplace.page')}
          {supportExtension && this.renderLink('/admin/extension/license/support', 'support')}
        </ul>
      </li>
    );
  }

  render() {
    const { appState, organization } = this.props;
    const portfoliosExtension = appState.globalPages.find(({ key }) => key === PORTFOLIOS);
    const portfoliosLink = portfoliosExtension && (
      <li key="portfolios">
        <Link to="/portfolios" activeClassName="active">
          {translate('portfolios.page')}
        </Link>
      </li>
    );

    const { editionStatus } = this.props;
    let notification;
    if (
      editionStatus &&
      (editionStatus.installError || editionStatus.installationStatus !== 'NONE')
    ) {
      notification = <SettingsEditionsNotifContainer editionStatus={editionStatus} />;
    }

    return (
      <ContextNavBar id="global-navigation" height={notification ? 95 : 65} notif={notification}>
        {this.renderHeader()}
        {this.renderMeta()}
        <NavBarTabs>
          <CommonNavigation afterProjects={portfoliosLink} />
          {this.renderExtensions()}
          {organization.canAdmin && this.renderAdministration()}
        </NavBarTabs>
      </ContextNavBar>
    );
  }
}

const mapStateToProps = (state: any) => ({
  appState: getAppState(state),
  editionStatus: getMarketplaceEditionStatus(state),
  editionsUrl: (getGlobalSettingValue(state, 'sonar.editions.jsonUrl') || {}).value
});

const mapDispatchToProps = { fetchEditions, setEditionStatus };

export default connect<StateProps, DispatchProps>(mapStateToProps, mapDispatchToProps)(
  DefaultOrganizationNavigation
);
