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
import {
  createAzureConfiguration,
  createBitbucketCloudConfiguration,
  createBitbucketServerConfiguration,
  createGithubConfiguration,
  createGitlabConfiguration,
  updateAzureConfiguration,
  updateBitbucketCloudConfiguration,
  updateBitbucketServerConfiguration,
  updateGithubConfiguration,
  updateGitlabConfiguration
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

export interface AlmBindingDefinitionFormChildrenProps {
  formData: AlmBindingDefinition;
  onFieldChange: (fieldId: string, value: string) => void;
}

interface Props {
  alm: AlmKeys;
  bindingDefinition?: AlmBindingDefinition;
  alreadyHaveInstanceConfigured: boolean;
  onDelete?: (definitionKey: string) => void;
  onEdit?: (definitionKey: string) => void;
  onCancel?: () => void;
  afterSubmit?: (data: AlmBindingDefinitionBase) => void;
}

interface State {
  formData: AlmBindingDefinition;
  touched: boolean;
  submitting: boolean;
  bitbucketVariant?: AlmKeys.BitbucketServer | AlmKeys.BitbucketCloud;
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
    const { alm } = this.props;
    const { formData, bitbucketVariant } = this.state;
    const apiAlm = bitbucketVariant ?? alm;

    const apiMethod = this.props.bindingDefinition?.key
      ? BINDING_PER_ALM[apiAlm].updateApi({
          newKey: formData.key,
          ...formData,
          key: this.props.bindingDefinition.key
        } as any)
      : BINDING_PER_ALM[apiAlm].createApi({ ...formData } as any);

    this.setState({ submitting: true });

    try {
      await apiMethod;

      if (this.props.afterSubmit) {
        this.props.afterSubmit(formData);
      }
    } finally {
      if (this.mounted) {
        this.setState({ submitting: false });
      }
    }
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
    const { formData, submitting, bitbucketVariant } = this.state;

    const isUpdate = !!bindingDefinition;

    return (
      <AlmBindingDefinitionFormRenderer
        alm={alm}
        isUpdate={isUpdate}
        canSubmit={this.canSubmit()}
        alreadyHaveInstanceConfigured={alreadyHaveInstanceConfigured}
        onCancel={() => this.props.onCancel && this.props.onCancel()}
        onSubmit={this.handleFormSubmit}
        onFieldChange={this.handleFieldChange}
        formData={formData}
        submitting={submitting}
        bitbucketVariant={bitbucketVariant}
        onBitbucketVariantChange={this.handleBitbucketVariantChange}
      />
    );
  }
}
