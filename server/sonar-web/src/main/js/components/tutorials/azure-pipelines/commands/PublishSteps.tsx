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
} from '../../../../app/components/available-features/withAvailableFeatures';
import { Alert } from '../../../../components/ui/Alert';
import { ALM_DOCUMENTATION_PATHS } from '../../../../helpers/constants';
import { translate } from '../../../../helpers/l10n';
import { AlmKeys } from '../../../../types/alm-settings';
import { Feature } from '../../../../types/features';
import DocLink from '../../../common/DocLink';
import SentenceWithHighlights from '../../components/SentenceWithHighlights';

export interface PublishStepsProps extends WithAvailableFeaturesProps {}
export function PublishSteps(props: PublishStepsProps) {
  const branchSupportEnabled = props.hasFeature(Feature.BranchSupport);

  return (
    <>
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
          translationKey={
            branchSupportEnabled
              ? 'onboarding.tutorial.with.azure_pipelines.BranchAnalysis.continous_integration'
              : 'onboarding.tutorial.with.azure_pipelines.BranchAnalysis.continous_integration.no_branches'
          }
          highlightKeys={['tab', 'continuous_integration']}
        />
      </li>
      {branchSupportEnabled && (
        <>
          <hr />
          <FormattedMessage
            id="onboarding.tutorial.with.azure_pipelines.BranchAnalysis.branch_protection"
            defaultMessage={translate(
              'onboarding.tutorial.with.azure_pipelines.BranchAnalysis.branch_protection'
            )}
            values={{
              link: (
                <DocLink to={ALM_DOCUMENTATION_PATHS[AlmKeys.Azure]}>
                  {translate(
                    'onboarding.tutorial.with.azure_pipelines.BranchAnalysis.branch_protection.link'
                  )}
                </DocLink>
              ),
            }}
          />
        </>
      )}
    </>
  );
}

export default withAvailableFeatures(PublishSteps);
