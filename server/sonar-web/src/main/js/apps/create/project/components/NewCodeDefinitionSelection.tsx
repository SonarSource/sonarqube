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
import { ButtonPrimary, DeferredSpinner, Link, Title } from 'design-system';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import { Router } from '../../../../components/hoc/withRouter';
import NewCodeDefinitionSelector from '../../../../components/new-code-definition/NewCodeDefinitionSelector';
import { useDocUrl } from '../../../../helpers/docs';
import { addGlobalSuccessMessage } from '../../../../helpers/globalMessages';
import { translate } from '../../../../helpers/l10n';
import { getProjectUrl } from '../../../../helpers/urls';
import { NewCodeDefinitiondWithCompliance } from '../../../../types/new-code-definition';
import { CreateProjectApiCallback } from '../types';

interface Props {
  canAdmin: boolean;
  createProjectFnRef: CreateProjectApiCallback | null;
  router: Router;
}

export default function NewCodeDefinitionSelection(props: Props) {
  const { canAdmin, createProjectFnRef, router } = props;

  const [submitting, setSubmitting] = React.useState(false);
  const [selectedDefinition, selectDefinition] = React.useState<NewCodeDefinitiondWithCompliance>();

  const getDocUrl = useDocUrl();

  const handleProjectCreation = React.useCallback(async () => {
    if (createProjectFnRef && selectedDefinition) {
      setSubmitting(true);
      const { project } = await createProjectFnRef(
        selectedDefinition.type,
        selectedDefinition.value
      );
      setSubmitting(false);
      router.push(getProjectUrl(project.key));

      addGlobalSuccessMessage(translate('onboarding.create_project.success'));
    }
  }, [createProjectFnRef, router, selectedDefinition]);

  return (
    <div id="project-ncd-selection" className="sw-body-sm">
      <Title>{translate('onboarding.create_project.new_code_definition.title')}</Title>

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

      <NewCodeDefinitionSelector canAdmin={canAdmin} onNcdChanged={selectDefinition} />

      <div className="sw-mt-10 sw-mb-8">
        <ButtonPrimary
          onClick={handleProjectCreation}
          disabled={!selectedDefinition?.isCompliant || submitting}
          type="submit"
        >
          {translate('onboarding.create_project.new_code_definition.create_project')}
          <DeferredSpinner className="sw-ml-2" loading={submitting} />
        </ButtonPrimary>
      </div>
    </div>
  );
}
