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

import { FormattedMessage } from 'react-intl';
import { ClipboardIconButton, CodeSnippet, ListItem, UnorderedList } from '~design-system';
import { translate } from '../../../../helpers/l10n';
import { InlineSnippet } from '../../components/InlineSnippet';
import SentenceWithHighlights from '../../components/SentenceWithHighlights';
import { BuildTools } from '../../types';
import { isCFamily } from '../../utils';

export enum PrepareType {
  JavaMavenGradle,
  StandAlone,
  MSBuild,
}

export interface PrepareAnalysisCommandProps {
  buildTool: BuildTools;
  kind: PrepareType;
  projectKey: string;
  projectName?: string;
}

export default function PrepareAnalysisCommand(props: PrepareAnalysisCommandProps) {
  const { buildTool, kind, projectKey, projectName } = props;

  const ADDITIONAL_PROPERTY = 'sonar.cfamily.compile-commands=bw-output/compile_commands.json';

  const MAVEN_GRADLE_PROPS_SNIPPET = `# Additional properties that will be passed to the scanner,
# Put one key=value per line, example:
# sonar.exclusions=**/*.bin
sonar.projectKey=${projectKey}
sonar.projectName=${projectName}
`;

  return (
    <UnorderedList ticks className="sw-ml-12 sw-my-2">
      <ListItem>
        <SentenceWithHighlights
          translationKey="onboarding.tutorial.with.azure_pipelines.BranchAnalysis.prepare.endpoint"
          highlightKeys={['endpoint']}
        />
      </ListItem>
      <ListItem>
        <FormattedMessage
          id="onboarding.tutorial.with.azure_pipelines.BranchAnalysis.prepare.run_analysis"
          defaultMessage={translate(
            'onboarding.tutorial.with.azure_pipelines.BranchAnalysis.prepare.run_analysis',
          )}
          values={{
            section: (
              <b className="sw-font-semibold">
                {translate(
                  'onboarding.tutorial.with.azure_pipelines.BranchAnalysis.prepare.run_analysis.section',
                )}
              </b>
            ),
            run_analysis_value: (
              <b className="sw-font-semibold">
                {translate(
                  'onboarding.tutorial.with.azure_pipelines.BranchAnalysis.prepare.run_analysis.values',
                  buildTool,
                )}
              </b>
            ),
          }}
        />
      </ListItem>

      {kind === PrepareType.StandAlone && (
        <>
          <ListItem>
            <SentenceWithHighlights
              translationKey="onboarding.tutorial.with.azure_pipelines.BranchAnalysis.manual"
              highlightKeys={['mode']}
            />
          </ListItem>

          <ListItem>
            <span className="sw-flex sw-items-center">
              <FormattedMessage
                id="onboarding.tutorial.with.azure_pipelines.BranchAnalysis.run.key.sentence"
                defaultMessage={translate(
                  'onboarding.tutorial.with.azure_pipelines.BranchAnalysis.run.key.sentence',
                )}
                values={{
                  project_key: (
                    <b className="sw-font-semibold sw-mx-1">
                      {translate(
                        'onboarding.tutorial.with.azure_pipelines.BranchAnalysis.run.key.sentence.project_key',
                      )}
                    </b>
                  ),
                  key: (
                    <span className="sw-ml-1">
                      <InlineSnippet snippet={projectKey} />
                    </span>
                  ),
                  button: <ClipboardIconButton className="sw-ml-2" copyValue={projectKey} />,
                }}
              />
            </span>
          </ListItem>
          {isCFamily(buildTool) && (
            <ListItem>
              <span className="sw-flex sw-items-center sw-flex-wrap">
                <FormattedMessage
                  id="onboarding.tutorial.with.azure_pipelines.BranchAnalysis.prepare_additional.ccpp"
                  defaultMessage={translate(
                    'onboarding.tutorial.with.azure_pipelines.BranchAnalysis.prepare_additional.ccpp',
                  )}
                  values={{
                    advanced: (
                      <b className="sw-font-semibold sw-mx-1">
                        {translate(
                          'onboarding.tutorial.with.azure_pipelines.BranchAnalysis.prepare_additional.ccpp.advanced',
                        )}
                      </b>
                    ),
                    additional: (
                      <b className="sw-font-semibold sw-mx-1">
                        {translate(
                          'onboarding.tutorial.with.azure_pipelines.BranchAnalysis.prepare_additional.ccpp.additional',
                        )}
                      </b>
                    ),
                    property: (
                      <span className="sw-ml-1">
                        <InlineSnippet snippet={ADDITIONAL_PROPERTY} />
                      </span>
                    ),
                    button: (
                      <ClipboardIconButton className="sw-ml-2" copyValue={ADDITIONAL_PROPERTY} />
                    ),
                  }}
                />
              </span>
            </ListItem>
          )}
        </>
      )}

      {kind === PrepareType.JavaMavenGradle && (
        <ListItem>
          <SentenceWithHighlights
            translationKey="onboarding.tutorial.with.azure_pipelines.BranchAnalysis.advanced_properties"
            highlightKeys={['section', 'properties']}
          />
          :
          <CodeSnippet
            className="sw-p-6"
            language="properties"
            snippet={MAVEN_GRADLE_PROPS_SNIPPET}
          />
        </ListItem>
      )}
      {kind === PrepareType.MSBuild && (
        <ListItem>
          <span className="sw-flex sw-items-center">
            <FormattedMessage
              id="onboarding.tutorial.with.azure_pipelines.BranchAnalysis.run.key.sentence"
              defaultMessage={translate(
                'onboarding.tutorial.with.azure_pipelines.BranchAnalysis.run.key.sentence',
              )}
              values={{
                project_key: (
                  <b className="sw-font-semibold sw-mx-1">
                    {translate(
                      'onboarding.tutorial.with.azure_pipelines.BranchAnalysis.run.key.sentence.project_key',
                    )}
                  </b>
                ),
                key: (
                  <span className="sw-ml-1">
                    <InlineSnippet snippet={projectKey} />
                  </span>
                ),
                button: <ClipboardIconButton className="sw-ml-2" copyValue={projectKey} />,
              }}
            />
          </span>
        </ListItem>
      )}
    </UnorderedList>
  );
}
