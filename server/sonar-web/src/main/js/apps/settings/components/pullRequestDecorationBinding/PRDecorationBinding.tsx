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
import {
  deleteProjectAlmBinding,
  getAlmSettings,
  getProjectAlmBinding,
  setProjectAzureBinding,
  setProjectBitbucketBinding,
  setProjectGithubBinding
} from '../../../../api/almSettings';
import throwGlobalError from '../../../../app/utils/throwGlobalError';
import { AlmSettingsInstance, ALM_KEYS, ProjectAlmBinding } from '../../../../types/alm-settings';
import PRDecorationBindingRenderer from './PRDecorationBindingRenderer';

interface Props {
  component: T.Component;
}

interface State {
  formData: ProjectAlmBinding;
  hasBinding: boolean;
  instances: AlmSettingsInstance[];
  isValid: boolean;
  loading: boolean;
  saving: boolean;
  success: boolean;
}

const FIELDS_BY_ALM: {
  [almKey: string]: Array<'repository' | 'repositoryKey' | 'repositorySlug'>;
} = {
  [ALM_KEYS.AZURE]: [],
  [ALM_KEYS.BITBUCKET]: ['repositoryKey', 'repositorySlug'],
  [ALM_KEYS.GITHUB]: ['repository']
};

export default class PRDecorationBinding extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = {
    formData: {
      key: '',
      repository: ''
    },
    hasBinding: false,
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
      .then(([instances, data]) => {
        if (this.mounted) {
          this.setState(({ formData }) => {
            const newFormData = data || formData;
            return {
              formData: newFormData,
              hasBinding: Boolean(data),
              instances,
              isValid: this.validateForm(newFormData),
              loading: false
            };
          });

          if (!data && instances.length === 1) {
            this.handleFieldChange('key', instances[0].key);
          }
        }
      })
      .catch(() => {
        if (this.mounted) {
          this.setState({ loading: false });
        }
      });
  };

  getProjectBinding(project: string) {
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
              repository: ''
            },
            hasBinding: false,
            saving: false,
            success: true
          });
        }
      })
      .catch(this.catchError);
  };

  submitProjectAlmBinding(
    alm: ALM_KEYS,
    key: string,
    almSpecificFields?: { repository?: string; repositoryKey?: string; repositorySlug?: string }
  ): Promise<void> {
    const almSetting = key;
    const project = this.props.component.key;

    switch (alm) {
      case ALM_KEYS.AZURE:
        return setProjectAzureBinding({
          almSetting,
          project
        });
      case ALM_KEYS.BITBUCKET: {
        if (!almSpecificFields) {
          return Promise.reject();
        }
        const { repositoryKey = '', repositorySlug = '' } = almSpecificFields;
        return setProjectBitbucketBinding({
          almSetting,
          project,
          repositoryKey,
          repositorySlug
        });
      }
      case ALM_KEYS.GITHUB: {
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
              hasBinding: true,
              saving: false,
              success: true
            });
          }
        })
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
    return FIELDS_BY_ALM[selected.alm].reduce(
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
