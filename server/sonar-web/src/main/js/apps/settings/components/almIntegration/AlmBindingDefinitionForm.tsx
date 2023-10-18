/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
  validateAlmSettings,
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
  isBitbucketCloudBindingDefinition,
} from '../../../../types/alm-settings';
import { Dict } from '../../../../types/types';
import { BITBUCKET_CLOUD_WORKSPACE_ID_FORMAT } from '../../constants';
import AlmBindingDefinitionFormRenderer from './AlmBindingDefinitionFormRenderer';

export interface AlmBindingDefinitionFormProps {
  alm: AlmKeys;
  bindingDefinition?: AlmBindingDefinition;
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

const BINDING_PER_ALM: {
  [key in AlmKeys]: {
    createApi: (def: AlmBindingDefinition) => Promise<void>;
    updateApi: (def: AlmBindingDefinition) => Promise<void>;
    defaultBinding: AlmBindingDefinition;
    optionalFields: Dict<boolean>;
  };
} = {
  [AlmKeys.Azure]: {
    createApi: createAzureConfiguration,
    updateApi: updateAzureConfiguration,
    defaultBinding: { key: '', personalAccessToken: '', url: '' } as AzureBindingDefinition,
    optionalFields: {},
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
      privateKey: '',
      webhookSecret: '',
    } as GithubBindingDefinition,
    optionalFields: { webhookSecret: true },
  },
  [AlmKeys.GitLab]: {
    createApi: createGitlabConfiguration,
    updateApi: updateGitlabConfiguration,
    defaultBinding: { key: '', personalAccessToken: '', url: '' } as GitlabBindingDefinition,
    optionalFields: {},
  },
  [AlmKeys.BitbucketServer]: {
    createApi: createBitbucketServerConfiguration,
    updateApi: updateBitbucketServerConfiguration,
    defaultBinding: {
      key: '',
      url: '',
      personalAccessToken: '',
    } as BitbucketServerBindingDefinition,
    optionalFields: {},
  },
  [AlmKeys.BitbucketCloud]: {
    createApi: createBitbucketCloudConfiguration,
    updateApi: updateBitbucketCloudConfiguration,
    defaultBinding: {
      key: '',
      clientId: '',
      clientSecret: '',
      workspace: '',
    } as BitbucketCloudBindingDefinition,
    optionalFields: {},
  },
};

export default class AlmBindingDefinitionForm extends React.PureComponent<
  AlmBindingDefinitionFormProps,
  State
> {
  mounted = false;
  errorListElement = React.createRef<HTMLDivElement>();

  constructor(props: AlmBindingDefinitionFormProps) {
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
      bitbucketVariant,
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
        [fieldId]: value,
      },
      touched: true,
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
        key: alreadySavedFormData.key,
      } as any);
    } else if (this.props.bindingDefinition?.key) {
      apiMethod = BINDING_PER_ALM[apiAlm].updateApi({
        newKey: formData.key,
        ...formData,
        key: this.props.bindingDefinition.key,
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
        if (this.errorListElement?.current) {
          this.errorListElement.current.scrollIntoView({ block: 'start' });
        }
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
    bitbucketVariant: AlmKeys.BitbucketServer | AlmKeys.BitbucketCloud,
  ) => {
    this.setState({
      bitbucketVariant,
      formData: { ...BINDING_PER_ALM[bitbucketVariant].defaultBinding },
    });
  };

  canSubmit = () => {
    const { bitbucketVariant, formData, touched } = this.state;
    const { alm } = this.props;
    const allRequiredFieldsProvided =
      touched &&
      !Object.entries(formData)
        .filter(([key, _value]) => !BINDING_PER_ALM[alm].optionalFields[key])
        .some(([_key, value]) => !value);

    if (
      bitbucketVariant === AlmKeys.BitbucketCloud &&
      isBitbucketCloudBindingDefinition(formData)
    ) {
      return (
        allRequiredFieldsProvided && BITBUCKET_CLOUD_WORKSPACE_ID_FORMAT.test(formData.workspace)
      );
    }

    return allRequiredFieldsProvided;
  };

  render() {
    const { alm, bindingDefinition } = this.props;
    const { formData, submitting, bitbucketVariant, validationError } = this.state;

    const isUpdate = !!bindingDefinition;

    return (
      <AlmBindingDefinitionFormRenderer
        alm={alm}
        isUpdate={isUpdate}
        canSubmit={this.canSubmit()}
        onCancel={this.handleOnCancel}
        onSubmit={this.handleFormSubmit}
        onFieldChange={this.handleFieldChange}
        formData={formData}
        submitting={submitting}
        bitbucketVariant={bitbucketVariant}
        onBitbucketVariantChange={this.handleBitbucketVariantChange}
        validationError={validationError}
        errorListElementRef={this.errorListElement}
      />
    );
  }
}
