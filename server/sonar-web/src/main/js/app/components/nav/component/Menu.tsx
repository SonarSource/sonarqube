/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import classNames from 'classnames';
import { LocationDescriptorObject } from 'history';
import { omit } from 'lodash';
import * as React from 'react';
import { Link, LinkProps } from 'react-router';
import Dropdown from '../../../../components/controls/Dropdown';
import Tooltip from '../../../../components/controls/Tooltip';
import BulletListIcon from '../../../../components/icons/BulletListIcon';
import DropdownIcon from '../../../../components/icons/DropdownIcon';
import NavBarTabs from '../../../../components/ui/NavBarTabs';
import { getBranchLikeQuery, isPullRequest } from '../../../../helpers/branch-like';
import { hasMessage, translate, translateWithParameters } from '../../../../helpers/l10n';
import { getPortfolioUrl, getProjectQueryUrl } from '../../../../helpers/urls';
import { BranchLike, BranchParameters } from '../../../../types/branch-like';
import { ComponentQualifier, isPortfolioLike } from '../../../../types/component';
import { AppState, Component, Extension } from '../../../../types/types';
import withAppStateContext from '../../app-state/withAppStateContext';
import './Menu.css';

const SETTINGS_URLS = [
  '/project/admin',
  '/project/baseline',
  '/project/branches',
  '/project/settings',
  '/project/quality_profiles',
  '/project/quality_gate',
  '/project/links',
  '/project_roles',
  '/project/history',
  'background_tasks',
  '/project/key',
  '/project/deletion',
  '/project/webhooks'
];

interface Props {
  appState: AppState;
  branchLike: BranchLike | undefined;
  branchLikes: BranchLike[] | undefined;
  component: Component;
  isInProgress?: boolean;
  isPending?: boolean;
  onToggleProjectInfo: () => void;
}

type Query = BranchParameters & { id: string };

export class Menu extends React.PureComponent<Props> {
  hasAnalysis = () => {
    const { branchLikes = [], component, isInProgress, isPending } = this.props;
    const hasBranches = branchLikes.length > 1;
    return hasBranches || isInProgress || isPending || component.analysisDate !== undefined;
  };

  isProject = () => {
    return this.props.component.qualifier === ComponentQualifier.Project;
  };

  isDeveloper = () => {
    return this.props.component.qualifier === ComponentQualifier.Developper;
  };

  isPortfolio = () => {
    const { qualifier } = this.props.component;
    return isPortfolioLike(qualifier);
  };

  isApplication = () => {
    return this.props.component.qualifier === ComponentQualifier.Application;
  };

  isAllChildProjectAccessible = () => {
    return Boolean(this.props.component.canBrowseAllChildProjects);
  };

  isApplicationChildInaccessble = () => {
    return this.isApplication() && !this.isAllChildProjectAccessible();
  };

  isGovernanceEnabled = () => {
    const {
      component: { extensions }
    } = this.props;

    return extensions && extensions.some(extension => extension.key.startsWith('governance/'));
  };

  getConfiguration = () => {
    return this.props.component.configuration || {};
  };

  getQuery = (): Query => {
    return { id: this.props.component.key, ...getBranchLikeQuery(this.props.branchLike) };
  };

  renderLinkWhenInaccessibleChild(label: React.ReactNode) {
    return (
      <li>
        <Tooltip
          overlay={translateWithParameters(
            'layout.all_project_must_be_accessible',
            translate('qualifier', this.props.component.qualifier)
          )}>
          <a aria-disabled="true" className="disabled-link">
            {label}
          </a>
        </Tooltip>
      </li>
    );
  }

  renderMenuLink = ({
    label,
    to,
    ...props
  }: Omit<LinkProps, 'to'> & {
    label: React.ReactNode;
    to: LocationDescriptorObject;
  }) => {
    const hasAnalysis = this.hasAnalysis();
    const isApplicationChildInaccessble = this.isApplicationChildInaccessble();
    const query = this.getQuery();
    if (isApplicationChildInaccessble) {
      return this.renderLinkWhenInaccessibleChild(label);
    }
    return (
      <li>
        {hasAnalysis ? (
          <Link
            activeClassName="active"
            to={{ ...to, query: { ...query, ...to.query } }}
            {...omit(props, ['to'])}>
            {label}
          </Link>
        ) : (
          <Tooltip overlay={translate('layout.must_be_configured')}>
            <a aria-disabled="true" className="disabled-link">
              {label}
            </a>
          </Tooltip>
        )}
      </li>
    );
  };

