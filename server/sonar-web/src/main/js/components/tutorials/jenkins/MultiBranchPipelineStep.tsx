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
import { rawSizes } from '../../../app/theme';
import { Button } from '../../../components/controls/buttons';
import ChevronRightIcon from '../../../components/icons/ChevronRightIcon';
import { translate } from '../../../helpers/l10n';
import {
  AlmKeys,
  AlmSettingsInstance,
  ProjectAlmBindingResponse,
} from '../../../types/alm-settings';
import CodeSnippet from '../../common/CodeSnippet';
import LabelActionPair from '../components/LabelActionPair';
import LabelValuePair from '../components/LabelValuePair';
import SentenceWithHighlights from '../components/SentenceWithHighlights';
import Step from '../components/Step';
import { buildGithubLink } from '../utils';

export interface MultiBranchPipelineStepProps {
  alm: AlmKeys;
  almBinding?: AlmSettingsInstance;
  finished: boolean;
  onDone: () => void;
  onOpen: () => void;
  open: boolean;
  projectBinding?: ProjectAlmBindingResponse;
}

/* Capture [workspaceID] from this pattern: https://bitbucket.org/[workspaceId]/  */
const bitbucketcloudUrlRegex = new RegExp('https:\\/\\/bitbucket.org\\/(.+)\\/');

function extractBitbucketCloudWorkspaceId(almBinding?: AlmSettingsInstance): string | undefined {
  if (almBinding?.url) {
    const result = almBinding.url.match(bitbucketcloudUrlRegex);

    return result ? result[1] : undefined;
  }
}

