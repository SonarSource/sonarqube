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
import withAvailableFeatures, {
  WithAvailableFeaturesProps,
} from '../../../app/components/available-features/withAvailableFeatures';
import { translate } from '../../../helpers/l10n';
import { getBaseUrl } from '../../../helpers/system';
import { AlmKeys } from '../../../types/alm-settings';
import { Feature } from '../../../types/features';
import SentenceWithHighlights from './SentenceWithHighlights';

export interface AllSetProps extends WithAvailableFeaturesProps {
  alm: AlmKeys;
  willRefreshAutomatically?: boolean;
}

export function AllSet(props: AllSetProps) {
  const { alm, willRefreshAutomatically } = props;
  const branchSupportEnabled = props.hasFeature(Feature.BranchSupport);

  return (
    <>
      <div className="abs-width-600">
        <p className="big-spacer-bottom">
          <SentenceWithHighlights
            highlightKeys={['all_set']}
            translationKey="onboarding.tutorial.ci_outro.all_set"
          />
        </p>
        <div className="display-flex-row big-spacer-bottom">
          <div>
            <img
              alt="" // Should be ignored by screen readers
              className="big-spacer-right"
              width={30}
              src={`${getBaseUrl()}/images/tutorials/commit.svg`}
            />
          </div>
          <div>
            <p className="little-spacer-bottom">
              <strong>{translate('onboarding.tutorial.ci_outro.commit')}</strong>
            </p>
            <p>
              {branchSupportEnabled
                ? translate('onboarding.tutorial.ci_outro.commit.why', alm)
                : translate('onboarding.tutorial.ci_outro.commit.why.no_branches')}
            </p>
          </div>
        </div>
        {willRefreshAutomatically && (
          <div className="display-flex-row">
            <div>
              <img
                alt="" // Should be ignored by screen readers
                className="big-spacer-right"
                width={30}
                src={`${getBaseUrl()}/images/tutorials/refresh.svg`}
              />
            </div>
            <div>
              <p className="little-spacer-bottom">
                <strong>{translate('onboarding.tutorial.ci_outro.refresh')}</strong>
              </p>
              <p>{translate('onboarding.tutorial.ci_outro.refresh.why')}</p>
            </div>
          </div>
        )}
      </div>
      {willRefreshAutomatically && (
        <div className="huge-spacer-bottom huge-spacer-top big-padded-top text-muted display-flex-center display-flex-justify-center">
          <i className="spinner spacer-right" />
          {translate('onboarding.tutorial.ci_outro.waiting_for_fist_analysis')}
        </div>
      )}
    </>
  );
}

export default withAvailableFeatures(AllSet);
