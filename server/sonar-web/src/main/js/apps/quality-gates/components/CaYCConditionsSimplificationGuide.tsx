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

import { SpotlightTour, SpotlightTourStep } from 'design-system';
import React from 'react';
import { dismissNotice } from '../../../api/users';
import { CurrentUserContext } from '../../../app/components/current-user/CurrentUserContext';
import DocumentationLink from '../../../components/common/DocumentationLink';
import { DocLink } from '../../../helpers/doc-links';
import { translate } from '../../../helpers/l10n';
import { QualityGate } from '../../../types/types';
import { NoticeType } from '../../../types/users';

interface Props {
  readonly qualityGate: QualityGate;
}

export default function CaYCConditionsSimplificationGuide({ qualityGate }: Props) {
  const { currentUser, updateDismissedNotices } = React.useContext(CurrentUserContext);
  const shouldRun =
    currentUser.isLoggedIn &&
    !currentUser.dismissedNotices[NoticeType.QG_CAYC_CONDITIONS_SIMPLIFICATION];

  const steps: SpotlightTourStep[] = [
    {
      target: '#cayc-highlight',
      content: (
        <p>{translate('quality_gates.cayc.condition_simplification_tour.page_1.content1')}</p>
      ),
      title: translate('quality_gates.cayc.condition_simplification_tour.page_1.title'),
      placement: 'top',
    },
    {
      target: '[data-guiding-id="caycConditionsSimplification"]',
      content: (
        <>
          <p className="sw-mb-4">
            {translate('quality_gates.cayc.condition_simplification_tour.page_2.content1')}
          </p>
          <p>{translate('quality_gates.cayc.condition_simplification_tour.page_2.content2')}</p>
        </>
      ),
      title: translate('quality_gates.cayc.condition_simplification_tour.page_2.title'),
      placement: 'right',
    },
    {
      target: '[data-guiding-id="caycConditionsSimplification"]',
      content: (
        <>
          <p className="sw-mb-4">
            {translate('quality_gates.cayc.condition_simplification_tour.page_3.content1')}
          </p>
          <DocumentationLink to={DocLink.IssueResolutions}>
            {translate('quality_gates.cayc.condition_simplification_tour.page_3.content2')}
          </DocumentationLink>
        </>
      ),
      title: translate('quality_gates.cayc.condition_simplification_tour.page_3.title'),
      placement: 'right',
    },
  ];

  const onCallback = async (props: { action: string; type: string }) => {
    if (props.action === 'close' && props.type === 'tour:end' && shouldRun) {
      await dismissNotice(NoticeType.QG_CAYC_CONDITIONS_SIMPLIFICATION);
      updateDismissedNotices(NoticeType.QG_CAYC_CONDITIONS_SIMPLIFICATION, true);
    }
  };

  if (!qualityGate.isBuiltIn) {
    return null;
  }

  return (
    <SpotlightTour
      continuous
      run={shouldRun}
      closeLabel={translate('dismiss')}
      callback={onCallback}
      steps={steps}
    />
  );
}
