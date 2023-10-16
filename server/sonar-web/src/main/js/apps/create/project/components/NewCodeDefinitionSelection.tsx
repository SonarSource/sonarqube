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
import { ButtonPrimary, ButtonSecondary, FlagMessage, Link, Spinner, Title } from 'design-system';
import { omit } from 'lodash';
import * as React from 'react';
import { useEffect } from 'react';
import { FormattedMessage, useIntl } from 'react-intl';
import { useNavigate, unstable_usePrompt as usePrompt } from 'react-router-dom';
import NewCodeDefinitionSelector from '../../../../components/new-code-definition/NewCodeDefinitionSelector';
import { useDocUrl } from '../../../../helpers/docs';
import { addGlobalSuccessMessage } from '../../../../helpers/globalMessages';
import { translate } from '../../../../helpers/l10n';
import { getProjectUrl, queryToSearch } from '../../../../helpers/urls';
import {
  MutationArg,
  useImportProjectMutation,
  useImportProjectProgress,
} from '../../../../queries/import-projects';
import { NewCodeDefinitiondWithCompliance } from '../../../../types/new-code-definition';
import { ImportProjectParam } from '../CreateProjectPage';

const listener = (event: BeforeUnloadEvent) => {
  event.returnValue = true;
};

interface Props {
  importProjects: ImportProjectParam;
}

export default function NewCodeDefinitionSelection(props: Props) {
  const { importProjects } = props;

  const [selectedDefinition, selectDefinition] = React.useState<NewCodeDefinitiondWithCompliance>();
  const { mutate, isLoading, data, reset } = useImportProjectMutation();
  const mutateCount = useImportProjectProgress();
  const intl = useIntl();
  const navigate = useNavigate();
  const getDocUrl = useDocUrl();
  usePrompt({
    when: isLoading,
    message: translate('onboarding.create_project.please_dont_leave'),
  });

  const projectCount = importProjects.projects.length;
  const isMultipleProjects = projectCount > 1;

  useEffect(() => {
    if (mutateCount > 0 || !data) {
      return;
    }
    reset();
    addGlobalSuccessMessage(
      intl.formatMessage(
        { id: 'onboarding.create_project.success' },
        {
          count: projectCount,
        },
      ),
    );

    if (projectCount === 1) {
      navigate(getProjectUrl(data.project.key));
    } else {
      navigate({
        pathname: '/projects',
        search: queryToSearch({ sort: '-creation_date' }),
      });
    }
  }, [data, projectCount, mutateCount, reset, intl, navigate]);

  React.useEffect(() => {
    if (isLoading) {
      window.addEventListener('beforeunload', listener);
    }

    return () => window.removeEventListener('beforeunload', listener);
  }, [isLoading]);

  const handleProjectCreation = () => {
    if (selectedDefinition) {
      importProjects.projects.forEach((p) => {
        const arg = {
          // eslint-disable-next-line local-rules/use-metrickey-enum
          ...omit(importProjects, 'projects'),
          ...p,
        } as MutationArg;
        mutate({
          newCodeDefinitionType: selectedDefinition.type,
          newCodeDefinitionValue: selectedDefinition.value,
          ...arg,
        });
      });
    }
  };

  return (
    <div id="project-ncd-selection" className="sw-body-sm">
      <Title>
        <FormattedMessage
          defaultMessage={translate('onboarding.create_x_project.new_code_definition.title')}
          id="onboarding.create_x_project.new_code_definition.title"
          values={{
            count: projectCount,
          }}
        />
      </Title>

      <p className="sw-mb-2">
        <FormattedMessage
          defaultMessage={translate('onboarding.create_project.new_code_definition.description')}
          id="onboarding.create_project.new_code_definition.description"
          values={{
            link: (
              <Link to={getDocUrl('/project-administration/defining-new-code/')}>
                {translate('onboarding.create_project.new_code_definition.description.link')}
              </Link>
            ),
          }}
        />
      </p>

      <NewCodeDefinitionSelector
        onNcdChanged={selectDefinition}
        isMultipleProjects={isMultipleProjects}
      />

      {isMultipleProjects && (
        <FlagMessage variant="info">
          {translate('onboarding.create_projects.new_code_definition.change_info')}
        </FlagMessage>
      )}

      <div className="sw-mt-10 sw-mb-8 sw-flex sw-gap-2 sw-items-center">
        <ButtonSecondary onClick={() => navigate(-1)}>{translate('back')}</ButtonSecondary>
        <ButtonPrimary
          onClick={handleProjectCreation}
          disabled={!selectedDefinition?.isCompliant || isLoading}
          type="submit"
        >
          <FormattedMessage
            defaultMessage={translate(
              'onboarding.create_project.new_code_definition.create_x_projects',
            )}
            id="onboarding.create_project.new_code_definition.create_x_projects"
            values={{
              count: projectCount,
            }}
          />
          <Spinner className="sw-ml-2" loading={isLoading} />
        </ButtonPrimary>
        {isLoading && (
          <FlagMessage variant="warning">
            <FormattedMessage
              id="onboarding.create_project.import_in_progress"
              values={{
                count: projectCount - mutateCount,
                total: projectCount,
              }}
            />
          </FlagMessage>
        )}
      </div>
    </div>
  );
}
