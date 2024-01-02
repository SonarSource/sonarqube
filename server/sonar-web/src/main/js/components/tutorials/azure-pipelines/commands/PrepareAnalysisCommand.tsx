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
import { ClipboardIconButton } from '../../../../components/controls/clipboard';
import { translate } from '../../../../helpers/l10n';
import CodeSnippet from '../../../common/CodeSnippet';
import SentenceWithHighlights from '../../components/SentenceWithHighlights';
import { BuildTools } from '../../types';

export enum PrepareType {
  JavaMavenGradle,
  StandAlone,
  MSBuild,
}

export interface PrepareAnalysisCommandProps {
  buildTool: BuildTools;
  kind: PrepareType;
  projectKey: string;
}

export default function PrepareAnalysisCommand(props: PrepareAnalysisCommandProps) {
  const { buildTool, kind, projectKey } = props;

  const ADDITIONAL_PROPERTY = 'sonar.cfamily.build-wrapper-output=bw-output';

  const MAVEN_GRADLE_PROPS_SNIPPET = `# Additional properties that will be passed to the scanner,
# Put one key=value per line, example:
# sonar.exclusions=**/*.bin
sonar.projectKey=${projectKey}`;

  return (
    <ul className="list-styled list-alpha spacer-top">
      <li>
        <SentenceWithHighlights
          translationKey="onboarding.tutorial.with.azure_pipelines.BranchAnalysis.prepare.endpoint"
          highlightKeys={['endpoint']}
        />
      </li>
      <li>
        <FormattedMessage
          id="onboarding.tutorial.with.azure_pipelines.BranchAnalysis.prepare.run_analysis"
          defaultMessage={translate(
            'onboarding.tutorial.with.azure_pipelines.BranchAnalysis.prepare.run_analysis'
          )}
          values={{
            section: (
              <strong>
                {translate(
                  'onboarding.tutorial.with.azure_pipelines.BranchAnalysis.prepare.run_analysis.section'
                )}
              </strong>
            ),
            run_analysis_value: (
              <strong>
                {translate(
                  'onboarding.tutorial.with.azure_pipelines.BranchAnalysis.prepare.run_analysis.values',
                  buildTool
                )}
              </strong>
            ),
          }}
        />
      </li>

      {kind === PrepareType.StandAlone && (
        <>
          <li>
            <SentenceWithHighlights
              translationKey="onboarding.tutorial.with.azure_pipelines.BranchAnalysis.manual"
              highlightKeys={['mode']}
            />
          </li>

          <li>
            <FormattedMessage
              id="onboarding.tutorial.with.azure_pipelines.BranchAnalysis.run.key.sentence"
              defaultMessage={translate(
                'onboarding.tutorial.with.azure_pipelines.BranchAnalysis.run.key.sentence'
              )}
              values={{
                project_key: (
                  <b>
                    {translate(
                      'onboarding.tutorial.with.azure_pipelines.BranchAnalysis.run.key.sentence.project_key'
                    )}
                  </b>
                ),
                key: <code className="rule">{projectKey}</code>,
                button: <ClipboardIconButton copyValue={projectKey} />,
              }}
            />
          </li>
          {buildTool === BuildTools.CFamily && (
            <li>
              <FormattedMessage
                id="onboarding.tutorial.with.azure_pipelines.BranchAnalysis.prepare_additional.ccpp"
                defaultMessage={translate(
                  'onboarding.tutorial.with.azure_pipelines.BranchAnalysis.prepare_additional.ccpp'
                )}
                values={{
                  advanced: (
                    <b>
                      {translate(
                        'onboarding.tutorial.with.azure_pipelines.BranchAnalysis.prepare_additional.ccpp.advanced'
                      )}
                    </b>
                  ),
                  additional: (
                    <b>
                      {translate(
                        'onboarding.tutorial.with.azure_pipelines.BranchAnalysis.prepare_additional.ccpp.additional'
                      )}
                    </b>
                  ),
                  property: <code className="rule">{ADDITIONAL_PROPERTY}</code>,
                  button: <ClipboardIconButton copyValue={ADDITIONAL_PROPERTY} />,
                }}
              />
            </li>
          )}
        </>
      )}

      {kind === PrepareType.JavaMavenGradle && (
        <li>
          <SentenceWithHighlights
            translationKey="onboarding.tutorial.with.azure_pipelines.BranchAnalysis.advanced_properties"
            highlightKeys={['section', 'properties']}
          />
          :
          <CodeSnippet snippet={MAVEN_GRADLE_PROPS_SNIPPET} />
        </li>
      )}
      {kind === PrepareType.MSBuild && (
        <li>
          <FormattedMessage
            id="onboarding.tutorial.with.azure_pipelines.BranchAnalysis.run.key.sentence"
            defaultMessage={translate(
              'onboarding.tutorial.with.azure_pipelines.BranchAnalysis.run.key.sentence'
            )}
            values={{
              project_key: (
                <b>
                  {translate(
                    'onboarding.tutorial.with.azure_pipelines.BranchAnalysis.run.key.sentence.project_key'
                  )}
                </b>
              ),
              key: <code className="rule">{projectKey}</code>,
              button: <ClipboardIconButton copyValue={projectKey} />,
            }}
          />
        </li>
      )}
    </ul>
  );
}
