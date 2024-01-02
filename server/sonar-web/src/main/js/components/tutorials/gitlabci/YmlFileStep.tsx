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
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import withAvailableFeatures, {
  WithAvailableFeaturesProps,
} from '../../../app/components/available-features/withAvailableFeatures';
import { ClipboardIconButton } from '../../../components/controls/clipboard';
import { translate } from '../../../helpers/l10n';
import { Feature } from '../../../types/features';
import FinishButton from '../components/FinishButton';
import GithubCFamilyExampleRepositories from '../components/GithubCFamilyExampleRepositories';
import Step from '../components/Step';
import { BuildTools, TutorialModes } from '../types';
import PipeCommand from './commands/PipeCommand';

export interface YmlFileStepProps extends WithAvailableFeaturesProps {
  buildTool?: BuildTools;
  finished: boolean;
  onDone: () => void;
  onOpen: () => void;
  open: boolean;
  projectKey: string;
  mainBranchName: string;
}

export function YmlFileStep(props: YmlFileStepProps) {
  const { buildTool, open, finished, projectKey, mainBranchName } = props;
  const branchSupportEnabled = props.hasFeature(Feature.BranchSupport);

  const renderForm = () => (
    <div className="boxed-group-inner">
      <div className="flex-columns">
        <div className="flex-column-full">
          {buildTool && (
            <>
              {buildTool === BuildTools.CFamily && (
                <GithubCFamilyExampleRepositories
                  className="big-spacer-bottom abs-width-600"
                  ci={TutorialModes.GitLabCI}
                />
              )}
              <div className="big-spacer-bottom">
                <FormattedMessage
                  defaultMessage={translate('onboarding.tutorial.with.gitlab_ci.yml.description')}
                  id="onboarding.tutorial.with.gitlab_ci.yml.description"
                  values={{
                    filename: (
                      <>
                        <code className="rule">
                          {translate('onboarding.tutorial.with.gitlab_ci.yml.filename')}
                        </code>
                        <ClipboardIconButton
                          className="little-spacer-left"
                          copyValue={translate('onboarding.tutorial.with.gitlab_ci.yml.filename')}
                        />
                      </>
                    ),
                  }}
                />
              </div>
              <div className="big-spacer-bottom abs-width-600">
                <PipeCommand
                  buildTool={buildTool}
                  branchesEnabled={branchSupportEnabled}
                  mainBranchName={mainBranchName}
                  projectKey={projectKey}
                />
              </div>
              <p className="little-spacer-bottom">
                {branchSupportEnabled
                  ? translate('onboarding.tutorial.with.gitlab_ci.yml.baseconfig')
                  : translate('onboarding.tutorial.with.gitlab_ci.yml.baseconfig.no_branches')}
              </p>
              <p>{translate('onboarding.tutorial.with.gitlab_ci.yml.existing')}</p>
            </>
          )}
        </div>
      </div>
      <FinishButton onClick={props.onDone} />
    </div>
  );

  return (
    <Step
      finished={finished}
      onOpen={props.onOpen}
      open={open}
      renderForm={renderForm}
      stepNumber={3}
      stepTitle={translate('onboarding.tutorial.with.gitlab_ci.yml.title')}
    />
  );
}

export default withAvailableFeatures(YmlFileStep);
