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
import { AlmKeys, AlmSettingsInstance, ProjectAlmBinding } from '../../../../types/alm-settings';
import PRDecorationBindingRenderer from './PRDecorationBindingRenderer';

interface Props {
  component: T.Component;
}

interface State {
  formData: ProjectAlmBinding;
  instances: AlmSettingsInstance[];
  isValid: boolean;
  loading: boolean;
  originalData?: ProjectAlmBinding;
  saving: boolean;
  success: boolean;
}

const REQUIRED_FIELDS_BY_ALM: {
  [almKey in AlmKeys]: Array<keyof T.Omit<ProjectAlmBinding, 'key'>>;
} = {
  [AlmKeys.Azure]: [],
  [AlmKeys.Bitbucket]: ['repository', 'slug'],
  [AlmKeys.GitHub]: ['repository'],
  [AlmKeys.GitLab]: []
};

export default class PRDecorationBinding extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = {
    formData: { key: '' },
    instances: [],
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
              isValid: this.validateForm(newFormData),
              loading: false,
              originalData
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

  getProjectBinding(project: string): Promise<ProjectAlmBinding | undefined> {
    return getProjectAlmBinding(project).catch((response: Response) => {
      if (response && response.status === 404) {
        return Promise.resolve(undefined);
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
            originalData: undefined,
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
    almSpecificFields?: T.Omit<ProjectAlmBinding, 'key'>
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
        const repository = almSpecificFields && almSpecificFields.repository;
        if (!repository) {
          return Promise.reject();
        }
        return setProjectGithubBinding({
          almSetting,
          project,
          repository
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

    if (key) {
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
    }
  };

  handleFieldChange = (id: keyof ProjectAlmBinding, value: string) => {
    this.setState(({ formData }) => {
      const newFormData = {
        ...formData,
        [id]: value
      };
      return {
        formData: newFormData,
        isValid: this.validateForm(newFormData),
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