export default function MultiBranchPipelineStep(props: MultiBranchPipelineStepProps) {
  const { alm, almBinding, finished, open, projectBinding } = props;

  const workspaceId = extractBitbucketCloudWorkspaceId(almBinding);
  const isGitLab = alm === AlmKeys.GitLab;
  const isBitbucketServer = alm === AlmKeys.BitbucketServer;
  const isBitbucketCloud = alm === AlmKeys.BitbucketCloud;
  const isGitHub = alm === AlmKeys.GitHub;

  const renderForm = React.useCallback(
    () => (
      <div className="boxed-group-inner">
        <p className="big-spacer-bottom">
          {translate('onboarding.tutorial.with.jenkins.multi_branch_pipeline.intro')}
        </p>
        <ol className="list-styled">
          <li>
            <SentenceWithHighlights
              highlightKeys={['new_item', 'type']}
              translationKey="onboarding.tutorial.with.jenkins.multi_branch_pipeline.step1"
            />
          </li>
          <li>
            <SentenceWithHighlights
              highlightKeys={['tab', 'source']}
              translationKey={`onboarding.tutorial.with.jenkins.multi_branch_pipeline.step2.${alm}`}
            />
            <ul className="list-styled list-alpha">
              {isBitbucketServer && (
                <>
                  <li>
                    <LabelActionPair translationKey="onboarding.tutorial.with.jenkins.multi_branch_pipeline.step2.bitbucket.server" />
                  </li>
                  <li>
                    <LabelActionPair translationKey="onboarding.tutorial.with.jenkins.multi_branch_pipeline.step2.bitbucket.creds" />
                  </li>
                  <li>
                    {projectBinding?.repository ? (
                      <LabelValuePair
                        translationKey="onboarding.tutorial.with.jenkins.multi_branch_pipeline.step2.bitbucket.owner"
                        value={projectBinding.repository}
                      />
                    ) : (
                      <LabelActionPair translationKey="onboarding.tutorial.with.jenkins.multi_branch_pipeline.step2.bitbucket.owner" />
                    )}
                  </li>
                  <li>
                    {projectBinding?.slug ? (
                      <LabelValuePair
                        translationKey="onboarding.tutorial.with.jenkins.multi_branch_pipeline.step2.bitbucket.repo"
                        value={projectBinding.slug}
                      />
                    ) : (
                      <LabelActionPair translationKey="onboarding.tutorial.with.jenkins.multi_branch_pipeline.step2.bitbucket.repo" />
                    )}
                  </li>
                </>
              )}
              {isBitbucketCloud && (
                <>
                  <li>
                    <LabelActionPair translationKey="onboarding.tutorial.with.jenkins.multi_branch_pipeline.step2.bitbucketcloud.server" />
                  </li>
                  <li>
                    <LabelActionPair translationKey="onboarding.tutorial.with.jenkins.multi_branch_pipeline.step2.bitbucketcloud.creds" />
                  </li>
                  <li>
                    {workspaceId ? (
                      <LabelValuePair
                        translationKey="onboarding.tutorial.with.jenkins.multi_branch_pipeline.step2.bitbucketcloud.owner"
                        value={workspaceId}
                      />
                    ) : (
                      <LabelActionPair translationKey="onboarding.tutorial.with.jenkins.multi_branch_pipeline.step2.bitbucketcloud.owner" />
                    )}
                  </li>
                  <li>
                    {projectBinding?.repository ? (
                      <LabelValuePair
                        translationKey="onboarding.tutorial.with.jenkins.multi_branch_pipeline.step2.bitbucketcloud.repo"
                        value={projectBinding.repository}
                      />
                    ) : (
                      <LabelActionPair translationKey="onboarding.tutorial.with.jenkins.multi_branch_pipeline.step2.bitbucketcloud.repo" />
                    )}
                  </li>
                </>
              )}
              {isGitHub && (
                <>
                  <li>
                    <LabelActionPair translationKey="onboarding.tutorial.with.jenkins.multi_branch_pipeline.step2.github.creds" />
                  </li>
                  <li>
                    {almBinding !== undefined &&
                    projectBinding !== undefined &&
                    buildGithubLink(almBinding, projectBinding) !== null ? (
                      <LabelValuePair
                        translationKey="onboarding.tutorial.with.jenkins.multi_branch_pipeline.step2.github.repo_url"
                        value={buildGithubLink(almBinding, projectBinding) as string}
                      />
                    ) : (
                      <LabelActionPair translationKey="onboarding.tutorial.with.jenkins.multi_branch_pipeline.step2.github.repo_url" />
                    )}
                  </li>
                </>
              )}
              {isGitLab && (
                <>
                  <li>
                    <LabelActionPair translationKey="onboarding.tutorial.with.jenkins.multi_branch_pipeline.step2.gitlab.creds" />
                  </li>
                  <li>
                    <LabelActionPair translationKey="onboarding.tutorial.with.jenkins.multi_branch_pipeline.step2.gitlab.owner" />
                  </li>
                  <li>
                    <LabelActionPair translationKey="onboarding.tutorial.with.jenkins.multi_branch_pipeline.step2.gitlab.repo" />
                  </li>
                </>
              )}
              <li>
                <strong>
                  {translate(
                    'onboarding.tutorial.with.jenkins.multi_branch_pipeline.step2.behaviors.label'
                  )}
                  :
                </strong>
                <ol className="list-styled list-roman little-spacer-top abs-width-600">
                  <li>
                    <LabelActionPair
                      translationKey={`onboarding.tutorial.with.jenkins.multi_branch_pipeline.step2.behaviors.${
                        isGitLab ? 'branches_mrs' : 'branches_prs'
                      }`}
                    />
                  </li>
                  <li>
                    <LabelActionPair
                      translationKey={`onboarding.tutorial.with.jenkins.multi_branch_pipeline.step2.behaviors.${
                        isGitLab ? 'discover_mrs' : 'discover_prs'
                      }`}
                    />
                  </li>
                  <li>
                    <strong>
                      {translate(
                        'onboarding.tutorial.with.jenkins.multi_branch_pipeline.step2.behaviors.ref_specs.label'
                      )}
                      :
                    </strong>
                    <ul className="list-styled little-spacer-top">
                      <li>
                        <SentenceWithHighlights
                          highlightKeys={['add', 'ref_spec']}
                          translationKey="onboarding.tutorial.with.jenkins.multi_branch_pipeline.step2.behaviors.ref_specs.add_behaviour"
                        />
                      </li>
                      <li>
                        <SentenceWithHighlights
                          highlightKeys={['ref_spec']}
                          translationKey={`onboarding.tutorial.with.jenkins.multi_branch_pipeline.step2.behaviors.ref_specs.${
                            isGitLab ? 'set_mr_ref_specs' : 'set_pr_ref_specs'
                          }`}
                        />
                        <CodeSnippet
                          isOneLine={true}
                          snippet="+refs/heads/*:refs/remotes/@{remote}/*"
                        />
                      </li>
                    </ul>
                  </li>
                </ol>
              </li>
            </ul>
            <p className="big-spacer-left padder-left">
              {translate(
                'onboarding.tutorial.with.jenkins.multi_branch_pipeline.step2.leave_defaults'
              )}
            </p>
          </li>
          <li>
            <SentenceWithHighlights
              highlightKeys={['tab']}
              translationKey="onboarding.tutorial.with.jenkins.multi_branch_pipeline.step3"
            />
            <ul className="list-styled list-alpha">
              <li>
                <LabelActionPair translationKey="onboarding.tutorial.with.jenkins.multi_branch_pipeline.step3.mode" />
              </li>
              <li>
                <LabelActionPair translationKey="onboarding.tutorial.with.jenkins.multi_branch_pipeline.step3.script_path" />
              </li>
            </ul>
          </li>
          <li>
            <SentenceWithHighlights
              highlightKeys={['save']}
              translationKey="onboarding.tutorial.with.jenkins.multi_branch_pipeline.step4"
            />
          </li>
        </ol>
        <Button className="big-spacer-top" onClick={props.onDone}>
          {translate('continue')}
          <ChevronRightIcon size={rawSizes.baseFontSizeRaw} />
        </Button>
      </div>
    ),
    [
      isBitbucketCloud,
      isBitbucketServer,
      isGitHub,
      isGitLab,
      workspaceId,
      alm,
      projectBinding,
      almBinding,
      props.onDone,
    ]
  );

  return (
    <Step
      finished={finished}
      onOpen={props.onOpen}
      open={open}
      renderForm={renderForm}
      stepNumber={1}
      stepTitle={translate('onboarding.tutorial.with.jenkins.multi_branch_pipeline.title')}
    />
  );
}