  renderDashboardLink = () => {
    const { id, ...branchLike } = this.getQuery();

    if (this.isPortfolio()) {
      return this.isGovernanceEnabled() ? (
        <li>
          <Link activeClassName="active" to={getPortfolioUrl(id)}>
            {translate('overview.page')}
          </Link>
        </li>
      ) : null;
    }

    const isApplicationChildInaccessble = this.isApplicationChildInaccessble();
    if (isApplicationChildInaccessble) {
      return this.renderLinkWhenInaccessibleChild(translate('overview.page'));
    }
    return (
      <li>
        <Link activeClassName="active" to={getProjectQueryUrl(id, branchLike)}>
          {translate('overview.page')}
        </Link>
      </li>
    );
  };

  renderBreakdownLink = () => {
    return this.isPortfolio() && this.isGovernanceEnabled()
      ? this.renderMenuLink({
          label: translate('portfolio_breakdown.page'),
          to: { pathname: '/code' }
        })
      : null;
  };

  renderCodeLink = () => {
    if (this.isPortfolio() || this.isDeveloper()) {
      return null;
    }

    const label = this.isApplication() ? translate('view_projects.page') : translate('code.page');

    return this.renderMenuLink({ label, to: { pathname: '/code' } });
  };

  renderActivityLink = () => {
    const { branchLike } = this.props;

    if (isPullRequest(branchLike)) {
      return null;
    }

    return this.renderMenuLink({
      label: translate('project_activity.page'),
      to: { pathname: '/project/activity' }
    });
  };

  renderIssuesLink = () => {
    return this.renderMenuLink({
      label: translate('issues.page'),
      to: { pathname: '/project/issues', query: { resolved: 'false' } }
    });
  };

  renderComponentMeasuresLink = () => {
    return this.renderMenuLink({
      label: translate('layout.measures'),
      to: { pathname: '/component_measures' }
    });
  };

  renderSecurityHotspotsLink = () => {
    const isPortfolio = this.isPortfolio();
    return (
      !isPortfolio &&
      this.renderMenuLink({
        label: translate('layout.security_hotspots'),
        to: { pathname: '/security_hotspots' }
      })
    );
  };

  renderSecurityReports = () => {
    const { branchLike, component } = this.props;
    const { extensions = [] } = component;

    if (isPullRequest(branchLike)) {
      return null;
    }

    const hasSecurityReportsEnabled = extensions.some(extension =>
      extension.key.startsWith('securityreport/')
    );

    if (!hasSecurityReportsEnabled) {
      return null;
    }

    return this.renderMenuLink({
      label: translate('layout.security_reports'),
      to: { pathname: '/project/extension/securityreport/securityreport' }
    });
  };

