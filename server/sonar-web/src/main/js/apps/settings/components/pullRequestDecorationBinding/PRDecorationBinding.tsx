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

import { cloneDeep } from 'lodash';
import * as React from 'react';
import { getAlmSettings, validateProjectAlmBinding } from '../../../../api/alm-settings';
import withCurrentUserContext from '../../../../app/components/current-user/withCurrentUserContext';
import { hasGlobalPermission } from '../../../../helpers/users';
import {
  useDeleteProjectAlmBindingMutation,
  useProjectBindingQuery,
  useSetProjectBindingMutation,
} from '../../../../queries/devops-integration';
import {
  AlmKeys,
  AlmSettingsInstance,
  ProjectAlmBindingConfigurationErrors,
  ProjectAlmBindingResponse,
} from '../../../../types/alm-settings';
import { Permissions } from '../../../../types/permissions';
import { Component } from '../../../../types/types';
import { CurrentUser } from '../../../../types/users';
import PRDecorationBindingRenderer from './PRDecorationBindingRenderer';

type FormData = Omit<ProjectAlmBindingResponse, 'alm'>;

interface Props {
  component: Component;
  currentUser: CurrentUser;
}

interface State {
  checkingConfiguration: boolean;
  configurationErrors?: ProjectAlmBindingConfigurationErrors;
  formData: FormData;
  instances: AlmSettingsInstance[];
  isChanged: boolean;
  isConfigured: boolean;
  isValid: boolean;
  loading: boolean;
  originalData?: FormData;
  successfullyUpdated: boolean;
  updating: boolean;
}

const REQUIRED_FIELDS_BY_ALM: {
  [almKey in AlmKeys]: Array<keyof Omit<FormData, 'key'>>;
} = {
  [AlmKeys.Azure]: ['repository', 'slug'],
  [AlmKeys.BitbucketServer]: ['repository', 'slug'],
  [AlmKeys.BitbucketCloud]: ['repository'],
  [AlmKeys.GitHub]: ['repository'],
  [AlmKeys.GitLab]: ['repository'],
};

const INITIAL_FORM_DATA = { key: '', repository: '', monorepo: false };

