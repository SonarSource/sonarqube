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
import classNames from 'classnames';
import * as React from 'react';
import { NavLink } from 'react-router-dom';
import { ButtonLink } from '../../../../components/controls/buttons';
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
import { Feature } from '../../../../types/features';
import { Component, Dict, Extension } from '../../../../types/types';
import withAvailableFeatures, {
  WithAvailableFeaturesProps,
} from '../../available-features/withAvailableFeatures';
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
    label: React.ReactNode;
    pathname: string;
    additionalQueryParams?: Dict<string>;
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
          <NavLink
            to={{
              pathname,
              search: new URLSearchParams({ ...query, ...additionalQueryParams }).toString(),
            }}
          >
            {label}
          </NavLink>
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
          <NavLink to={getPortfolioUrl(id)}>{translate('overview.page')}</NavLink>
        </li>
      ) : null;
    }

    const isApplicationChildInaccessble = this.isApplicationChildInaccessble();
    if (isApplicationChildInaccessble) {
      return this.renderLinkWhenInaccessibleChild(translate('overview.page'));
    }
    return (
      <li>
        <NavLink to={getProjectQueryUrl(id, branchLike)}>{translate('overview.page')}</NavLink>
      </li>
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

    const isSettingsActive = SETTINGS_URLS.some((url) => window.location.href.indexOf(url) !== -1);

    const adminLinks = this.renderAdministrationLinks(query, isProject, isApplication, isPortfolio);
    if (!adminLinks.some((link) => link != null)) {
      return null;
    }

    return (
      <Dropdown
        data-test="administration"
        overlay={<ul className="menu">{adminLinks}</ul>}
        tagName="li"
      >
        {({ onToggleClick, open }) => (
          <ButtonLink
            aria-expanded={open}
            aria-haspopup="menu"
            className={classNames('dropdown-toggle', { active: isSettingsActive || open })}
            id="component-navigation-admin"
            onClick={onToggleClick}
          >
            {hasMessage('layout.settings', component.qualifier)
              ? translate('layout.settings', component.qualifier)
              : translate('layout.settings')}
            <DropdownIcon className="little-spacer-left" />
          </ButtonLink>
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
        <li>
          <ButtonLink
            className="show-project-info-button"
            onClick={this.props.onToggleProjectInfo}
            innerRef={(node) => {
              this.projectInfoLink = node;
            }}
          >
            <BulletListIcon className="little-spacer-right" />
            {label}
          </ButtonLink>
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
        <NavLink
          to={{ pathname: '/project/settings', search: new URLSearchParams(query).toString() }}
        >
          {translate('project_settings.page')}
        </NavLink>
      </li>
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
      <li key="branches">
        <NavLink
          to={{ pathname: '/project/branches', search: new URLSearchParams(query).toString() }}
        >
          {translate('project_branch_pull_request.page')}
        </NavLink>
      </li>
    );
  };

  renderBaselineLink = (query: Query, isApplication: boolean, isPortfolio: boolean) => {
    if (!this.getConfiguration().showSettings || isApplication || isPortfolio) {
      return null;
    }
    return (
      <li key="baseline">
        <NavLink
          to={{ pathname: '/project/baseline', search: new URLSearchParams(query).toString() }}
        >
          {translate('project_baseline.page')}
        </NavLink>
      </li>
    );
  };

  renderImportExportLink = (query: Query, isProject: boolean) => {
    if (!isProject) {
      return null;
    }
    return (
      <li key="import-export">
        <NavLink
          to={{
            pathname: '/project/import_export',
            search: new URLSearchParams(query).toString(),
          }}
        >
          {translate('project_dump.page')}
        </NavLink>
      </li>
    );
  };

  renderProfilesLink = (query: Query) => {
    if (!this.getConfiguration().showQualityProfiles) {
      return null;
    }
    return (
      <li key="profiles">
        <NavLink
          to={{
            pathname: '/project/quality_profiles',
            search: new URLSearchParams(query).toString(),
          }}
        >
          {translate('project_quality_profiles.page')}
        </NavLink>
      </li>
    );
  };

  renderQualityGateLink = (query: Query) => {
    if (!this.getConfiguration().showQualityGates) {
      return null;
    }
    return (
      <li key="quality_gate">
        <NavLink
          to={{ pathname: '/project/quality_gate', search: new URLSearchParams(query).toString() }}
        >
          {translate('project_quality_gate.page')}
        </NavLink>
      </li>
    );
  };

  renderLinksLink = (query: Query) => {
    if (!this.getConfiguration().showLinks) {
      return null;
    }
    return (
      <li key="links">
        <NavLink to={{ pathname: '/project/links', search: new URLSearchParams(query).toString() }}>
          {translate('project_links.page')}
        </NavLink>
      </li>
    );
  };

  renderPermissionsLink = (query: Query) => {
    if (!this.getConfiguration().showPermissions) {
      return null;
    }
    return (
      <li key="permissions">
        <NavLink to={{ pathname: '/project_roles', search: new URLSearchParams(query).toString() }}>
          {translate('permissions.page')}
        </NavLink>
      </li>
    );
  };

  renderBackgroundTasksLink = (query: Query) => {
    if (!this.getConfiguration().showBackgroundTasks) {
      return null;
    }
    return (
      <li key="background_tasks">
        <NavLink
          to={{
            pathname: '/project/background_tasks',
            search: new URLSearchParams(query).toString(),
          }}
        >
          {translate('background_tasks.page')}
        </NavLink>
      </li>
    );
  };

  renderUpdateKeyLink = (query: Query) => {
    if (!this.getConfiguration().showUpdateKey) {
      return null;
    }
    return (
      <li key="update_key">
        <NavLink to={{ pathname: '/project/key', search: new URLSearchParams(query).toString() }}>
          {translate('update_key.page')}
        </NavLink>
      </li>
    );
  };

  renderWebhooksLink = (query: Query, isProject: boolean) => {
    if (!this.getConfiguration().showSettings || !isProject) {
      return null;
    }
    return (
      <li key="webhooks">
        <NavLink
          to={{ pathname: '/project/webhooks', search: new URLSearchParams(query).toString() }}
        >
          {translate('webhooks.page')}
        </NavLink>
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
        ComponentQualifier.Application,
      ].includes(qualifier as ComponentQualifier)
    ) {
      return null;
    }

    return (
      <li key="project_delete">
        <NavLink
          to={{ pathname: '/project/deletion', search: new URLSearchParams(query).toString() }}
        >
          {translate('deletion.page')}
        </NavLink>
      </li>
    );
  };

  renderExtension = ({ key, name }: Extension, isAdmin: boolean, baseQuery: Query) => {
    const pathname = isAdmin ? `/project/admin/extension/${key}` : `/project/extension/${key}`;
    const query = { ...baseQuery, qualifier: this.props.component.qualifier };
    return (
      <li key={key}>
        <NavLink to={{ pathname, search: new URLSearchParams(query).toString() }}>{name}</NavLink>
      </li>
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
    const extensions = this.props.component.extensions || [];
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
        overlay={
          <ul className="menu">
            {withoutSecurityExtension.map((e) => this.renderExtension(e, false, query))}
          </ul>
        }
        tagName="li"
      >
        {({ onToggleClick, open }) => (
          <ButtonLink
            aria-expanded={open}
            aria-haspopup="menu"
            className={classNames('dropdown-toggle', { active: open })}
            id="component-navigation-more"
            onClick={onToggleClick}
          >
            {translate('more')}
            <DropdownIcon className="little-spacer-left" />
          </ButtonLink>
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

export default withAvailableFeatures(Menu);