  renderAdministration = () => {
    const { branchLike, component } = this.props;
    const isProject = this.isProject();
    const isPortfolio = this.isPortfolio();
    const isApplication = this.isApplication();
    const query = this.getQuery();

    if (!this.getConfiguration().showSettings || isPullRequest(branchLike)) {
      return null;
    }

    const isSettingsActive = SETTINGS_URLS.some(url => window.location.href.indexOf(url) !== -1);

    const adminLinks = this.renderAdministrationLinks(query, isProject, isApplication, isPortfolio);
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
            aria-haspopup="menu"
            role="button"
            className={classNames('dropdown-toggle', { active: isSettingsActive || open })}
            href="#"
            id="component-navigation-admin"
            onClick={onToggleClick}>
            {hasMessage('layout.settings', component.qualifier)
              ? translate('layout.settings', component.qualifier)
              : translate('layout.settings')}
            <DropdownIcon className="little-spacer-left" />
          </a>
        )}
      </Dropdown>
    );
  };

  renderAdministrationLinks = (
    query: Query,
    isProject: boolean,
    isApplication: boolean,
    isPortfolio: boolean
  ) => {
    return [
      this.renderSettingsLink(query, isApplication, isPortfolio),
      this.renderBranchesLink(query, isProject),
      this.renderBaselineLink(query, isApplication, isPortfolio),
      ...this.renderAdminExtensions(query, isApplication),
      this.renderImportExportLink(query, isProject),
      this.renderProfilesLink(query),
      this.renderQualityGateLink(query),
      this.renderLinksLink(query),
      this.renderPermissionsLink(query),
      this.renderBackgroundTasksLink(query),
      this.renderUpdateKeyLink(query),
      this.renderWebhooksLink(query, isProject),
      this.renderDeletionLink(query)
    ];
  };

  renderProjectInformationButton = () => {
    const isProject = this.isProject();
    const isApplication = this.isApplication();
    const label = translate(isProject ? 'project' : 'application', 'info.title');
    const isApplicationChildInaccessble = this.isApplicationChildInaccessble();

    if (isPullRequest(this.props.branchLike)) {
      return null;
    }

    if (isApplicationChildInaccessble) {
      return this.renderLinkWhenInaccessibleChild(label);
    }

    return (
      (isProject || isApplication) && (
        <li>
          <a
            className="menu-button"
            onClick={(e: React.SyntheticEvent<HTMLAnchorElement>) => {
              e.preventDefault();
              e.currentTarget.blur();
              this.props.onToggleProjectInfo();
            }}
            role="button"
            tabIndex={0}>
            <BulletListIcon className="little-spacer-right" />
            {label}
          </a>
        </li>
      )
    );
  };

  renderSettingsLink = (query: Query, isApplication: boolean, isPortfolio: boolean) => {
    if (!this.getConfiguration().showSettings || isApplication || isPortfolio) {
      return null;
    }
    return (
      <li key="settings">
        <Link activeClassName="active" to={{ pathname: '/project/settings', query }}>
          {translate('project_settings.page')}
        </Link>
      </li>
    );
  };

  renderBranchesLink = (query: Query, isProject: boolean) => {
    if (
      !this.props.appState.branchesEnabled ||
      !isProject ||
      !this.getConfiguration().showSettings
    ) {
      return null;
    }

    return (
      <li key="branches">
        <Link activeClassName="active" to={{ pathname: '/project/branches', query }}>
          {translate('project_branch_pull_request.page')}
        </Link>
      </li>
    );
  };

  renderBaselineLink = (query: Query, isApplication: boolean, isPortfolio: boolean) => {
    if (!this.getConfiguration().showSettings || isApplication || isPortfolio) {
      return null;
    }
    return (
      <li key="baseline">
        <Link activeClassName="active" to={{ pathname: '/project/baseline', query }}>
          {translate('project_baseline.page')}
        </Link>
      </li>
    );
  };

  renderImportExportLink = (query: Query, isProject: boolean) => {
    if (!isProject) {
      return null;
    }
    return (
      <li key="import-export">
        <Link activeClassName="active" to={{ pathname: '/project/import_export', query }}>
          {translate('project_dump.page')}
        </Link>
      </li>
    );
  };

  renderProfilesLink = (query: Query) => {
    if (!this.getConfiguration().showQualityProfiles) {
      return null;
    }
    return (
      <li key="profiles">
        <Link activeClassName="active" to={{ pathname: '/project/quality_profiles', query }}>
          {translate('project_quality_profiles.page')}
        </Link>
      </li>
    );
  };

  renderQualityGateLink = (query: Query) => {
    if (!this.getConfiguration().showQualityGates) {
      return null;
    }
    return (
      <li key="quality_gate">
        <Link activeClassName="active" to={{ pathname: '/project/quality_gate', query }}>
          {translate('project_quality_gate.page')}
        </Link>
      </li>
    );
  };

  renderLinksLink = (query: Query) => {
    if (!this.getConfiguration().showLinks) {
      return null;
    }
    return (
      <li key="links">
        <Link activeClassName="active" to={{ pathname: '/project/links', query }}>
          {translate('project_links.page')}
        </Link>
      </li>
    );
  };

  renderPermissionsLink = (query: Query) => {
    if (!this.getConfiguration().showPermissions) {
      return null;
    }
    return (
      <li key="permissions">
        <Link activeClassName="active" to={{ pathname: '/project_roles', query }}>
          {translate('permissions.page')}
        </Link>
      </li>
    );
  };

  renderBackgroundTasksLink = (query: Query) => {
    if (!this.getConfiguration().showBackgroundTasks) {
      return null;
    }
    return (
      <li key="background_tasks">
        <Link activeClassName="active" to={{ pathname: '/project/background_tasks', query }}>
          {translate('background_tasks.page')}
        </Link>
      </li>
    );
  };

  renderUpdateKeyLink = (query: Query) => {
    if (!this.getConfiguration().showUpdateKey) {
      return null;
    }
    return (
      <li key="update_key">
        <Link activeClassName="active" to={{ pathname: '/project/key', query }}>
          {translate('update_key.page')}
        </Link>
      </li>
    );
  };

  renderWebhooksLink = (query: Query, isProject: boolean) => {
    if (!this.getConfiguration().showSettings || !isProject) {
      return null;
    }
    return (
      <li key="webhooks">
        <Link activeClassName="active" to={{ pathname: '/project/webhooks', query }}>
          {translate('webhooks.page')}
        </Link>
      </li>
    );
  };

  renderDeletionLink = (query: Query) => {
    const { qualifier } = this.props.component;

    if (!this.getConfiguration().showSettings) {
      return null;
    }

    if (
      ![
        ComponentQualifier.Project,
        ComponentQualifier.Portfolio,
        ComponentQualifier.Application
      ].includes(qualifier as ComponentQualifier)
    ) {
      return null;
    }

    return (
      <li key="project_delete">
        <Link activeClassName="active" to={{ pathname: '/project/deletion', query }}>
          {translate('deletion.page')}
        </Link>
      </li>
    );
  };

  renderExtension = ({ key, name }: Extension, isAdmin: boolean, baseQuery: Query) => {
    const pathname = isAdmin ? `/project/admin/extension/${key}` : `/project/extension/${key}`;
    const query = { ...baseQuery, qualifier: this.props.component.qualifier };
    return (
      <li key={key}>
        <Link activeClassName="active" to={{ pathname, query }}>
          {name}
        </Link>
      </li>
    );
  };

  renderAdminExtensions = (query: Query, isApplication: boolean) => {
    const extensions = this.getConfiguration().extensions || [];
    return extensions
      .filter(e => !isApplication || e.key !== 'governance/console')
      .map(e => this.renderExtension(e, true, query));
  };

  renderExtensions = () => {
    const query = this.getQuery();
    const extensions = this.props.component.extensions || [];
    const withoutSecurityExtension = extensions.filter(
      extension =>
        !extension.key.startsWith('securityreport/') && !extension.key.startsWith('governance/')
    );

    if (withoutSecurityExtension.length === 0) {
      return null;
    }

    return (
      <Dropdown
        data-test="extensions"
        overlay={
          <ul className="menu">
            {withoutSecurityExtension.map(e => this.renderExtension(e, false, query))}
          </ul>
        }
        tagName="li">
        {({ onToggleClick, open }) => (
          <a
            aria-expanded={open}
            aria-haspopup="menu"
            role="button"
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
  };

  render() {
    return (
      <div className="display-flex-center display-flex-space-between">
        <NavBarTabs>
          {this.renderDashboardLink()}
          {this.renderBreakdownLink()}
          {this.renderIssuesLink()}
          {this.renderSecurityHotspotsLink()}
          {this.renderSecurityReports()}
          {this.renderComponentMeasuresLink()}
          {this.renderCodeLink()}
          {this.renderActivityLink()}
          {this.renderExtensions()}
        </NavBarTabs>
        <NavBarTabs>
          {this.renderAdministration()}
          {this.renderProjectInformationButton()}
        </NavBarTabs>
      </div>
    );
  }
}

export default withAppStateContext(Menu);
