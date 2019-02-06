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
import * as classNames from 'classnames';
import Dropdown from '../../../../components/controls/Dropdown';
import NavBarTabs from '../../../../components/nav/NavBarTabs';
import {
  isShortLivingBranch,
  isPullRequest,
  isMainBranch,
  getBranchLikeQuery
} from '../../../../helpers/branches';
import { translate } from '../../../../helpers/l10n';
import DropdownIcon from '../../../../components/icons-components/DropdownIcon';
import { withAppState } from '../../../../components/hoc/withAppState';
import { isSonarCloud } from '../../../../helpers/system';

const SETTINGS_URLS = [
  '/project/admin',
  '/project/branches',
  '/project/settings',
  '/project/quality_profiles',
  '/project/quality_gate',
  '/custom_measures',
  '/project/links',
  '/project_roles',
  '/project/history',
  'background_tasks',
  '/project/key',
  '/project/deletion',
  '/project/webhooks'
];

interface Props {
  appState: Pick<T.AppState, 'branchesEnabled'>;
  branchLike: T.BranchLike | undefined;
  component: T.Component;
  location?: any;
}

export class ComponentNavMenu extends React.PureComponent<Props> {
  isProject() {
    return this.props.component.qualifier === 'TRK';
  }

  isDeveloper() {
    return this.props.component.qualifier === 'DEV';
  }

  isPortfolio() {
    const { qualifier } = this.props.component;
    return qualifier === 'VW' || qualifier === 'SVW';
  }

  isApplication() {
    return this.props.component.qualifier === 'APP';
  }

  getConfiguration() {
    return this.props.component.configuration || {};
  }

  getQuery = () => {
    return { id: this.props.component.key, ...getBranchLikeQuery(this.props.branchLike) };
  };

  renderDashboardLink() {
    const pathname = this.isPortfolio() ? '/portfolio' : '/dashboard';
    return (
      <li>
        <Link activeClassName="active" to={{ pathname, query: this.getQuery() }}>
          {translate('overview.page')}
        </Link>
      </li>
    );
  }

  renderCodeLink() {
    if (this.isDeveloper()) {
      return null;
    }

    return (
      <li>
        <Link activeClassName="active" to={{ pathname: '/code', query: this.getQuery() }}>
          {this.isPortfolio() || this.isApplication()
            ? translate('view_projects.page')
            : translate('code.page')}
        </Link>
      </li>
    );
  }

  renderActivityLink() {
    const { branchLike } = this.props;

    if (isShortLivingBranch(branchLike) || isPullRequest(branchLike)) {
      return null;
    }

    return (
      <li>
        <Link
          activeClassName="active"
          to={{ pathname: '/project/activity', query: this.getQuery() }}>
          {translate('project_activity.page')}
        </Link>
      </li>
    );
  }

  renderIssuesLink() {
    const { location = { pathname: '' } } = this.props;
    const isIssuesActive = location.pathname.startsWith('/project/issues');
    return (
      <li>
        <Link
          activeClassName="active"
          className={classNames({ active: isIssuesActive })}
          to={{ pathname: '/project/issues', query: { ...this.getQuery(), resolved: 'false' } }}>
          {translate('issues.page')}
        </Link>
      </li>
    );
  }

  renderComponentMeasuresLink() {
    return (
      <li>
        <Link
          activeClassName="active"
          to={{ pathname: '/component_measures', query: this.getQuery() }}>
          {translate('layout.measures')}
        </Link>
      </li>
    );
  }

  renderSecurityReportsLink() {
    return (
      <ul className="menu">
        <li>
          <Link
            activeClassName="active"
            to={{ pathname: '/project/security_reports/owasp_top_10', query: this.getQuery() }}>
            {translate('security_reports.owaspTop10.page')}
          </Link>
        </li>
        <li>
          <Link
            activeClassName="active"
            to={{ pathname: '/project/security_reports/sans_top_25', query: this.getQuery() }}>
            {translate('security_reports.sansTop25.page')}
          </Link>
        </li>
      </ul>
    );
  }

