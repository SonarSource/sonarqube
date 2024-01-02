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
import { BasicSeparator, Title, TutorialStepList } from 'design-system';
import * as React from 'react';
import withAvailableFeatures, {
  WithAvailableFeaturesProps,
} from '../../../app/components/available-features/withAvailableFeatures';
import { translate } from '../../../helpers/l10n';
import { useProjectBindingQuery } from '../../../queries/devops-integration';
import { AlmKeys, AlmSettingsInstance } from '../../../types/alm-settings';
import { Feature } from '../../../types/features';
import { Component } from '../../../types/types';
import AllSet from '../components/AllSet';
import JenkinsfileStep from './JenkinsStep';
import MultiBranchPipelineStep from './MultiBranchPipelineStep';
import PipelineStep from './PipelineStep';
import PreRequisitesStep from './PreRequisitesStep';
import SelectAlmStep from './SelectAlmStep';
import WebhookStep from './WebhookStep';

export interface JenkinsTutorialProps extends WithAvailableFeaturesProps {
  almBinding?: AlmSettingsInstance;
  baseUrl: string;
  component: Component;
  willRefreshAutomatically?: boolean;
}

export function JenkinsTutorial(props: JenkinsTutorialProps) {
  const { almBinding, baseUrl, component, willRefreshAutomatically } = props;
  const { data: projectBinding } = useProjectBindingQuery(component.key);
  const hasSelectAlmStep = projectBinding?.alm === undefined;
  const branchSupportEnabled = props.hasFeature(Feature.BranchSupport);
  const [alm, setAlm] = React.useState<AlmKeys | undefined>(projectBinding?.alm);
  const [done, setDone] = React.useState(false);

  React.useEffect(() => {
    setAlm(projectBinding?.alm);
  }, [projectBinding]);

  return (
    <>
      <Title>{translate('onboarding.tutorial.with.jenkins.title')}</Title>

      {hasSelectAlmStep && <SelectAlmStep alm={alm} onChange={setAlm} />}
      {alm && (
        <>
          <TutorialStepList className="sw-mb-10">
            <PreRequisitesStep alm={alm} branchesEnabled={branchSupportEnabled} />

            {branchSupportEnabled ? (
              <MultiBranchPipelineStep
                alm={alm}
                almBinding={almBinding}
                projectBinding={projectBinding}
              />
            ) : (
              <PipelineStep alm={alm} />
            )}

            <WebhookStep
              alm={alm}
              almBinding={almBinding}
              branchesEnabled={branchSupportEnabled}
              projectBinding={projectBinding}
            />

            <JenkinsfileStep component={component} baseUrl={baseUrl} onDone={setDone} />
          </TutorialStepList>
          {done && (
            <>
              <BasicSeparator className="sw-my-10" />
              <AllSet alm={alm} willRefreshAutomatically={willRefreshAutomatically} />
            </>
          )}
        </>
      )}
    </>
  );
}

export default withAvailableFeatures(JenkinsTutorial);
