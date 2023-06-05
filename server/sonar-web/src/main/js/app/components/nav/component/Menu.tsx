/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
  DisabledTabLink,
  Dropdown,
  ItemNavLink,
  Link,
  NavBarTabLink,
  NavBarTabs,
  PopupZLevel,
} from 'design-system';
import * as React from 'react';
import Tooltip from '../../../../components/controls/Tooltip';
import { getBranchLikeQuery, isPullRequest } from '../../../../helpers/branch-like';
import { hasMessage, translate, translateWithParameters } from '../../../../helpers/l10n';
import { getPortfolioUrl, getProjectQueryUrl } from '../../../../helpers/urls';
import { BranchLike, BranchParameters } from '../../../../types/branch-like';
import { ComponentQualifier, isPortfolioLike } from '../../../../types/component';
import { Feature } from '../../../../types/features';
import { Component, Dict, Extension } from '../../../../types/types';
import withAvailableFeatures, {
  WithAvailableFeaturesProps,
} from '../../available-features/withAvailableFeatures';

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
  '/project/webhooks',
];

interface Props extends WithAvailableFeaturesProps {
  branchLike: BranchLike | undefined;
  branchLikes: BranchLike[] | undefined;
  component: Component;
  isInProgress?: boolean;
  isPending?: boolean;
  onToggleProjectInfo: () => void;
  projectInfoDisplayed: boolean;
}

type Query = BranchParameters & { id: string };

export class Menu extends React.PureComponent<Props> {
  projectInfoLink: HTMLElement | null = null;

