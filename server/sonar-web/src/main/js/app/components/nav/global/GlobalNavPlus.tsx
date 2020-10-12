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
import { getAlmSettings } from '../../../../api/alm-settings';
import { getComponentNavigation } from '../../../../api/nav';
import CreateFormShim from '../../../../apps/portfolio/components/CreateFormShim';
import { Router, withRouter } from '../../../../components/hoc/withRouter';
import { getExtensionStart } from '../../../../helpers/extensions';
import { getComponentAdminUrl, getComponentOverviewUrl } from '../../../../helpers/urls';
import { hasGlobalPermission } from '../../../../helpers/users';
import { AlmKeys, AlmSettingsInstance } from '../../../../types/alm-settings';
import { ComponentQualifier } from '../../../../types/component';
import CreateApplicationForm from '../../extensions/CreateApplicationForm';
import GlobalNavPlusMenu from './GlobalNavPlusMenu';

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

/*
 * ALMs for which the import feature has been implemented
 */
const IMPORT_COMPATIBLE_ALMS = [AlmKeys.Bitbucket, AlmKeys.GitHub, AlmKeys.GitLab];

const almSettingsValidators = {
  [AlmKeys.Azure]: (_: AlmSettingsInstance) => true,
  [AlmKeys.Bitbucket]: (_: AlmSettingsInstance) => true,
  [AlmKeys.GitHub]: (_: AlmSettingsInstance) => true,
  [AlmKeys.GitLab]: (settings: AlmSettingsInstance) => !!settings.url
};

export class GlobalNavPlus extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { boundAlms: [], governanceReady: false };

  componentDidMount() {
    this.mounted = true;

    this.fetchAlmBindings();

    if (this.props.appState.qualifiers.includes(ComponentQualifier.Portfolio)) {
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

  almSettingIsValid = (settings: AlmSettingsInstance) => {
    return almSettingsValidators[settings.alm](settings);
  };

  fetchAlmBindings = async () => {
    const {
      appState: { branchesEnabled },
      currentUser
    } = this.props;
    const canCreateProject = hasGlobalPermission(currentUser, 'provisioning');

    // getAlmSettings requires branchesEnabled
    if (!canCreateProject || !branchesEnabled) {
      return;
    }

    const almSettings = await getAlmSettings();

    // Import is only available if exactly one binding is configured
    const boundAlms = IMPORT_COMPATIBLE_ALMS.filter(key => {
      const currentAlmSettings = almSettings.filter(s => s.alm === key);
      return currentAlmSettings.length === 1 && this.almSettingIsValid(currentAlmSettings[0]);
    });

    if (this.mounted) {
      this.setState({
        boundAlms
      });
    }
  };

  handleComponentCreationClick = (qualifier: ComponentQualifier) => {
    this.setState({ creatingComponent: qualifier });
  };

  handleComponentCreate = ({ key, qualifier }: { key: string; qualifier: ComponentQualifier }) => {
    return getComponentNavigation({ component: key }).then(({ configuration }) => {
      if (configuration && configuration.showSettings) {
        this.props.router.push(getComponentAdminUrl(key, qualifier));
      } else {
        this.props.router.push(getComponentOverviewUrl(key, qualifier));
      }
      this.closeComponentCreationForm();
    });
  };

  render() {
    const { appState, currentUser } = this.props;
    const { boundAlms, governanceReady, creatingComponent } = this.state;
    const canCreateApplication =
      appState.qualifiers.includes(ComponentQualifier.Application) &&
      hasGlobalPermission(currentUser, 'applicationcreator');
    const canCreatePortfolio =
      appState.qualifiers.includes(ComponentQualifier.Portfolio) &&
      hasGlobalPermission(currentUser, 'portfoliocreator');
    const canCreateProject = hasGlobalPermission(currentUser, 'provisioning');

    if (!canCreateProject && !canCreateApplication && !canCreatePortfolio) {
      return null;
    }

    return (
      <>
        <Dropdown
          onOpen={this.fetchAlmBindings}
          overlay={
            <GlobalNavPlusMenu
              canCreateApplication={canCreateApplication}
              canCreatePortfolio={canCreatePortfolio}
              canCreateProject={canCreateProject}
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

        {canCreateApplication && creatingComponent === ComponentQualifier.Application && (
          <CreateApplicationForm
            onClose={this.closeComponentCreationForm}
            onCreate={this.handleComponentCreate}
          />
        )}

        {governanceReady && creatingComponent === ComponentQualifier.Portfolio && (
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
