/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import Dropdown from 'sonar-ui-common/components/controls/Dropdown';
import PlusIcon from 'sonar-ui-common/components/icons/PlusIcon';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { getComponentNavigation } from '../../../../api/nav';
import CreateFormShim from '../../../../apps/portfolio/components/CreateFormShim';
import { Router, withRouter } from '../../../../components/hoc/withRouter';
import { getExtensionStart } from '../../../../helpers/extensions';
import { getPortfolioAdminUrl, getPortfolioUrl } from '../../../../helpers/urls';
import { hasGlobalPermission } from '../../../../helpers/users';
import { ComponentQualifier } from '../../../../types/component';
import GlobalNavPlusMenu from './GlobalNavPlusMenu';
import { isSonarCloud } from "../../../../helpers/system";

interface Props {
  appState: Pick<T.AppState, 'branchesEnabled' | 'qualifiers'>;
  currentUser: T.LoggedInUser;
  router: Router;
}

interface State {
  boundAlms: Array<string>;
  creatingComponent?: ComponentQualifier;
  governanceReady: boolean;
}

export class GlobalNavPlus extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { boundAlms: [], governanceReady: false };

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

  closeComponentCreationForm = () => {
    this.setState({ creatingComponent: undefined });
  };

  handleComponentCreationClick = (qualifier: ComponentQualifier) => {
    this.setState({ creatingComponent: qualifier });
  };

  handleComponentCreate = ({ key, qualifier }: { key: string; qualifier: ComponentQualifier }) => {
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
      this.closeComponentCreationForm();
    });
  };

  render() {
    const { appState, currentUser } = this.props;
    const { boundAlms, governanceReady, creatingComponent } = this.state;
    const governanceInstalled = appState.qualifiers.includes(ComponentQualifier.Portfolio);
    const canCreateApplication =
      governanceInstalled && hasGlobalPermission(currentUser, 'applicationcreator');
    const canCreatePortfolio =
      governanceInstalled && hasGlobalPermission(currentUser, 'portfoliocreator');
    const canCreateProject = isSonarCloud() || hasGlobalPermission(currentUser, 'provisioning');
    const canCreateOrg = isSonarCloud();

    if (!canCreateProject && !canCreateApplication && !canCreatePortfolio && !canCreateOrg) {
      return null;
    }

    return (
      <>
        <Dropdown
          overlay={
            <GlobalNavPlusMenu
              canCreateApplication={canCreateApplication}
              canCreatePortfolio={canCreatePortfolio}
              canCreateProject={canCreateProject}
              canCreateOrg={canCreateOrg}
              compatibleAlms={boundAlms}
              onComponentCreationClick={this.handleComponentCreationClick}
            />
          }
          tagName="li">
          <a
            className="navbar-icon navbar-plus"
            href="#"
            title={translate('my_account.create_new_project_portfolio_or_application')}>
            <PlusIcon />
          </a>
        </Dropdown>
        {governanceReady && creatingComponent && (
          <CreateFormShim
            defaultQualifier={creatingComponent}
            onClose={this.closeComponentCreationForm}
            onCreate={this.handleComponentCreate}
          />
        )}
      </>
    );
  }
}

export default withRouter(GlobalNavPlus);
