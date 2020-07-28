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
import {
  deleteProjectAlmBinding,
  getAlmSettings,
  getProjectAlmBinding,
  setProjectAzureBinding,
  setProjectBitbucketBinding,
  setProjectGithubBinding,
  setProjectGitlabBinding
} from '../../../../api/alm-settings';
import throwGlobalError from '../../../../app/utils/throwGlobalError';
import {
  AlmKeys,
  AlmSettingsInstance,
  ProjectAlmBindingResponse
} from '../../../../types/alm-settings';
import PRDecorationBindingRenderer from './PRDecorationBindingRenderer';

type FormData = T.Omit<ProjectAlmBindingResponse, 'alm'>;

interface Props {
  component: T.Component;
}

interface State {
  formData: FormData;
  instances: AlmSettingsInstance[];
  isChanged: boolean;
  isConfigured: boolean;
  isValid: boolean;
  loading: boolean;
  orignalData?: FormData;
  saving: boolean;
  success: boolean;
}

const REQUIRED_FIELDS_BY_ALM: {
  [almKey in AlmKeys]: Array<keyof T.Omit<FormData, 'key'>>;
} = {
  [AlmKeys.Azure]: [],
  [AlmKeys.Bitbucket]: ['repository', 'slug'],
  [AlmKeys.GitHub]: ['repository'],
  [AlmKeys.GitLab]: ['repository']
};

export default class PRDecorationBinding extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = {
    formData: { key: '' },
    instances: [],
    isChanged: false,
    isConfigured: false,
    isValid: false,
    loading: true,
    saving: false,
    success: false
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
              orignalData: newFormData
            };
          });
        }
      })
      .catch(() => {
        if (this.mounted) {
          this.setState({ loading: false });
        }
      });
  };

  getProjectBinding(project: string): Promise<ProjectAlmBindingResponse | undefined> {
    return getProjectAlmBinding(project).catch((response: Response) => {
      if (response && response.status === 404) {
        return undefined;
      }
      return throwGlobalError(response);
    });
  }

  catchError = () => {
    if (this.mounted) {
      this.setState({ saving: false });
    }
  };

  handleReset = () => {
    const { component } = this.props;
    this.setState({ saving: true });
    deleteProjectAlmBinding(component.key)
      .then(() => {
        if (this.mounted) {
          this.setState({
            formData: {
              key: '',
              repository: '',
              slug: ''
            },
            isChanged: false,
            isConfigured: false,
            saving: false,
            success: true
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

    switch (alm) {
      case AlmKeys.Azure:
        return setProjectAzureBinding({
          almSetting,
          project
        });
      case AlmKeys.Bitbucket: {
        if (!almSpecificFields) {
          return Promise.reject();
        }
        const { repository = '', slug = '' } = almSpecificFields;
        return setProjectBitbucketBinding({
          almSetting,
          project,
          repository,
          slug
        });
      }
      case AlmKeys.GitHub: {
        const repository = almSpecificFields?.repository;
        // By default it must remain true.
        const summaryCommentEnabled =
          almSpecificFields?.summaryCommentEnabled === undefined
            ? true
            : almSpecificFields?.summaryCommentEnabled;
        if (!repository) {
          return Promise.reject();
        }
        return setProjectGithubBinding({
          almSetting,
          project,
          repository,
          summaryCommentEnabled
        });
      }

      case AlmKeys.GitLab: {
        const repository = almSpecificFields && almSpecificFields.repository;
        return setProjectGitlabBinding({
          almSetting,
          project,
          repository
        });
      }

      default:
        return Promise.reject();
    }
  }

  handleSubmit = () => {
    this.setState({ saving: true });
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
            saving: false,
            success: true
          });
        }
      })
      .then(this.fetchDefinitions)
      .catch(this.catchError);
  };

  isDataSame(
    { key, repository = '', slug = '', summaryCommentEnabled = false }: FormData,
    {
      key: oKey = '',
      repository: oRepository = '',
      slug: oSlug = '',
      summaryCommentEnabled: osummaryCommentEnabled = false
    }: FormData
  ) {
    return (
      key === oKey &&
      repository === oRepository &&
      slug === oSlug &&
      summaryCommentEnabled === osummaryCommentEnabled
    );
  }

  handleFieldChange = (id: keyof ProjectAlmBindingResponse, value: string | boolean) => {
    this.setState(({ formData, orignalData }) => {
      const newFormData = {
        ...formData,
        [id]: value
      };
      return {
        formData: newFormData,
        isValid: this.validateForm(newFormData),
        isChanged: !this.isDataSame(newFormData, orignalData || { key: '' }),
        success: false
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

  render() {
    return (
      <PRDecorationBindingRenderer
        onFieldChange={this.handleFieldChange}
        onReset={this.handleReset}
        onSubmit={this.handleSubmit}
        {...this.state}
      />
    );
  }
}