  renderSecurityReports() {
    const { branchLike } = this.props;

    if (isShortLivingBranch(branchLike) || isPullRequest(branchLike)) {
      return null;
    }

    const { location = { pathname: '' } } = this.props;
    const isActive = location.pathname.startsWith('/project/security_reports');
    return (
      <Dropdown overlay={this.renderSecurityReportsLink()} tagName="li">
        {({ onToggleClick, open }) => (
          <a
            aria-expanded={open}
            aria-haspopup="true"
            className={classNames('dropdown-toggle', { active: isActive || open })}
            href="#"
            onClick={onToggleClick}>
            {translate('layout.security_reports')}
            <DropdownIcon className="little-spacer-left" />
          </a>
        )}
      </Dropdown>
    );
  }

  renderAdministration() {
    const { branchLike } = this.props;

    if (!this.getConfiguration().showSettings || (branchLike && !isMainBranch(branchLike))) {
      return null;
    }

    const isSettingsActive = SETTINGS_URLS.some(url => window.location.href.indexOf(url) !== -1);

    const adminLinks = this.renderAdministrationLinks();
    if (!adminLinks.some(link => link != null)) {
      return null;
    }

    return (
      <Dropdown
        data-test="administration"
        overlay={<ul className="menu">{adminLinks}</ul>}
        tagName="li">
        {({ onToggleClick, open }) => (
          <a
            aria-expanded={open}
            aria-haspopup="true"
            className={classNames('dropdown-toggle', { active: isSettingsActive || open })}
            href="#"
            id="component-navigation-admin"
            onClick={onToggleClick}>
            {translate('layout.settings')}
            <DropdownIcon className="little-spacer-left" />
          </a>
        )}
      </Dropdown>
    );
  }

  renderAdministrationLinks() {
    return [
      this.renderSettingsLink(),
      this.renderBranchesLink(),
      this.renderProfilesLink(),
      this.renderQualityGateLink(),
      this.renderCustomMeasuresLink(),
      this.renderLinksLink(),
      this.renderPermissionsLink(),
      this.renderBackgroundTasksLink(),
      this.renderUpdateKeyLink(),
      this.renderWebhooksLink(),
      ...this.renderAdminExtensions(),
      this.renderDeletionLink()
    ];
  }

  renderSettingsLink() {
    if (!this.getConfiguration().showSettings || this.isApplication() || this.isPortfolio()) {
      return null;
    }
    return (
      <li key="settings">
        <Link
          activeClassName="active"
          to={{ pathname: '/project/settings', query: this.getQuery() }}>
          {translate('project_settings.page')}
        </Link>
      </li>
    );
  }

  renderBranchesLink() {
    if (
      !this.props.appState.branchesEnabled ||
      !this.isProject() ||
      !this.getConfiguration().showSettings
    ) {
      return null;
    }

    return (
      <li key="branches">
        <Link
          activeClassName="active"
          to={{ pathname: '/project/branches', query: { id: this.props.component.key } }}>
          {translate('project_branches.page')}
        </Link>
      </li>
    );
  }

  renderProfilesLink() {
    if (!this.getConfiguration().showQualityProfiles) {
      return null;
    }
    return (
      <li key="profiles">
        <Link
          activeClassName="active"
          to={{ pathname: '/project/quality_profiles', query: { id: this.props.component.key } }}>
          {translate('project_quality_profiles.page')}
        </Link>
      </li>
    );
  }

  renderQualityGateLink() {
    if (!this.getConfiguration().showQualityGates) {
      return null;
    }
    return (
      <li key="quality_gate">
        <Link
          activeClassName="active"
          to={{ pathname: '/project/quality_gate', query: { id: this.props.component.key } }}>
          {translate('project_quality_gate.page')}
        </Link>
      </li>
    );
  }

  renderCustomMeasuresLink() {
    if (isSonarCloud() || !this.getConfiguration().showManualMeasures) {
      return null;
    }
    return (
      <li key="custom_measures">
        <Link
          activeClassName="active"
          to={{ pathname: '/custom_measures', query: { id: this.props.component.key } }}>
          {translate('custom_measures.page')}
        </Link>
      </li>
    );
  }

