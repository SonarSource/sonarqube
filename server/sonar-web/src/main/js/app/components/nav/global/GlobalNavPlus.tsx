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
import { Link, withRouter, WithRouterProps } from 'react-router';
import CreateFormShim from '../../../../apps/portfolio/components/CreateFormShim';
import Dropdown from '../../../../components/controls/Dropdown';
import PlusIcon from '../../../../components/icons-components/PlusIcon';
import { getExtensionStart } from '../../extensions/utils';
import { getComponentNavigation } from '../../../../api/nav';
import { translate } from '../../../../helpers/l10n';
import { isSonarCloud } from '../../../../helpers/system';
import { getPortfolioAdminUrl, getPortfolioUrl } from '../../../../helpers/urls';
import { hasGlobalPermission } from '../../../../helpers/users';
import { OnboardingContextShape } from '../../OnboardingContext';

interface Props {
  appState: Pick<T.AppState, 'qualifiers'>;
  currentUser: T.LoggedInUser;
  openProjectOnboarding: OnboardingContextShape;
}

interface State {
  createPortfolio: boolean;
  governanceReady: boolean;
}

export class GlobalNavPlus extends React.PureComponent<Props & WithRouterProps, State> {
  mounted = false;
  state: State = { createPortfolio: false, governanceReady: false };

  componentDidMount() {
    this.mounted = true;
    if (this.props.appState.qualifiers.includes('VW')) {
      getExtensionStart('governance/console').then(
        () => {
          if (this.mounted) {
            this.setState({ governanceReady: true });
          }
        },
        () => {}
      );
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  handleNewProjectClick = (event: React.MouseEvent<HTMLAnchorElement>) => {
    event.preventDefault();
    this.props.openProjectOnboarding();
  };

  openCreatePortfolioForm = () => {
    this.setState({ createPortfolio: true });
  };

  closeCreatePortfolioForm = () => {
    this.setState({ createPortfolio: false });
  };

  handleNewPortfolioClick = (event: React.MouseEvent<HTMLAnchorElement>) => {
    event.preventDefault();
    event.currentTarget.blur();
    this.openCreatePortfolioForm();
  };

  handleCreatePortfolio = ({ key, qualifier }: { key: string; qualifier: string }) => {
    return getComponentNavigation({ component: key }).then(data => {
      if (
        data.configuration &&
        data.configuration.extensions &&
        data.configuration.extensions.find(
          (item: { key: string; name: string }) => item.key === 'governance/console'
        )
      ) {
        this.props.router.push(getPortfolioAdminUrl(key, qualifier));
      } else {
        this.props.router.push(getPortfolioUrl(key));
      }
      this.closeCreatePortfolioForm();
    });
  };

  renderCreateProject(canCreateProject: boolean) {
    if (!canCreateProject) {
      return null;
    }
    return (
      <li>
        <a className="js-new-project" href="#" onClick={this.handleNewProjectClick}>
          {isSonarCloud()
            ? translate('provisioning.analyze_new_project')
            : translate('my_account.create_new.TRK')}
        </a>
      </li>
    );
  }

  renderCreateOrganization(canCreateOrg: boolean) {
    if (!canCreateOrg) {
      return null;
    }

    return (
      <li>
        <Link className="js-new-organization" to="/create-organization">
          {translate('my_account.create_new_organization')}
        </Link>
      </li>
    );
  }

  renderCreatePortfolio(showGovernanceEntry: boolean, defaultQualifier?: string) {
    const governanceInstalled = this.props.appState.qualifiers.includes('VW');
    if (!governanceInstalled || !showGovernanceEntry) {
      return null;
    }

    return (
      <li>
        <a className="js-new-portfolio" href="#" onClick={this.handleNewPortfolioClick}>
          {defaultQualifier
            ? translate('my_account.create_new', defaultQualifier)
            : translate('my_account.create_new_portfolio_application')}
        </a>
      </li>
    );
  }

  render() {
    const { currentUser } = this.props;
    const canCreateApplication = hasGlobalPermission(currentUser, 'applicationcreator');
    const canCreateOrg = isSonarCloud();
    const canCreatePortfolio = hasGlobalPermission(currentUser, 'portfoliocreator');
    const canCreateProject = isSonarCloud() || hasGlobalPermission(currentUser, 'provisioning');

    if (!canCreateProject && !canCreateApplication && !canCreatePortfolio && !canCreateOrg) {
      return null;
    }

    let defaultQualifier: string | undefined;
    if (!canCreateApplication) {
      defaultQualifier = 'VW';
    } else if (!canCreatePortfolio) {
      defaultQualifier = 'APP';
    }

    return (
      <>
        <Dropdown
          overlay={
            <ul className="menu">
              {this.renderCreateProject(canCreateProject)}
              {this.renderCreateOrganization(canCreateOrg)}
              {this.renderCreatePortfolio(
                canCreateApplication || canCreatePortfolio,
                defaultQualifier
              )}
            </ul>
          }
          tagName="li">
          <a
            className="navbar-icon navbar-plus"
            href="#"
            title={
              isSonarCloud()
                ? translate('my_account.create_new_project_or_organization')
                : translate('my_account.create_new_project_portfolio_or_application')
            }>
            <PlusIcon />
          </a>
        </Dropdown>
        {this.state.governanceReady && this.state.createPortfolio && (
          <CreateFormShim
            defaultQualifier={defaultQualifier}
            onClose={this.closeCreatePortfolioForm}
            onCreate={this.handleCreatePortfolio}
          />
        )}
      </>
    );
  }
}

export default withRouter(GlobalNavPlus);
