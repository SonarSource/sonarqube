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
  CodeSnippet,
  ListItem,
  NumberedList,
  NumberedListItem,
  TutorialStep,
  UnorderedList,
} from 'design-system';
import * as React from 'react';
import { translate } from '../../../helpers/l10n';
import {
  AlmKeys,
  AlmSettingsInstance,
  ProjectAlmBindingResponse,
} from '../../../types/alm-settings';
import LabelActionPair from '../components/LabelActionPair';
import LabelValuePair from '../components/LabelValuePair';
import SentenceWithHighlights from '../components/SentenceWithHighlights';
import { buildGithubLink } from '../utils';

export interface MultiBranchPipelineStepProps {
  alm: AlmKeys;
  almBinding?: AlmSettingsInstance;

  projectBinding?: ProjectAlmBindingResponse | null;
}

/* Capture [workspaceID] from this pattern: https://bitbucket.org/[workspaceId]/  */
const bitbucketcloudUrlRegex = /https:\/\/bitbucket.org\/(.+)\//;

function extractBitbucketCloudWorkspaceId(almBinding?: AlmSettingsInstance): string | undefined {
  if (almBinding?.url) {
    const result = bitbucketcloudUrlRegex.exec(almBinding.url);

    return result ? result[1] : undefined;
  }
}

