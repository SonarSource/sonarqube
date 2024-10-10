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

import { ButtonIcon, ButtonSize, ButtonVariety, IconX } from '@sonarsource/echoes-react';
import classNames from 'classnames';
import {
  ButtonPrimary,
  ButtonSecondary,
  FlagErrorIcon,
  FlagMessage,
  FlagSuccessIcon,
  FormField,
  InputField,
  Link,
  Note,
  Title,
} from 'design-system';
import { isEmpty } from 'lodash';
import * as React from 'react';
import { FormattedMessage, useIntl } from 'react-intl';
import { getValue } from '../../../../api/settings';
import { DocLink } from '../../../../helpers/doc-links';
import { useDocUrl } from '../../../../helpers/docs';
import { translate } from '../../../../helpers/l10n';
import { GlobalSettingKeys } from '../../../../types/settings';
import { ImportProjectParam } from '../CreateProjectPage';
import ProjectValidation, { ProjectData } from '../components/ProjectValidation';
import { CreateProjectModes } from '../types';

interface Props {
  branchesEnabled: boolean;
  onClose: () => void;
  onProjectSetupDone: (importProjects: ImportProjectParam) => void;
}

interface MainBranchState {
  mainBranchName: string;
  mainBranchNameError?: boolean;
  mainBranchNameTouched: boolean;
}

type ValidState = ProjectData & Required<Pick<ProjectData, 'key' | 'name'>>;

export default function ManualProjectCreate(props: Readonly<Props>) {
  const [mainBranch, setMainBranch] = React.useState<MainBranchState>({
    mainBranchName: 'main',
    mainBranchNameTouched: false,
  });
  const [project, setProject] = React.useState<ProjectData>({
    hasError: false,
    key: '',
    name: '',
    touched: false,
  });

  const intl = useIntl();
  const docUrl = useDocUrl(DocLink.BranchAnalysis);

  React.useEffect(() => {
    async function fetchMainBranchName() {
      const { value: mainBranchName } =
        (await getValue({ key: GlobalSettingKeys.MainBranchName })) ?? {};

      if (mainBranchName !== undefined) {
        setMainBranch((prevBranchName) => ({
          ...prevBranchName,
          mainBranchName,
        }));
      }
    }

    fetchMainBranchName();
  }, []);

  const canSubmit = (
    mainBranch: MainBranchState,
    projectData: ProjectData,
  ): projectData is ValidState => {
    const { mainBranchName } = mainBranch;
    const { key, name, hasError } = projectData;
    return Boolean(!hasError && !isEmpty(key) && !isEmpty(name) && !isEmpty(mainBranchName));
  };

  const handleFormSubmit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (canSubmit(mainBranch, project)) {
      props.onProjectSetupDone({
        creationMode: CreateProjectModes.Manual,
        monorepo: false,
        projects: [
          {
            project: project.key,
            name: (project.name ?? project.key).trim(),
            mainBranch: mainBranchName,
          },
        ],
      });
    }
  };

  const handleBranchNameChange = (mainBranchName: string, fromUI = false) => {
    setMainBranch({
      mainBranchName,
      mainBranchNameError: validateMainBranchName(mainBranchName),
      mainBranchNameTouched: fromUI,
    });
  };

  const validateMainBranchName = (mainBranchName: string) => {
    if (isEmpty(mainBranchName)) {
      return true;
    }
    return undefined;
  };

  const { mainBranchName, mainBranchNameError, mainBranchNameTouched } = mainBranch;
  const { branchesEnabled } = props;

  const mainBranchNameIsValid = mainBranchNameTouched && mainBranchNameError === undefined;
  const mainBranchNameIsInvalid = mainBranchNameTouched && mainBranchNameError !== undefined;

  return (
    <section
      aria-label={translate('onboarding.create_project.manual.title')}
      className="sw-typo-default"
    >
      <div className="sw-flex sw-justify-between">
        <FormattedMessage
          id="onboarding.create_project.manual.step1"
          defaultMessage={translate('onboarding.create_project.manual.step1')}
        />
        <ButtonIcon
          Icon={IconX}
          ariaLabel={intl.formatMessage({ id: 'clear' })}
          onClick={props.onClose}
          size={ButtonSize.Medium}
          variety={ButtonVariety.DefaultGhost}
        />
      </div>
      <Title>{translate('onboarding.create_project.manual.title')}</Title>
      {branchesEnabled && (
        <FlagMessage className="sw-my-4" variant="info">
          {translate('onboarding.create_project.pr_decoration.information')}
        </FlagMessage>
      )}
      <div className="sw-max-w-[50%] sw-mt-2">
        <form
          id="create-project-manual"
          className="sw-flex-col sw-typo-default"
          onSubmit={handleFormSubmit}
        >
          <ProjectValidation onChange={setProject} />

          <FormField
            htmlFor="main-branch-name"
            label={translate('onboarding.create_project.main_branch_name')}
            required
          >
            <div>
              <InputField
                className={classNames({
                  'js__is-invalid': mainBranchNameIsInvalid,
                })}
                size="large"
                id="main-branch-name"
                minLength={1}
                onChange={(e) => handleBranchNameChange(e.currentTarget.value, true)}
                type="text"
                value={mainBranchName}
                isInvalid={mainBranchNameIsInvalid}
                isValid={mainBranchNameIsValid}
                required
              />
              {mainBranchNameIsInvalid && <FlagErrorIcon className="sw-ml-2" />}
              {mainBranchNameIsValid && <FlagSuccessIcon className="sw-ml-2" />}
            </div>
            <Note className="sw-mt-2">
              <FormattedMessage
                id="onboarding.create_project.main_branch_name.description"
                defaultMessage={translate('onboarding.create_project.main_branch_name.description')}
                values={{
                  learn_more: <Link to={docUrl}>{translate('learn_more')}</Link>,
                }}
              />
            </Note>
          </FormField>

          <ButtonSecondary className="sw-mt-4 sw-mr-4" onClick={props.onClose} type="button">
            {intl.formatMessage({ id: 'cancel' })}
          </ButtonSecondary>
          <ButtonPrimary
            className="sw-mt-4"
            type="submit"
            disabled={!canSubmit(mainBranch, project)}
          >
            {translate('next')}
          </ButtonPrimary>
        </form>
      </div>
    </section>
  );
}
