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
  setProjectAlmBinding
} from '../../../../api/almSettings';
import throwGlobalError from '../../../../app/utils/throwGlobalError';
import PRDecorationBindingRenderer from './PRDecorationBindingRenderer';

interface Props {
  component: T.Component;
}

interface State {
  formData: T.GithubBinding;
  hasBinding: boolean;
  instances: T.AlmSettingsInstance[];
  isValid: boolean;
  loading: boolean;
  saving: boolean;
  success: boolean;
}

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
          this.setState(({ formData }) => ({
            formData: data || formData,
            hasBinding: Boolean(data),
            instances,
            isValid: this.validateForm(),
            loading: false
          }));

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

  handleSubmit = () => {
    this.setState({ saving: true });
    const {
      formData: { key, repository }
    } = this.state;

    if (key && repository) {
      setProjectAlmBinding({
        almSetting: key,
        project: this.props.component.key,
        repository
      })
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

  handleFieldChange = (id: keyof T.GithubBinding, value: string) => {
    this.setState(({ formData: formdata }) => ({
      formData: {
        ...formdata,
        [id]: value
      },
      isValid: this.validateForm(),
      success: false
    }));
  };

  validateForm = () => {
    const { formData } = this.state;
    return Object.values(formData).reduce(
      (result: boolean, value) => result && Boolean(value),
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
