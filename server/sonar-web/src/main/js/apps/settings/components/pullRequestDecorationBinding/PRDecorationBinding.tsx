/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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
import { connect } from 'react-redux';
import {
  deleteProjectAlmBinding,
  getAlmSettings,
  getProjectAlmBinding,
  setProjectAzureBinding,
  setProjectBitbucketBinding,
  setProjectBitbucketCloudBinding,
  setProjectGithubBinding,
  setProjectGitlabBinding,
  validateProjectAlmBinding
} from '../../../../api/alm-settings';
import throwGlobalError from '../../../../app/utils/throwGlobalError';
import { withCurrentUser } from '../../../../components/hoc/withCurrentUser';
import { hasGlobalPermission } from '../../../../helpers/users';
import { HttpStatus } from '../../../../sonar-ui-common/helpers/request';
import { getAppState, Store } from '../../../../store/rootReducer';
import {
  AlmKeys,
  AlmSettingsInstance,
  ProjectAlmBindingConfigurationErrors,
  ProjectAlmBindingResponse
} from '../../../../types/alm-settings';
import { EditionKey } from '../../../../types/editions';
import { Permissions } from '../../../../types/permissions';
import PRDecorationBindingRenderer from './PRDecorationBindingRenderer';

type FormData = T.Omit<ProjectAlmBindingResponse, 'alm'>;

interface StateProps {
  monorepoEnabled: boolean;
}

interface Props {
  component: T.Component;
  currentUser: T.CurrentUser;
}

interface State {
  formData: FormData;
  instances: AlmSettingsInstance[];
  isChanged: boolean;
  isConfigured: boolean;
  isValid: boolean;
  loading: boolean;
  originalData?: FormData;
  updating: boolean;
  successfullyUpdated: boolean;
  checkingConfiguration: boolean;
  configurationErrors?: ProjectAlmBindingConfigurationErrors;
}

const REQUIRED_FIELDS_BY_ALM: {
  [almKey in AlmKeys]: Array<keyof T.Omit<FormData, 'key'>>;
} = {
  [AlmKeys.Azure]: ['repository', 'slug'],
  [AlmKeys.BitbucketServer]: ['repository', 'slug'],
  [AlmKeys.BitbucketCloud]: ['repository'],
  [AlmKeys.GitHub]: ['repository'],
  [AlmKeys.GitLab]: ['repository']
};

export class PRDecorationBinding extends React.PureComponent<Props & StateProps, State> {
  mounted = false;
  state: State = {
    formData: { key: '', monorepo: false },
    instances: [],
    isChanged: false,
    isConfigured: false,
    isValid: false,
    loading: true,
    updating: false,
    successfullyUpdated: false,
    checkingConfiguration: false
  };

