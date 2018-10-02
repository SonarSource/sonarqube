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
import { Link, withRouter, WithRouterProps } from 'react-router';
import CreateFormShim from '../../../../apps/portfolio/components/CreateFormShim';
import Dropdown from '../../../../components/controls/Dropdown';
import PlusIcon from '../../../../components/icons-components/PlusIcon';
import { AppState, hasGlobalPermission, CurrentUser } from '../../../types';
import { getPortfolioAdminUrl } from '../../../../helpers/urls';
import { getExtensionStart } from '../../extensions/utils';
import { isSonarCloud } from '../../../../helpers/system';
import { translate } from '../../../../helpers/l10n';

interface Props {
  appState: Pick<AppState, 'qualifiers'>;
  currentUser: CurrentUser;
  openProjectOnboarding: () => void;
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
    this.closeCreatePortfolioForm();
    this.props.router.push(getPortfolioAdminUrl(key, qualifier));
  };

  renderCreateProject() {
    const { currentUser } = this.props;
    if (!hasGlobalPermission(currentUser, 'provisioning')) {
      return null;
    }
    return (
      <li>
        <a className="js-new-project" href="#" onClick={this.handleNewProjectClick}>
          {translate('provisioning.create_new_project')}
        </a>
      </li>
    );
  }

  renderCreateOrganization() {
    if (!isSonarCloud()) {
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
    const canCreatePortfolio = hasGlobalPermission(currentUser, 'portfoliocreator');

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
              {this.renderCreateProject()}
              {this.renderCreateOrganization()}
              {this.renderCreatePortfolio(
                canCreateApplication || canCreatePortfolio,
                defaultQualifier
              )}
            </ul>
          }
          tagName="li">
          <a
            className="navbar-plus"
            href="#"
            title={
              isSonarCloud()
                ? translate('my_account.create_new_project_or_organization')
                : translate('my_account.create_new_project_portfolio_or_application')
            }>
            <PlusIcon />
          </a>
        </Dropdown>
        {this.state.governanceReady &&
          this.state.createPortfolio && (
            <CreateFormShim
              onClose={this.closeCreatePortfolioForm}
              onCreate={this.handleCreatePortfolio}
              defaultQualifier={defaultQualifier}
            />
          )}
      </>
    );
  }
}

export default withRouter(GlobalNavPlus);