  renderLinksLink() {
    if (!this.getConfiguration().showLinks) {
      return null;
    }
    return (
      <li key="links">
        <Link
          activeClassName="active"
          to={{ pathname: '/project/links', query: { id: this.props.component.key } }}>
          {translate('project_links.page')}
        </Link>
      </li>
    );
  }

  renderPermissionsLink() {
    if (!this.getConfiguration().showPermissions) {
      return null;
    }
    return (
      <li key="permissions">
        <Link
          activeClassName="active"
          to={{ pathname: '/project_roles', query: { id: this.props.component.key } }}>
          {translate('permissions.page')}
        </Link>
      </li>
    );
  }

  renderBackgroundTasksLink() {
    if (!this.getConfiguration().showBackgroundTasks) {
      return null;
    }
    return (
      <li key="background_tasks">
        <Link
          activeClassName="active"
          to={{ pathname: '/project/background_tasks', query: { id: this.props.component.key } }}>
          {translate('background_tasks.page')}
        </Link>
      </li>
    );
  }

  renderUpdateKeyLink() {
    if (!this.getConfiguration().showUpdateKey) {
      return null;
    }
    return (
      <li key="update_key">
        <Link
          activeClassName="active"
          to={{ pathname: '/project/key', query: { id: this.props.component.key } }}>
          {translate('update_key.page')}
        </Link>
      </li>
    );
  }

  renderWebhooksLink() {
    if (!this.getConfiguration().showSettings || !this.isProject()) {
      return null;
    }
    return (
      <li key="webhooks">
        <Link
          activeClassName="active"
          to={{ pathname: '/project/webhooks', query: { id: this.props.component.key } }}>
          {translate('webhooks.page')}
        </Link>
      </li>
    );
  }

  renderDeletionLink() {
    const { qualifier } = this.props.component;

    if (!this.getConfiguration().showSettings) {
      return null;
    }

    if (qualifier !== 'TRK' && qualifier !== 'VW' && qualifier !== 'APP') {
      return null;
    }

    return (
      <li key="project_delete">
        <Link
          activeClassName="active"
          to={{ pathname: '/project/deletion', query: { id: this.props.component.key } }}>
          {translate('deletion.page')}
        </Link>
      </li>
    );
  }

  renderExtension = ({ key, name }: T.Extension, isAdmin: boolean) => {
    const pathname = isAdmin ? `/project/admin/extension/${key}` : `/project/extension/${key}`;
    const query = { id: this.props.component.key, qualifier: this.props.component.qualifier };
    return (
      <li key={key}>
        <Link activeClassName="active" to={{ pathname, query }}>
          {name}
        </Link>
      </li>
    );
  };

  renderAdminExtensions() {
    if (this.props.branchLike && !isMainBranch(this.props.branchLike)) {
      return [];
    }
    const extensions = this.getConfiguration().extensions || [];
    return extensions.map(e => this.renderExtension(e, true));
  }

  renderExtensions() {
    const extensions = this.props.component.extensions || [];
    if (!extensions.length || (this.props.branchLike && !isMainBranch(this.props.branchLike))) {
      return null;
    }

    return (
      <Dropdown
        data-test="extensions"
        overlay={<ul className="menu">{extensions.map(e => this.renderExtension(e, false))}</ul>}
        tagName="li">
        {({ onToggleClick, open }) => (
          <a
            aria-expanded={open}
            aria-haspopup="true"
            className={classNames('dropdown-toggle', { active: open })}
            href="#"
            id="component-navigation-more"
            onClick={onToggleClick}>
            {translate('more')}
            <DropdownIcon className="little-spacer-left" />
          </a>
        )}
      </Dropdown>
    );
  }

  render() {
    return (
      <NavBarTabs>
        {this.renderDashboardLink()}
        {this.renderIssuesLink()}
        {this.renderSecurityReports()}
        {this.renderComponentMeasuresLink()}
        {this.renderCodeLink()}
        {this.renderActivityLink()}
        {this.renderAdministration()}
        {this.renderExtensions()}
      </NavBarTabs>
    );
  }
}

export default withAppState(ComponentNavMenu);