  componentDidMount() {
    this.mounted = true;
    this.fetchDefinitions();
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchDefinitions = () => {
    const project = this.props.component.key;
    return Promise.all([getAlmSettings(project), this.getProjectBinding(project)])
      .then(([instances, originalData]) => {
        if (this.mounted) {
          this.setState(({ formData }) => {
            const newFormData = originalData || formData;
            return {
              formData: newFormData,
              instances: instances || [],
              isChanged: false,
              isConfigured: !!originalData,
              isValid: this.validateForm(newFormData),
              loading: false,
              originalData: newFormData,
              configurationErrors: undefined
            };
          });
        }
      })
      .catch(() => {
        if (this.mounted) {
          this.setState({ loading: false });
        }
      })
      .then(() => this.checkConfiguration());
  };

  getProjectBinding(project: string): Promise<ProjectAlmBindingResponse | undefined> {
    return getProjectAlmBinding(project).catch((response: Response) => {
      if (response && response.status === HttpStatus.NotFound) {
        return undefined;
      }
      return throwGlobalError(response);
    });
  }

  catchError = () => {
    if (this.mounted) {
      this.setState({ updating: false });
    }
  };

  handleReset = () => {
    const { component } = this.props;
    this.setState({ updating: true });
    deleteProjectAlmBinding(component.key)
      .then(() => {
        if (this.mounted) {
          this.setState({
            formData: {
              key: '',
              repository: '',
              slug: '',
              monorepo: false
            },
            originalData: undefined,
            isChanged: false,
            isConfigured: false,
            updating: false,
            successfullyUpdated: true,
            configurationErrors: undefined
          });
        }
      })
      .catch(this.catchError);
  };

  submitProjectAlmBinding(
    alm: AlmKeys,
    key: string,
    almSpecificFields?: T.Omit<FormData, 'key'>
  ): Promise<void> {
    const almSetting = key;
    const project = this.props.component.key;
    const repository = almSpecificFields?.repository;
    const slug = almSpecificFields?.slug;
    const monorepo = almSpecificFields?.monorepo ?? false;

    if (!repository) {
      return Promise.reject();
    }

    switch (alm) {
      case AlmKeys.Azure: {
        if (!slug) {
          return Promise.reject();
        }
        return setProjectAzureBinding({
          almSetting,
          project,
          projectName: slug,
          repositoryName: repository,
          monorepo
        });
      }
      case AlmKeys.BitbucketServer: {
        if (!slug) {
          return Promise.reject();
        }
        return setProjectBitbucketBinding({
          almSetting,
          project,
          repository,
          slug,
          monorepo
        });
      }
      case AlmKeys.BitbucketCloud: {
        return setProjectBitbucketCloudBinding({
          almSetting,
          project,
          repository,
          monorepo
        });
      }
      case AlmKeys.GitHub: {
        // By default it must remain true.
        const summaryCommentEnabled = almSpecificFields?.summaryCommentEnabled ?? true;
        return setProjectGithubBinding({
          almSetting,
          project,
          repository,
          summaryCommentEnabled,
          monorepo
        });
      }

      case AlmKeys.GitLab: {
        return setProjectGitlabBinding({
          almSetting,
          project,
          repository,
          monorepo
        });
      }

      default:
        return Promise.reject();
    }
  }

  checkConfiguration = async () => {
    const {
      component: { key: projectKey }
    } = this.props;

    const { isConfigured } = this.state;

    if (!isConfigured) {
      return;
    }

    this.setState({ checkingConfiguration: true, configurationErrors: undefined });

    const configurationErrors = await validateProjectAlmBinding(projectKey).catch(error => error);

    if (this.mounted) {
      this.setState({ checkingConfiguration: false, configurationErrors });
    }
  };

  handleSubmit = () => {
    this.setState({ updating: true });
    const {
      formData: { key, ...additionalFields },
      instances
    } = this.state;

    const selected = instances.find(i => i.key === key);
    if (!key || !selected) {
      return;
    }

    this.submitProjectAlmBinding(selected.alm, key, additionalFields)
      .then(() => {
        if (this.mounted) {
          this.setState({
            updating: false,
            successfullyUpdated: true
          });
        }
      })
      .then(this.fetchDefinitions)
      .catch(this.catchError);
  };

  isDataSame(
    { key, repository = '', slug = '', summaryCommentEnabled = false, monorepo = false }: FormData,
    {
      key: oKey = '',
      repository: oRepository = '',
      slug: oSlug = '',
      summaryCommentEnabled: osummaryCommentEnabled = false,
      monorepo: omonorepo = false
    }: FormData
  ) {
    return (
      key === oKey &&
      repository === oRepository &&
      slug === oSlug &&
      summaryCommentEnabled === osummaryCommentEnabled &&
      monorepo === omonorepo
    );
  }

  handleFieldChange = (id: keyof ProjectAlmBindingResponse, value: string | boolean) => {
    this.setState(({ formData, originalData }) => {
      const newFormData = {
        ...formData,
        [id]: value
      };

      return {
        formData: newFormData,
        isValid: this.validateForm(newFormData),
        isChanged: !this.isDataSame(newFormData, originalData || { key: '', monorepo: false }),
        successfullyUpdated: false
      };
    });
  };

  validateForm = ({ key, ...additionalFields }: State['formData']) => {
    const { instances } = this.state;
    const selected = instances.find(i => i.key === key);
    if (!key || !selected) {
      return false;
    }
    return REQUIRED_FIELDS_BY_ALM[selected.alm].reduce(
      (result: boolean, field) => result && Boolean(additionalFields[field]),
      true
    );
  };

  handleCheckConfiguration = async () => {
    await this.checkConfiguration();
  };

  render() {
    const { currentUser, monorepoEnabled } = this.props;

    return (
      <PRDecorationBindingRenderer
        onFieldChange={this.handleFieldChange}
        onReset={this.handleReset}
        onSubmit={this.handleSubmit}
        onCheckConfiguration={this.handleCheckConfiguration}
        monorepoEnabled={monorepoEnabled}
        isSysAdmin={hasGlobalPermission(currentUser, Permissions.Admin)}
        {...this.state}
      />
    );
  }
}

const mapStateToProps = (state: Store): StateProps => ({
  // This feature trigger will be replaced when SONAR-14349 is implemented
  monorepoEnabled: [EditionKey.enterprise, EditionKey.datacenter].includes(
    getAppState(state).edition as EditionKey
  )
});

export default connect(mapStateToProps)(withCurrentUser(PRDecorationBinding));
