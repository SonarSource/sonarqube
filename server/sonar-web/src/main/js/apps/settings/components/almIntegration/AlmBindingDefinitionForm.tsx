/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
  createAzureConfiguration,
  createBitbucketCloudConfiguration,
  createBitbucketServerConfiguration,
  createGithubConfiguration,
  createGitlabConfiguration,
  deleteConfiguration,
  updateAzureConfiguration,
  updateBitbucketCloudConfiguration,
  updateBitbucketServerConfiguration,
  updateGithubConfiguration,
  updateGitlabConfiguration,
  validateAlmSettings
} from '../../../../api/alm-settings';
import {
  AlmBindingDefinition,
  AlmBindingDefinitionBase,
  AlmKeys,
  AzureBindingDefinition,
  BitbucketCloudBindingDefinition,
  BitbucketServerBindingDefinition,
  GithubBindingDefinition,
  GitlabBindingDefinition,
  isBitbucketCloudBindingDefinition
} from '../../../../types/alm-settings';
import AlmBindingDefinitionFormRenderer from './AlmBindingDefinitionFormRenderer';

interface Props {
  alm: AlmKeys;
  bindingDefinition?: AlmBindingDefinition;
  alreadyHaveInstanceConfigured: boolean;
  onCancel: () => void;
  afterSubmit: (data: AlmBindingDefinitionBase) => void;
  enforceValidation?: boolean;
}

interface State {
  formData: AlmBindingDefinition;
  touched: boolean;
  submitting: boolean;
  bitbucketVariant?: AlmKeys.BitbucketServer | AlmKeys.BitbucketCloud;
  alreadySavedFormData?: AlmBindingDefinition;
  validationError?: string;
}

const BINDING_PER_ALM = {
  [AlmKeys.Azure]: {
    createApi: createAzureConfiguration,
    updateApi: updateAzureConfiguration,
    defaultBinding: { key: '', personalAccessToken: '', url: '' } as AzureBindingDefinition
  },
  [AlmKeys.GitHub]: {
    createApi: createGithubConfiguration,
    updateApi: updateGithubConfiguration,
    defaultBinding: {
      key: '',
      appId: '',
      clientId: '',
      clientSecret: '',
      url: '',
      privateKey: ''
    } as GithubBindingDefinition
  },
  [AlmKeys.GitLab]: {
    createApi: createGitlabConfiguration,
    updateApi: updateGitlabConfiguration,
    defaultBinding: { key: '', personalAccessToken: '', url: '' } as GitlabBindingDefinition
  },
  [AlmKeys.BitbucketServer]: {
    createApi: createBitbucketServerConfiguration,
    updateApi: updateBitbucketServerConfiguration,
    defaultBinding: {
      key: '',
      url: '',
      personalAccessToken: ''
    } as BitbucketServerBindingDefinition
  },
  [AlmKeys.BitbucketCloud]: {
    createApi: createBitbucketCloudConfiguration,
    updateApi: updateBitbucketCloudConfiguration,
    defaultBinding: {
      key: '',
      clientId: '',
      clientSecret: '',
      workspace: ''
    } as BitbucketCloudBindingDefinition
  }
};

export default class AlmBindingDefinitionForm extends React.PureComponent<Props, State> {
  mounted = false;
  constructor(props: Props) {
    super(props);

    let bitbucketVariant: AlmKeys.BitbucketServer | AlmKeys.BitbucketCloud | undefined = undefined;

    if (props.bindingDefinition && props.alm === AlmKeys.BitbucketServer) {
      bitbucketVariant = isBitbucketCloudBindingDefinition(props.bindingDefinition)
        ? AlmKeys.BitbucketCloud
        : AlmKeys.BitbucketServer;
    }

    const alm = bitbucketVariant || props.alm;

    this.state = {
      formData: props.bindingDefinition ?? BINDING_PER_ALM[alm].defaultBinding,
      touched: false,
      submitting: false,
      bitbucketVariant
    };
  }

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  handleFieldChange = (fieldId: string, value: string) => {
    this.setState(({ formData }) => ({
      formData: {
        ...formData,
        [fieldId]: value
      },
      touched: true
    }));
  };

  handleFormSubmit = async () => {
    const { alm, enforceValidation } = this.props;
    const { formData, bitbucketVariant, alreadySavedFormData, validationError } = this.state;
    const apiAlm = bitbucketVariant ?? alm;

    let apiMethod;

    if (alreadySavedFormData && validationError) {
      apiMethod = BINDING_PER_ALM[apiAlm].updateApi({
        newKey: formData.key,
        ...formData,
        key: alreadySavedFormData.key
      } as any);
    } else if (this.props.bindingDefinition?.key) {
      apiMethod = BINDING_PER_ALM[apiAlm].updateApi({
        newKey: formData.key,
        ...formData,
        key: this.props.bindingDefinition.key
      } as any);
    } else {
      apiMethod = BINDING_PER_ALM[apiAlm].createApi({ ...formData } as any);
    }

    this.setState({ submitting: true });

    try {
      await apiMethod;

      if (!this.mounted) {
        return;
      }

      this.setState({ alreadySavedFormData: formData });

      let error: string | undefined;

      if (enforceValidation) {
        error = await validateAlmSettings(formData.key);
      }

      if (!this.mounted) {
        return;
      }

      if (error) {
        this.setState({ validationError: error });
      } else {
        this.props.afterSubmit(formData);
      }
    } finally {
      if (this.mounted) {
        this.setState({ submitting: false, touched: false });
      }
    }
  };

  handleOnCancel = async () => {
    const { alreadySavedFormData } = this.state;

    if (alreadySavedFormData) {
      await deleteConfiguration(alreadySavedFormData.key);
    }

    this.props.onCancel();
  };

  handleBitbucketVariantChange = (
    bitbucketVariant: AlmKeys.BitbucketServer | AlmKeys.BitbucketCloud
  ) => {
    this.setState({
      bitbucketVariant,
      formData: { ...BINDING_PER_ALM[bitbucketVariant].defaultBinding }
    });
  };

  canSubmit = () => {
    const { formData, touched } = this.state;

    return touched && !Object.values(formData).some(v => !v);
  };

  render() {
    const { alm, bindingDefinition, alreadyHaveInstanceConfigured } = this.props;
    const { formData, submitting, bitbucketVariant, validationError } = this.state;

    const isUpdate = !!bindingDefinition;

    return (
      <AlmBindingDefinitionFormRenderer
        alm={alm}
        isUpdate={isUpdate}
        canSubmit={this.canSubmit()}
        alreadyHaveInstanceConfigured={alreadyHaveInstanceConfigured}
        onCancel={this.handleOnCancel}
        onSubmit={this.handleFormSubmit}
        onFieldChange={this.handleFieldChange}
        formData={formData}
        submitting={submitting}
        bitbucketVariant={bitbucketVariant}
        onBitbucketVariantChange={this.handleBitbucketVariantChange}
        validationError={validationError}
      />
    );
  }
}
