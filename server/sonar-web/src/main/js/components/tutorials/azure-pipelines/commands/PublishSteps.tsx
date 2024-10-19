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

import { BasicSeparator, FlagMessage, Link, NumberedListItem } from 'design-system';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import withAvailableFeatures, {
  WithAvailableFeaturesProps,
} from '../../../../app/components/available-features/withAvailableFeatures';
import { DocLink } from '../../../../helpers/doc-links';
import { useDocUrl } from '../../../../helpers/docs';
import { translate } from '../../../../helpers/l10n';
import { Feature } from '../../../../types/features';
import SentenceWithHighlights from '../../components/SentenceWithHighlights';

export interface PublishStepsProps extends WithAvailableFeaturesProps {}

export function PublishSteps(props: PublishStepsProps) {
  const branchSupportEnabled = props.hasFeature(Feature.BranchSupport);

  const docUrl = useDocUrl(DocLink.AlmAzureIntegration);

  return (
    <>
      <NumberedListItem>
        <SentenceWithHighlights
          translationKey="onboarding.tutorial.with.azure_pipelines.BranchAnalysis.publish_qg"
          highlightKeys={['task']}
        />
        <FlagMessage variant="info" className="sw-mt-2">
          {translate(
            'onboarding.tutorial.with.azure_pipelines.BranchAnalysis.publish_qg.info.sentence1',
          )}
        </FlagMessage>
      </NumberedListItem>
      <NumberedListItem>
        <SentenceWithHighlights
          translationKey={
            branchSupportEnabled
              ? 'onboarding.tutorial.with.azure_pipelines.BranchAnalysis.continous_integration'
              : 'onboarding.tutorial.with.azure_pipelines.BranchAnalysis.continous_integration.no_branches'
          }
          highlightKeys={['tab', 'continuous_integration']}
        />
      </NumberedListItem>
      {branchSupportEnabled && (
        <>
          <BasicSeparator className="sw-my-4" />
          <div>
            <FormattedMessage
              id="onboarding.tutorial.with.azure_pipelines.BranchAnalysis.branch_protection"
              defaultMessage={translate(
                'onboarding.tutorial.with.azure_pipelines.BranchAnalysis.branch_protection',
              )}
              values={{
                link: (
                  <Link to={docUrl}>
                    {translate(
                      'onboarding.tutorial.with.azure_pipelines.BranchAnalysis.branch_protection.link',
                    )}
                  </Link>
                ),
              }}
            />
          </div>
        </>
      )}
    </>
  );
}

export default withAvailableFeatures(PublishSteps);