  componentDidUpdate(prevProps: Props) {
    if (
      prevProps.projectInfoDisplayed &&
      !this.props.projectInfoDisplayed &&
      this.projectInfoLink
    ) {
      this.projectInfoLink.focus();
    }
  }

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
      component: { extensions },
    } = this.props;

    return extensions && extensions.some((extension) => extension.key.startsWith('governance/'));
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
          )}
        >
          <a aria-disabled="true" className="disabled-link">
            {label}
          </a>
        </Tooltip>
      </li>
    );
  }

  renderMenuLink = ({
    label,
    pathname,
    additionalQueryParams = {},
  }: {
    label: string;
    pathname: string;
    additionalQueryParams?: Dict<string>;
  }) => {
    const hasAnalysis = this.hasAnalysis();
    const isApplicationChildInaccessble = this.isApplicationChildInaccessble();
    const query = this.getQuery();
    if (isApplicationChildInaccessble) {
      return this.renderLinkWhenInaccessibleChild(label);
    }
    return hasAnalysis ? (
      <NavBarTabLink
        to={{
          pathname,
          search: new URLSearchParams({ ...query, ...additionalQueryParams }).toString(),
        }}
        text={label}
      />
    ) : (
      <DisabledTabLink overlay={translate('layout.must_be_configured')} label={label} />
    );
  };

  renderDashboardLink = () => {
    const { id, ...branchLike } = this.getQuery();

    if (this.isPortfolio()) {
      return this.isGovernanceEnabled() ? (
        <NavBarTabLink to={getPortfolioUrl(id)} text={translate('overview.page')} />
      ) : null;
    }

    const isApplicationChildInaccessble = this.isApplicationChildInaccessble();
    if (isApplicationChildInaccessble) {
      return this.renderLinkWhenInaccessibleChild(translate('overview.page'));
    }
    return (
      <NavBarTabLink to={getProjectQueryUrl(id, branchLike)} text={translate('overview.page')} />
    );
  };

  renderBreakdownLink = () => {
    return this.isPortfolio() && this.isGovernanceEnabled()
      ? this.renderMenuLink({
          label: translate('portfolio_breakdown.page'),
          pathname: '/code',
        })
      : null;
  };

  renderCodeLink = () => {
    if (this.isPortfolio() || this.isDeveloper()) {
      return null;
    }

    const label = this.isApplication() ? translate('view_projects.page') : translate('code.page');

    return this.renderMenuLink({ label, pathname: '/code' });
  };

  renderActivityLink = () => {
    const { branchLike } = this.props;

    if (isPullRequest(branchLike)) {
      return null;
    }

    return this.renderMenuLink({
      label: translate('project_activity.page'),
      pathname: '/project/activity',
    });
  };

  renderIssuesLink = () => {
    return this.renderMenuLink({
      label: translate('issues.page'),
      pathname: '/project/issues',
      additionalQueryParams: { resolved: 'false' },
    });
  };

  renderComponentMeasuresLink = () => {
    return this.renderMenuLink({
      label: translate('layout.measures'),
      pathname: '/component_measures',
    });
  };

  renderSecurityHotspotsLink = () => {
    const isPortfolio = this.isPortfolio();
    return (
      !isPortfolio &&
      this.renderMenuLink({
        label: translate('layout.security_hotspots'),
        pathname: '/security_hotspots',
      })
    );
  };

  renderSecurityReports = () => {
    const { branchLike, component } = this.props;
    const { extensions = [] } = component;

    if (isPullRequest(branchLike)) {
      return null;
    }

    const hasSecurityReportsEnabled = extensions.some((extension) =>
      extension.key.startsWith('securityreport/')
    );

    if (!hasSecurityReportsEnabled) {
      return null;
    }

    return this.renderMenuLink({
      label: translate('layout.security_reports'),
      pathname: '/project/extension/securityreport/securityreport',
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

    const isSettingsActive = SETTINGS_URLS.some((url) => window.location.href.includes(url));

    const adminLinks = this.renderAdministrationLinks(query, isProject, isApplication, isPortfolio);
    if (!adminLinks.some((link) => link != null)) {
      return null;
    }

    return (
      <Dropdown
        data-test="administration"
        id="component-navigation-admin"
        size="auto"
        zLevel={PopupZLevel.Global}
        overlay={adminLinks}
      >
        {({ onToggleClick, open, a11yAttrs }) => (
          <NavBarTabLink
            active={isSettingsActive || open}
            onClick={onToggleClick}
            text={
              hasMessage('layout.settings', component.qualifier)
                ? translate('layout.settings', component.qualifier)
                : translate('layout.settings')
            }
            withChevron
            to={{}}
            {...a11yAttrs}
          />
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
      this.renderDeletionLink(query),
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
        <li className="sw-body-md sw-pb-4">
          <Link
            onClick={this.props.onToggleProjectInfo}
            preventDefault
            ref={(node: HTMLAnchorElement | null) => (this.projectInfoLink = node)}
            to={{}}
          >
            {label}
          </Link>
        </li>
      )
    );
  };

  renderSettingsLink = (query: Query, isApplication: boolean, isPortfolio: boolean) => {
    if (!this.getConfiguration().showSettings || isApplication || isPortfolio) {
      return null;
    }
    return (
      <ItemNavLink
        key="settings"
        to={{ pathname: '/project/settings', search: new URLSearchParams(query).toString() }}
      >
        {translate('project_settings.page')}
      </ItemNavLink>
    );
  };

  renderBranchesLink = (query: Query, isProject: boolean) => {
    if (
      !this.props.hasFeature(Feature.BranchSupport) ||
      !isProject ||
      !this.getConfiguration().showSettings
    ) {
      return null;
    }

    return (
      <ItemNavLink
        key="branches"
        to={{ pathname: '/project/branches', search: new URLSearchParams(query).toString() }}
      >
        {translate('project_branch_pull_request.page')}
      </ItemNavLink>
    );
  };

  renderBaselineLink = (query: Query, isApplication: boolean, isPortfolio: boolean) => {
    if (!this.getConfiguration().showSettings || isApplication || isPortfolio) {
      return null;
    }
    return (
      <ItemNavLink
        key="baseline"
        to={{ pathname: '/project/baseline', search: new URLSearchParams(query).toString() }}
      >
        {translate('project_baseline.page')}
      </ItemNavLink>
    );
  };

  renderImportExportLink = (query: Query, isProject: boolean) => {
    if (!isProject) {
      return null;
    }
    return (
      <ItemNavLink
        key="import-export"
        to={{
          pathname: '/project/import_export',
          search: new URLSearchParams(query).toString(),
        }}
      >
        {translate('project_dump.page')}
      </ItemNavLink>
    );
  };

  renderProfilesLink = (query: Query) => {
    if (!this.getConfiguration().showQualityProfiles) {
      return null;
    }
    return (
      <ItemNavLink
        key="profiles"
        to={{
          pathname: '/project/quality_profiles',
          search: new URLSearchParams(query).toString(),
        }}
      >
        {translate('project_quality_profiles.page')}
      </ItemNavLink>
    );
  };

  renderQualityGateLink = (query: Query) => {
    if (!this.getConfiguration().showQualityGates) {
      return null;
    }
    return (
      <ItemNavLink
        key="quality_gate"
        to={{ pathname: '/project/quality_gate', search: new URLSearchParams(query).toString() }}
      >
        {translate('project_quality_gate.page')}
      </ItemNavLink>
    );
  };

  renderLinksLink = (query: Query) => {
    if (!this.getConfiguration().showLinks) {
      return null;
    }
    return (
      <ItemNavLink
        key="links"
        to={{ pathname: '/project/links', search: new URLSearchParams(query).toString() }}
      >
        {translate('project_links.page')}
      </ItemNavLink>
    );
  };

  renderPermissionsLink = (query: Query) => {
    if (!this.getConfiguration().showPermissions) {
      return null;
    }
    return (
      <ItemNavLink
        key="permissions"
        to={{ pathname: '/project_roles', search: new URLSearchParams(query).toString() }}
      >
        {translate('permissions.page')}
      </ItemNavLink>
    );
  };

  renderBackgroundTasksLink = (query: Query) => {
    if (!this.getConfiguration().showBackgroundTasks) {
      return null;
    }
    return (
      <ItemNavLink
        key="background_tasks"
        to={{
          pathname: '/project/background_tasks',
          search: new URLSearchParams(query).toString(),
        }}
      >
        {translate('background_tasks.page')}
      </ItemNavLink>
    );
  };

  renderUpdateKeyLink = (query: Query) => {
    if (!this.getConfiguration().showUpdateKey) {
      return null;
    }
    return (
      <ItemNavLink
        key="update_key"
        to={{ pathname: '/project/key', search: new URLSearchParams(query).toString() }}
      >
        {translate('update_key.page')}
      </ItemNavLink>
    );
  };

  renderWebhooksLink = (query: Query, isProject: boolean) => {
    if (!this.getConfiguration().showSettings || !isProject) {
      return null;
    }
    return (
      <ItemNavLink
        key="webhooks"
        to={{ pathname: '/project/webhooks', search: new URLSearchParams(query).toString() }}
      >
        {translate('webhooks.page')}
      </ItemNavLink>
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
        ComponentQualifier.Application,
      ].includes(qualifier as ComponentQualifier)
    ) {
      return null;
    }

    return (
      <ItemNavLink
        key="project_delete"
        to={{ pathname: '/project/deletion', search: new URLSearchParams(query).toString() }}
      >
        {translate('deletion.page')}
      </ItemNavLink>
    );
  };

  renderExtension = ({ key, name }: Extension, isAdmin: boolean, baseQuery: Query) => {
    const pathname = isAdmin ? `/project/admin/extension/${key}` : `/project/extension/${key}`;
    const query = { ...baseQuery, qualifier: this.props.component.qualifier };
    return (
      <ItemNavLink key={key} to={{ pathname, search: new URLSearchParams(query).toString() }}>
        {name}
      </ItemNavLink>
    );
  };

  renderAdminExtensions = (query: Query, isApplication: boolean) => {
    const extensions = this.getConfiguration().extensions || [];
    return extensions
      .filter((e) => !isApplication || e.key !== 'governance/console')
      .map((e) => this.renderExtension(e, true, query));
  };

  renderExtensions = () => {
    const query = this.getQuery();
    const extensions = this.props.component.extensions ?? [];
    const withoutSecurityExtension = extensions.filter(
      (extension) =>
        !extension.key.startsWith('securityreport/') && !extension.key.startsWith('governance/')
    );

    if (withoutSecurityExtension.length === 0) {
      return null;
    }

    return (
      <Dropdown
        data-test="extensions"
        id="component-navigation-more"
        size="auto"
        zLevel={PopupZLevel.Global}
        overlay={withoutSecurityExtension.map((e) => this.renderExtension(e, false, query))}
      >
        {({ onToggleClick, open, a11yAttrs }) => (
          <NavBarTabLink
            active={open}
            onClick={onToggleClick}
            preventDefault
            text={translate('more')}
            withChevron
            to={{}}
            {...a11yAttrs}
          />
        )}
      </Dropdown>
    );
  };

  render() {
    return (
      <div className="sw-flex sw-justify-between sw-pt-4 it__navbar-tabs">
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

export default withAvailableFeatures(Menu);
