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
import { LargeCenteredLayout } from 'design-system';
import * as React from 'react';
import { Helmet } from 'react-helmet-async';
import A11ySkipTarget from '~sonar-aligned/components/a11y/A11ySkipTarget';
import { withRouter } from '~sonar-aligned/components/hoc/withRouter';
import { Location, Router } from '~sonar-aligned/types/router';
import withAvailableFeatures, {
  WithAvailableFeaturesProps,
} from '../../../app/components/available-features/withAvailableFeatures';
import { translate } from '../../../helpers/l10n';
import { AlmKeys } from '../../../types/alm-settings';
import { DopSetting } from '../../../types/dop-translation';
import { Feature } from '../../../types/features';
import { CreateProjectModes } from './types';
import CreateProjectPageSonarCloud from "./CreateProjectPageSonarCloud";

export interface CreateProjectPageProps extends WithAvailableFeaturesProps {
  location: Location;
  router: Router;
}

interface State {
  azureSettings: DopSetting[];
  bitbucketCloudSettings: DopSetting[];
  bitbucketSettings: DopSetting[];
  creatingAlmDefinition?: AlmKeys;
  githubSettings: DopSetting[];
  gitlabSettings: DopSetting[];
  importProjects?: ImportProjectParam;
  loading: boolean;
  redirectTo: string;
}

export type ImportProjectParam =
  | {
      almSetting: string;
      creationMode: CreateProjectModes.AzureDevOps;
      monorepo: false;
      projects: {
        projectName: string;
        repositoryName: string;
      }[];
    }
  | {
      almSetting: string;
      creationMode: CreateProjectModes.BitbucketCloud;
      monorepo: false;
      projects: {
        repositorySlug: string;
      }[];
    }
  | {
      almSetting: string;
      creationMode: CreateProjectModes.BitbucketServer;
      monorepo: false;
      projects: {
        projectKey: string;
        repositorySlug: string;
      }[];
    }
  | {
      almSetting: string;
      creationMode: CreateProjectModes.GitHub;
      monorepo: false;
      projects: {
        repositoryKey: string;
      }[];
    }
  | {
      almSetting: string;
      creationMode: CreateProjectModes.GitLab;
      monorepo: false;
      projects: {
        gitlabProjectId: string;
      }[];
    }
  | {
      creationMode: CreateProjectModes.Manual;
      monorepo: false;
      projects: {
        mainBranch: string;
        name: string;
        project: string;
      }[];
    }
  | {
      creationMode: CreateProjectModes;
      devOpsPlatformSettingId: string;
      monorepo: true;
      projectIdentifier?: string;
      projects: {
        projectKey: string;
        projectName: string;
      }[];
      repositoryIdentifier: string;
    };

export class CreateProjectPage extends React.PureComponent<CreateProjectPageProps, State> {
  mounted = false;

  state: State = {
    loading: true,
    redirectTo: this.props.location.state?.from || '/projects',
  };

  componentDidMount() {
    this.mounted = true;

    this.cleanQueryParameters();
  }

  componentDidUpdate(prevProps: CreateProjectPageProps) {
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  cleanQueryParameters() {
    const { location, router } = this.props;

    const isMonorepoSupported = this.props.hasFeature(Feature.MonoRepositoryPullRequestDecoration);

    if (location.query?.mono === 'true' && !isMonorepoSupported) {
      // Timeout is required to force the refresh of the URL
      setTimeout(() => {
        location.query.mono = undefined;
        router.replace(location);
      }, 0);
    }
    if (location.query?.setncd === 'true') {
      // Timeout is required to force the refresh of the URL
      setTimeout(() => {
        location.query.setncd = undefined;
        router.replace(location);
      }, 0);
    }
  }

  render() {
    const { location } = this.props;
    const { importProjects, redirectTo } = this.state;
    const mode: CreateProjectModes | undefined = location.query?.mode;
    const isProjectSetupDone = location.query?.setncd === 'true';
    const gridLayoutStyle = mode ? 'sw-col-start-2 sw-col-span-10' : 'sw-col-span-12';
    const pageTitle = mode
      ? translate(`onboarding.create_project.${mode}.title`)
      : translate('onboarding.create_project.select_method');

    return (
      <LargeCenteredLayout
        id="create-project"
        className="sw-pt-8 sw-grid sw-gap-x-12 sw-gap-y-6 sw-grid-cols-12"
      >
        <div className={gridLayoutStyle}>
          <Helmet title={pageTitle} titleTemplate="%s" />
          <A11ySkipTarget anchor="create_project_main" />

          <CreateProjectPageSonarCloud />
        </div>
      </LargeCenteredLayout>
    );
  }
}

export default withRouter(withAvailableFeatures(CreateProjectPage));