export function PRDecorationBinding(props: Props) {
  const { component, currentUser } = props;
  const [formData, setFormData] = React.useState<FormData>(cloneDeep(INITIAL_FORM_DATA));
  const [instances, setInstances] = React.useState<AlmSettingsInstance[]>([]);
  const [configurationErrors, setConfigurationErrors] = React.useState(undefined);
  const [loading, setLoading] = React.useState(true);
  const [successfullyUpdated, setSuccessfullyUpdated] = React.useState(false);
  const [checkingConfiguration, setCheckingConfiguration] = React.useState(false);
  const { data: originalData } = useProjectBindingQuery(component.key);
  const { mutateAsync: deleteMutation, isPending: isDeleting } = useDeleteProjectAlmBindingMutation(
    component.key,
  );
  const { mutateAsync: updateMutation, isPending: isUpdating } = useSetProjectBindingMutation();

  const isConfigured = !!originalData;
  const updating = isDeleting || isUpdating;

  const isValid = React.useMemo(() => {
    const validateForm = ({ key, ...additionalFields }: State['formData']) => {
      const selected = instances.find((i) => i.key === key);
      if (!key || !selected) {
        return false;
      }
      return REQUIRED_FIELDS_BY_ALM[selected.alm].reduce(
        (result: boolean, field) => result && Boolean(additionalFields[field]),
        true,
      );
    };

    return validateForm(formData);
  }, [formData, instances]);

  const isDataSame = (
    { key, repository = '', slug = '', summaryCommentEnabled = false, monorepo = false }: FormData,
    {
      key: oKey = '',
      repository: oRepository = '',
      slug: oSlug = '',
      summaryCommentEnabled: osummaryCommentEnabled = false,
      monorepo: omonorepo = false,
    }: FormData,
  ) => {
    return (
      key === oKey &&
      repository === oRepository &&
      slug === oSlug &&
      summaryCommentEnabled === osummaryCommentEnabled &&
      monorepo === omonorepo
    );
  };

  const isChanged = !isDataSame(formData, originalData ?? cloneDeep(INITIAL_FORM_DATA));

  React.useEffect(() => {
    fetchDefinitions();
  }, []);

  React.useEffect(() => {
    checkConfiguration();
  }, [originalData]);

  React.useEffect(() => {
    setFormData((formData) => originalData ?? formData);
  }, [originalData]);

  const fetchDefinitions = () => {
    const project = component.key;

    return getAlmSettings(project)
      .then((instances) => {
        setInstances(instances || []);
        setConfigurationErrors(undefined);
        setLoading(false);
      })
      .catch(() => {
        setLoading(false);
      });
  };

  const handleReset = () => {
    deleteMutation()
      .then(() => {
        setFormData({
          key: '',
          repository: '',
          slug: '',
          monorepo: false,
        });
        setSuccessfullyUpdated(true);
        setConfigurationErrors(undefined);
      })
      .catch(() => {});
  };

  const submitProjectAlmBinding = (
    alm: AlmKeys,
    key: string,
    almSpecificFields: Omit<FormData, 'key'>,
  ): Promise<void> => {
    const almSetting = key;
    const { repository, slug = '', monorepo = false } = almSpecificFields;
    const project = component.key;

    const baseParams = {
      almSetting,
      project,
      repository,
      monorepo,
    };
    let updateParams;

    if (alm === AlmKeys.Azure || alm === AlmKeys.BitbucketServer) {
      updateParams = {
        alm,
        ...baseParams,
        slug,
      };
    } else if (alm === AlmKeys.GitHub) {
      updateParams = {
        alm,
        ...baseParams,
        summaryCommentEnabled: almSpecificFields?.summaryCommentEnabled ?? true,
      };
    } else {
      updateParams = {
        alm,
        ...baseParams,
      };
    }

    return updateMutation(updateParams);
  };

  const checkConfiguration = async () => {
    const projectKey = component.key;

    if (!isConfigured) {
      return;
    }

    setCheckingConfiguration(true);
    setConfigurationErrors(undefined);

    const configurationErrors = await validateProjectAlmBinding(projectKey).catch((error) => error);

    setCheckingConfiguration(false);
    setConfigurationErrors(configurationErrors);
  };

  const handleSubmit = () => {
    const { key, ...additionalFields } = formData;

    const selected = instances.find((i) => i.key === key);
    if (!key || !selected) {
      return;
    }

    submitProjectAlmBinding(selected.alm, key, additionalFields)
      .then(() => {
        setSuccessfullyUpdated(true);
      })
      .then(fetchDefinitions)
      .catch(() => {});
  };

  const handleFieldChange = (id: keyof ProjectAlmBindingResponse, value: string | boolean) => {
    setFormData((formData) => ({
      ...formData,
      [id]: value,
    }));
    setSuccessfullyUpdated(false);
  };

  const handleCheckConfiguration = async () => {
    await checkConfiguration();
  };

  return (
    <PRDecorationBindingRenderer
      onFieldChange={handleFieldChange}
      onReset={handleReset}
      onSubmit={handleSubmit}
      onCheckConfiguration={handleCheckConfiguration}
      isSysAdmin={hasGlobalPermission(currentUser, Permissions.Admin)}
      instances={instances}
      formData={formData}
      isChanged={isChanged}
      isValid={isValid}
      isConfigured={isConfigured}
      loading={loading}
      updating={updating}
      successfullyUpdated={successfullyUpdated}
      checkingConfiguration={checkingConfiguration}
      configurationErrors={configurationErrors}
    />
  );
}

export default withCurrentUserContext(PRDecorationBinding);
