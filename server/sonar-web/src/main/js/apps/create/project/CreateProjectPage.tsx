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
import * as React from 'react';
import { Helmet } from 'react-helmet-async';
import { getAlmSettings } from '../../../api/alm-settings';
import withAppStateContext from '../../../app/components/app-state/withAppStateContext';
import withAvailableFeatures, {
  WithAvailableFeaturesProps,
} from '../../../app/components/available-features/withAvailableFeatures';
import A11ySkipTarget from '../../../components/a11y/A11ySkipTarget';
import { Location, Router, withRouter } from '../../../components/hoc/withRouter';
import { translate } from '../../../helpers/l10n';
import { getProjectUrl } from '../../../helpers/urls';
import { AlmKeys, AlmSettingsInstance } from '../../../types/alm-settings';
import { AppState } from '../../../types/appstate';
import { Feature } from '../../../types/features';
import AlmBindingDefinitionForm from '../../settings/components/almIntegration/AlmBindingDefinitionForm';
import AzureProjectCreate from './AzureProjectCreate';
import BitbucketCloudProjectCreate from './BitbucketCloudProjectCreate';
import BitbucketProjectCreate from './BitbucketProjectCreate';
import CreateProjectModeSelection from './CreateProjectModeSelection';
import GitHubProjectCreate from './GitHubProjectCreate';
import GitlabProjectCreate from './GitlabProjectCreate';
import ManualProjectCreate from './ManualProjectCreate';
import './style.css';
import { CreateProjectModes } from './types';

export interface CreateProjectPageProps extends WithAvailableFeaturesProps {
  appState: AppState;
  location: Location;
  router: Router;
}

interface State {
  azureSettings: AlmSettingsInstance[];
  bitbucketSettings: AlmSettingsInstance[];
  bitbucketCloudSettings: AlmSettingsInstance[];
  githubSettings: AlmSettingsInstance[];
  gitlabSettings: AlmSettingsInstance[];
  loading: boolean;
  creatingAlmDefinition?: AlmKeys;
}

const PROJECT_MODE_FOR_ALM_KEY = {
  [AlmKeys.Azure]: CreateProjectModes.AzureDevOps,
  [AlmKeys.BitbucketCloud]: CreateProjectModes.BitbucketCloud,
  [AlmKeys.BitbucketServer]: CreateProjectModes.BitbucketServer,
  [AlmKeys.GitHub]: CreateProjectModes.GitHub,
  [AlmKeys.GitLab]: CreateProjectModes.GitLab,
};

export class CreateProjectPage extends React.PureComponent<CreateProjectPageProps, State> {
  mounted = false;
  state: State = {
    azureSettings: [],
    bitbucketSettings: [],
    bitbucketCloudSettings: [],
    githubSettings: [],
    gitlabSettings: [],
    loading: true,
  };

