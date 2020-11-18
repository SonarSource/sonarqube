/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import { Link } from 'react-router';
import { ClipboardIconButton } from 'sonar-ui-common/components/controls/clipboard';
import { Alert } from 'sonar-ui-common/components/ui/Alert';
import { translate, translateWithParameters } from 'sonar-ui-common/helpers/l10n';
import CodeSnippet from '../../common/CodeSnippet';
import RenderOptions from '../components/RenderOptions';
import SentenceWithHighlights from '../components/SentenceWithHighlights';

export interface BranchesAnalysisStepProps {
  component: T.Component;
  onStepValidationChange: (isValid: boolean) => void;
}

export enum BuildTechnology {
  DotNet = 'dotnet',
  Maven = 'maven',
  Gradle = 'gradle',
  Other = 'other'
}

export default function BranchAnalysisStepContent(props: BranchesAnalysisStepProps) {
  const { component, onStepValidationChange } = props;

  const [buildTechnology, setBuildTechnology] = React.useState<BuildTechnology | undefined>();

  React.useEffect(() => {
    if (buildTechnology) {
      onStepValidationChange(true);
    }
  }, [buildTechnology, onStepValidationChange]);

  const MAVEN_GRADLE_PROPS_SNIPPET = `# Additional properties that will be passed to the scanner,
# Put one key=value per line, example:
# sonar.exclusions=**/*.bin
sonar.projectKey=${component.key}`;

  return (
    <>
      <span>{translate('onboarding.build')}</span>
      <RenderOptions
        checked={buildTechnology}
        name="buildTechnology"
        onCheck={value => setBuildTechnology(value as BuildTechnology)}
        optionLabelKey="onboarding.build"
        options={Object.values(BuildTechnology)}
      />
      <ol className="list-styled big-spacer-top">
        {buildTechnology && (
          <>
            <li>
              <SentenceWithHighlights
                translationKey="onboarding.tutorial.with.azure_pipelines.BranchAnalysis.prepare"
                highlightKeys={['pipeline', 'task', 'before']}
              />
            </li>
            <ul className="list-styled">
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
                          buildTechnology
                        )}
                      </strong>
                    )
                  }}
                />
              </li>
              {buildTechnology === BuildTechnology.Other && (
                <li>
                  <SentenceWithHighlights
                    translationKey="onboarding.tutorial.with.azure_pipelines.BranchAnalysis.manual"
                    highlightKeys={['mode']}
                  />
                </li>
              )}
              {[BuildTechnology.DotNet, BuildTechnology.Other].includes(buildTechnology) && (
                <li>
                  <FormattedMessage
                    id="onboarding.tutorial.with.azure_pipelines.BranchAnalysis.run.key.sentence"
                    defaultMessage={translate(
                      'onboarding.tutorial.with.azure_pipelines.BranchAnalysis.run.key.sentence'
                    )}
                    values={{
                      key: <code className="rule">{component.key}</code>,
                      button: <ClipboardIconButton copyValue={component.key} />
                    }}
                  />
                </li>
              )}
              {[BuildTechnology.Maven, BuildTechnology.Gradle].includes(buildTechnology) && (
                <li>
                  <SentenceWithHighlights
                    translationKey="onboarding.tutorial.with.azure_pipelines.BranchAnalysis.advanced_properties"
                    highlightKeys={['section', 'properties']}
                  />
                  :
                  <CodeSnippet snippet={MAVEN_GRADLE_PROPS_SNIPPET} />
                </li>
              )}
            </ul>
            {[BuildTechnology.DotNet, BuildTechnology.Other].includes(buildTechnology) && (
              <li>
                <SentenceWithHighlights
                  translationKey="onboarding.tutorial.with.azure_pipelines.BranchAnalysis.run"
                  highlightKeys={['task', 'after']}
                />
              </li>
            )}
            {[BuildTechnology.Maven, BuildTechnology.Gradle].includes(buildTechnology) && (
              <>
                <li>
                  {translateWithParameters(
                    'onboarding.tutorial.with.azure_pipelines.BranchAnalysis.java',
                    translate('onboarding.build', buildTechnology)
                  )}
                </li>
                <ul className="list-styled big-spacer-bottom">
                  <li>
                    <SentenceWithHighlights
                      translationKey="onboarding.tutorial.with.azure_pipelines.BranchAnalysis.java.settings"
                      highlightKeys={['section', 'option']}
                    />
                  </li>
                </ul>
              </>
            )}

            <li>
              <SentenceWithHighlights
                translationKey="onboarding.tutorial.with.azure_pipelines.BranchAnalysis.publish_qg"
                highlightKeys={['task']}
              />
              <Alert variant="info" className="spacer-top">
                {translate(
                  'onboarding.tutorial.with.azure_pipelines.BranchAnalysis.publish_qg.info.sentence1'
                )}
              </Alert>
            </li>
            <li>
              <SentenceWithHighlights
                translationKey="onboarding.tutorial.with.azure_pipelines.BranchAnalysis.continous_integration"
                highlightKeys={['tab', 'continuous_integration']}
              />
            </li>
            <hr />
            <FormattedMessage
              id="onboarding.tutorial.with.azure_pipelines.BranchAnalysis.branch_protection"
              defaultMessage={translate(
                'onboarding.tutorial.with.azure_pipelines.BranchAnalysis.branch_protection'
              )}
              values={{
                link: (
                  <Link to="/documentation/analysis/azuredevops-integration/" target="_blank">
                    {translate(
                      'onboarding.tutorial.with.azure_pipelines.BranchAnalysis.branch_protection.link'
                    )}
                  </Link>
                )
              }}
            />
          </>
        )}
      </ol>
    </>
  );
}