export default function MultiBranchPipelineStep(props: MultiBranchPipelineStepProps) {
  const { alm, almBinding, projectBinding } = props;

  const workspaceId = extractBitbucketCloudWorkspaceId(almBinding);
  const isGitLab = alm === AlmKeys.GitLab;
  const isBitbucketServer = alm === AlmKeys.BitbucketServer;
  const isBitbucketCloud = alm === AlmKeys.BitbucketCloud;
  const isGitHub = alm === AlmKeys.GitHub;

  return (
    <TutorialStep title={translate('onboarding.tutorial.with.jenkins.multi_branch_pipeline.title')}>
      <p className="sw-mb-4">
        {translate('onboarding.tutorial.with.jenkins.multi_branch_pipeline.intro')}
      </p>
      <NumberedList>
        <NumberedListItem>
          <SentenceWithHighlights
            highlightKeys={['new_item', 'type']}
            translationKey="onboarding.tutorial.with.jenkins.multi_branch_pipeline.step1"
          />
        </NumberedListItem>
        <NumberedListItem>
          <SentenceWithHighlights
            highlightKeys={['tab', 'source']}
            translationKey={`onboarding.tutorial.with.jenkins.multi_branch_pipeline.step2.${alm}`}
          />
          <UnorderedList ticks className="sw-ml-12">
            {isBitbucketServer && (
              <>
                <ListItem>
                  <LabelActionPair translationKey="onboarding.tutorial.with.jenkins.multi_branch_pipeline.step2.bitbucket.server" />
                </ListItem>
                <ListItem>
                  <LabelActionPair translationKey="onboarding.tutorial.with.jenkins.multi_branch_pipeline.step2.bitbucket.creds" />
                </ListItem>
                <ListItem>
                  {projectBinding?.repository ? (
                    <LabelValuePair
                      translationKey="onboarding.tutorial.with.jenkins.multi_branch_pipeline.step2.bitbucket.owner"
                      value={projectBinding.repository}
                    />
                  ) : (
                    <LabelActionPair translationKey="onboarding.tutorial.with.jenkins.multi_branch_pipeline.step2.bitbucket.owner" />
                  )}
                </ListItem>
                <ListItem>
                  {projectBinding?.slug ? (
                    <LabelValuePair
                      translationKey="onboarding.tutorial.with.jenkins.multi_branch_pipeline.step2.bitbucket.repo"
                      value={projectBinding.slug}
                    />
                  ) : (
                    <LabelActionPair translationKey="onboarding.tutorial.with.jenkins.multi_branch_pipeline.step2.bitbucket.repo" />
                  )}
                </ListItem>
              </>
            )}
            {isBitbucketCloud && (
              <>
                <ListItem>
                  <LabelActionPair translationKey="onboarding.tutorial.with.jenkins.multi_branch_pipeline.step2.bitbucketcloud.server" />
                </ListItem>
                <ListItem>
                  <LabelActionPair translationKey="onboarding.tutorial.with.jenkins.multi_branch_pipeline.step2.bitbucketcloud.creds" />
                </ListItem>
                <ListItem>
                  {workspaceId ? (
                    <LabelValuePair
                      translationKey="onboarding.tutorial.with.jenkins.multi_branch_pipeline.step2.bitbucketcloud.owner"
                      value={workspaceId}
                    />
                  ) : (
                    <LabelActionPair translationKey="onboarding.tutorial.with.jenkins.multi_branch_pipeline.step2.bitbucketcloud.owner" />
                  )}
                </ListItem>
                <ListItem>
                  {projectBinding?.repository ? (
                    <LabelValuePair
                      translationKey="onboarding.tutorial.with.jenkins.multi_branch_pipeline.step2.bitbucketcloud.repo"
                      value={projectBinding.repository}
                    />
                  ) : (
                    <LabelActionPair translationKey="onboarding.tutorial.with.jenkins.multi_branch_pipeline.step2.bitbucketcloud.repo" />
                  )}
                </ListItem>
              </>
            )}
            {isGitHub && (
              <>
                <ListItem>
                  <LabelActionPair translationKey="onboarding.tutorial.with.jenkins.multi_branch_pipeline.step2.github.creds" />
                </ListItem>
                <ListItem>
                  {almBinding !== undefined &&
                  projectBinding != null &&
                  buildGithubLink(almBinding, projectBinding) !== null ? (
                    <LabelValuePair
                      translationKey="onboarding.tutorial.with.jenkins.multi_branch_pipeline.step2.github.repo_url"
                      value={buildGithubLink(almBinding, projectBinding) as string}
                    />
                  ) : (
                    <LabelActionPair translationKey="onboarding.tutorial.with.jenkins.multi_branch_pipeline.step2.github.repo_url" />
                  )}
                </ListItem>
              </>
            )}
            {isGitLab && (
              <>
                <ListItem>
                  <LabelActionPair translationKey="onboarding.tutorial.with.jenkins.multi_branch_pipeline.step2.gitlab.creds" />
                </ListItem>
                <ListItem>
                  <LabelActionPair translationKey="onboarding.tutorial.with.jenkins.multi_branch_pipeline.step2.gitlab.owner" />
                </ListItem>
                <ListItem>
                  <LabelActionPair translationKey="onboarding.tutorial.with.jenkins.multi_branch_pipeline.step2.gitlab.repo" />
                </ListItem>
              </>
            )}
            <ListItem>
              <strong>
                {translate(
                  'onboarding.tutorial.with.jenkins.multi_branch_pipeline.step2.behaviors.label',
                )}
                :
              </strong>
              <UnorderedList ticks className="sw-ml-4 sw-mt-1">
                <ListItem>
                  <LabelActionPair
                    translationKey={`onboarding.tutorial.with.jenkins.multi_branch_pipeline.step2.behaviors.${
                      isGitLab ? 'branches_mrs' : 'branches_prs'
                    }`}
                  />
                </ListItem>
                <ListItem>
                  <LabelActionPair
                    translationKey={`onboarding.tutorial.with.jenkins.multi_branch_pipeline.step2.behaviors.${
                      isGitLab ? 'discover_mrs' : 'discover_prs'
                    }`}
                  />
                </ListItem>
                <ListItem>
                  <strong>
                    {translate(
                      'onboarding.tutorial.with.jenkins.multi_branch_pipeline.step2.behaviors.ref_specs.label',
                    )}
                    :
                  </strong>
                  <UnorderedList className="sw-ml-4 sw-mt-1">
                    <ListItem>
                      <SentenceWithHighlights
                        highlightKeys={['add', 'ref_spec']}
                        translationKey="onboarding.tutorial.with.jenkins.multi_branch_pipeline.step2.behaviors.ref_specs.add_behaviour"
                      />
                    </ListItem>
                    <ListItem>
                      <SentenceWithHighlights
                        highlightKeys={['ref_spec']}
                        translationKey={`onboarding.tutorial.with.jenkins.multi_branch_pipeline.step2.behaviors.ref_specs.${
                          isGitLab ? 'set_mr_ref_specs' : 'set_pr_ref_specs'
                        }`}
                      />
                      <CodeSnippet
                        className="sw-p-4"
                        isOneLine
                        snippet="+refs/heads/*:refs/remotes/@{remote}/*"
                      />
                    </ListItem>
                  </UnorderedList>
                </ListItem>
              </UnorderedList>
            </ListItem>
          </UnorderedList>
          <p className="sw-ml-12">
            {translate(
              'onboarding.tutorial.with.jenkins.multi_branch_pipeline.step2.leave_defaults',
            )}
          </p>
        </NumberedListItem>
        <NumberedListItem>
          <SentenceWithHighlights
            highlightKeys={['tab']}
            translationKey="onboarding.tutorial.with.jenkins.multi_branch_pipeline.step3"
          />
          <UnorderedList ticks className="sw-ml-12">
            <ListItem>
              <LabelActionPair translationKey="onboarding.tutorial.with.jenkins.multi_branch_pipeline.step3.mode" />
            </ListItem>
            <ListItem>
              <LabelActionPair translationKey="onboarding.tutorial.with.jenkins.multi_branch_pipeline.step3.script_path" />
            </ListItem>
          </UnorderedList>
        </NumberedListItem>
        <NumberedListItem>
          <SentenceWithHighlights
            highlightKeys={['save']}
            translationKey="onboarding.tutorial.with.jenkins.multi_branch_pipeline.step4"
          />
        </NumberedListItem>
      </NumberedList>
    </TutorialStep>
  );
}