  componentDidMount() {
    this.mounted = true;
    this.fetchAlmBindings();
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchAlmBindings = () => {
    this.setState({ loading: true });
    return getAlmSettings()
      .then((almSettings) => {
        if (this.mounted) {
          this.setState({
            azureSettings: almSettings.filter((s) => s.alm === AlmKeys.Azure),
            bitbucketSettings: almSettings.filter((s) => s.alm === AlmKeys.BitbucketServer),
            bitbucketCloudSettings: almSettings.filter((s) => s.alm === AlmKeys.BitbucketCloud),
            githubSettings: almSettings.filter((s) => s.alm === AlmKeys.GitHub),
            gitlabSettings: almSettings.filter((s) => s.alm === AlmKeys.GitLab),
            loading: false,
          });
        }
      })
      .catch(() => {
        if (this.mounted) {
          this.setState({ loading: false });
        }
      });
  };

  handleModeSelect = (mode: CreateProjectModes) => {
    const { router, location } = this.props;
    router.push({
      pathname: location.pathname,
      query: { mode },
    });
  };

  handleModeConfig = (alm: AlmKeys) => {
    this.setState({ creatingAlmDefinition: alm });
  };

  handleProjectCreate = (projectKey: string) => {
    this.props.router.push(getProjectUrl(projectKey));
  };

  handleOnCancelCreation = () => {
    this.setState({ creatingAlmDefinition: undefined });
  };

  handleAfterSubmit = async () => {
    let { creatingAlmDefinition: createdAlmDefinition } = this.state;

    this.setState({ creatingAlmDefinition: undefined });

    await this.fetchAlmBindings();

    if (this.mounted && createdAlmDefinition) {
      const { bitbucketCloudSettings } = this.state;

      if (createdAlmDefinition === AlmKeys.BitbucketServer && bitbucketCloudSettings.length > 0) {
        createdAlmDefinition = AlmKeys.BitbucketCloud;
      }

      this.handleModeSelect(PROJECT_MODE_FOR_ALM_KEY[createdAlmDefinition]);
    }
  };

  renderProjectCreation(mode?: CreateProjectModes) {
    const {
      appState: { canAdmin },
      location,
      router,
    } = this.props;
    const {
      azureSettings,
      bitbucketSettings,
      bitbucketCloudSettings,
      githubSettings,
      gitlabSettings,
      loading,
    } = this.state;
    const branchSupportEnabled = this.props.hasFeature(Feature.BranchSupport);

    switch (mode) {
      case CreateProjectModes.AzureDevOps: {
        return (
          <AzureProjectCreate
            canAdmin={!!canAdmin}
            loadingBindings={loading}
            location={location}
            onProjectCreate={this.handleProjectCreate}
            router={router}
            almInstances={azureSettings}
          />
        );
      }
      case CreateProjectModes.BitbucketServer: {
        return (
          <BitbucketProjectCreate
            canAdmin={!!canAdmin}
            almInstances={bitbucketSettings}
            loadingBindings={loading}
            location={location}
            onProjectCreate={this.handleProjectCreate}
            router={router}
          />
        );
      }
      case CreateProjectModes.BitbucketCloud: {
        return (
          <BitbucketCloudProjectCreate
            canAdmin={!!canAdmin}
            loadingBindings={loading}
            location={location}
            onProjectCreate={this.handleProjectCreate}
            router={router}
            almInstances={bitbucketCloudSettings}
          />
        );
      }
      case CreateProjectModes.GitHub: {
        return (
          <GitHubProjectCreate
            canAdmin={!!canAdmin}
            loadingBindings={loading}
            location={location}
            onProjectCreate={this.handleProjectCreate}
            router={router}
            almInstances={githubSettings}
          />
        );
      }
      case CreateProjectModes.GitLab: {
        return (
          <GitlabProjectCreate
            canAdmin={!!canAdmin}
            loadingBindings={loading}
            location={location}
            onProjectCreate={this.handleProjectCreate}
            router={router}
            almInstances={gitlabSettings}
          />
        );
      }
      case CreateProjectModes.Manual: {
        return (
          <ManualProjectCreate
            branchesEnabled={branchSupportEnabled}
            onProjectCreate={this.handleProjectCreate}
          />
        );
      }
      default: {
        const almCounts = {
          [AlmKeys.Azure]: azureSettings.length,
          [AlmKeys.BitbucketServer]: bitbucketSettings.length,
          [AlmKeys.BitbucketCloud]: bitbucketCloudSettings.length,
          [AlmKeys.GitHub]: githubSettings.length,
          [AlmKeys.GitLab]: gitlabSettings.length,
        };
        return (
          <CreateProjectModeSelection
            almCounts={almCounts}
            loadingBindings={loading}
            onSelectMode={this.handleModeSelect}
            onConfigMode={this.handleModeConfig}
          />
        );
      }
    }
  }

  render() {
    const { location } = this.props;
    const { creatingAlmDefinition } = this.state;
    const mode: CreateProjectModes | undefined = location.query?.mode;

    return (
      <>
        <Helmet title={translate('onboarding.create_project.select_method')} titleTemplate="%s" />
        <A11ySkipTarget anchor="create_project_main" />
        <div className="page page-limited huge-spacer-bottom position-relative" id="create-project">
          {this.renderProjectCreation(mode)}
          {creatingAlmDefinition && (
            <AlmBindingDefinitionForm
              alm={creatingAlmDefinition}
              onCancel={this.handleOnCancelCreation}
              afterSubmit={this.handleAfterSubmit}
              enforceValidation={true}
            />
          )}
        </div>
      </>
    );
  }
}

export default withRouter(withAvailableFeatures(withAppStateContext(CreateProjectPage)));
