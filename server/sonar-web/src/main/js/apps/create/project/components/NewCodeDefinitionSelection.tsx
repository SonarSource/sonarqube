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
import {
  ButtonPrimary,
  ButtonSecondary,
  CloseIcon,
  FlagMessage,
  InteractiveIcon,
  Link,
  Spinner,
  Title,
  addGlobalErrorMessage,
  addGlobalSuccessMessage,
} from 'design-system';
import { omit } from 'lodash';
import * as React from 'react';
import { useEffect } from 'react';
import { FormattedMessage, useIntl } from 'react-intl';
import { useNavigate, unstable_usePrompt as usePrompt } from 'react-router-dom';
import { useLocation } from '../../../../components/hoc/withRouter';
import NewCodeDefinitionSelector from '../../../../components/new-code-definition/NewCodeDefinitionSelector';
import { useDocUrl } from '../../../../helpers/docs';
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
  onClose: () => void;
  redirectTo: string;
}

export default function NewCodeDefinitionSelection(props: Props) {
  const { importProjects, redirectTo, onClose } = props;

  const [selectedDefinition, selectDefinition] = React.useState<NewCodeDefinitiondWithCompliance>();
  const [failedImports, setFailedImports] = React.useState<number>(0);
  const { mutateAsync, data, reset, isIdle } = useImportProjectMutation();
  const mutateCount = useImportProjectProgress();
  const isImporting = mutateCount > 0;
  const intl = useIntl();
  const location = useLocation();
  const navigate = useNavigate();
  const getDocUrl = useDocUrl();
  usePrompt({
    when: isImporting,
    message: translate('onboarding.create_project.please_dont_leave'),
  });

  const projectCount = importProjects.projects.length;
  const isMultipleProjects = projectCount > 1;
  const isMonorepo = location.query?.mono === 'true';

  useEffect(() => {
    const redirect = (projectCount: number) => {
      if (!isMonorepo && projectCount === 1 && data) {
        if (redirectTo === '/projects') {
          navigate(getProjectUrl(data.project.key));
        } else {
          onClose();
        }
      } else {
        navigate({
          pathname: '/projects',
          search: queryToSearch({ sort: '-creation_date' }),
        });
      }
    };

    if (mutateCount > 0 || isIdle) {
      return;
    }

    if (failedImports > 0) {
      addGlobalErrorMessage(
        intl.formatMessage(
          { id: 'onboarding.create_project.failure' },
          {
            count: failedImports,
          },
        ),
      );
    }

    if (projectCount > failedImports) {
      if (redirectTo === '/projects') {
        addGlobalSuccessMessage(
          intl.formatMessage(
            {
              id: isMonorepo
                ? 'onboarding.create_project.monorepo.success'
                : 'onboarding.create_project.success',
            },
            {
              count: projectCount - failedImports,
            },
          ),
        );
      } else if (data) {
        addGlobalSuccessMessage(
          <FormattedMessage
            defaultMessage={translate('onboarding.create_project.success.admin')}
            id="onboarding.create_project.success.admin"
            values={{
              project_link: <Link to={getProjectUrl(data.project.key)}>{data.project.name}</Link>,
            }}
          />,
        );
      }
      redirect(projectCount);
    }

    reset();
    setFailedImports(0);
  }, [
    data,
    projectCount,
    failedImports,
    mutateCount,
    reset,
    intl,
    navigate,
    isIdle,
    redirectTo,
    onClose,
  ]);

  React.useEffect(() => {
    if (isImporting) {
      window.addEventListener('beforeunload', listener);
    }

    return () => window.removeEventListener('beforeunload', listener);
  }, [isImporting]);

  const handleProjectCreation = () => {
    if (selectedDefinition) {
      importProjects.projects.forEach((p) => {
        const arg = {
          // eslint-disable-next-line local-rules/use-metrickey-enum
          ...omit(importProjects, 'projects'),
          ...p,
        } as MutationArg;
        mutateAsync({
          newCodeDefinitionType: selectedDefinition.type,
          newCodeDefinitionValue: selectedDefinition.value,
          ...arg,
        }).catch(() => {
          setFailedImports((prev) => prev + 1);
        });
      });
    }
  };

  return (
    <section
      aria-label={translate('onboarding.create_project.new_code_definition.title')}
      id="project-ncd-selection"
      className="sw-body-sm"
    >
      <div className="sw-flex sw-justify-between">
        <FormattedMessage
          id="onboarding.create_project.manual.step2"
          defaultMessage={translate('onboarding.create_project.manual.step2')}
        />
        <InteractiveIcon
          Icon={CloseIcon}
          aria-label={intl.formatMessage({ id: 'clear' })}
          currentColor
          onClick={onClose}
          size="small"
        />
      </div>
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
          disabled={!selectedDefinition?.isCompliant || isImporting}
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
          <Spinner className="sw-ml-2" loading={isImporting} />
        </ButtonPrimary>
        {isImporting && projectCount > 1 && (
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
    </section>
  );
}
