/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import { ButtonPrimary, Link, Modal, SubHeading, Title } from 'design-system';
import { sortBy } from 'lodash';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import { useDocUrl } from '../../../helpers/docs';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { useFixQualityGateMutation } from '../../../queries/quality-gates';
import { Condition, Dict, Metric, QualityGate } from '../../../types/types';
import { getWeakMissingAndNonCaycConditions } from '../utils';
import ConditionsTable from './ConditionsTable';

interface Props {
  canEdit: boolean;
  metrics: Dict<Metric>;
  conditions: Condition[];
  scope: 'new' | 'overall' | 'new-cayc';
  onClose: () => void;
  lockEditing: () => void;
  qualityGate: QualityGate;
  isOptimizing?: boolean;
}

export default function CaycReviewUpdateConditionsModal(props: Readonly<Props>) {
  const { conditions, qualityGate, metrics, lockEditing, onClose, isOptimizing } = props;
  const { mutateAsync: fixQualityGate } = useFixQualityGateMutation(qualityGate.name);

  const { weakConditions, missingConditions } = getWeakMissingAndNonCaycConditions(conditions);
  const sortedWeakConditions = sortBy(
    weakConditions,
    (condition) => metrics[condition.metric]?.name,
  );

  const sortedMissingConditions = sortBy(
    missingConditions,
    (condition) => metrics[condition.metric]?.name,
  );

  const getDocUrl = useDocUrl();

  const updateCaycQualityGate = React.useCallback(async () => {
    await fixQualityGate({ weakConditions, missingConditions });
    lockEditing();
  }, [fixQualityGate, weakConditions, missingConditions, lockEditing]);

  const body = (
    <div className="sw-mb-10">
      <SubHeading as="p" className="sw-body-sm">
        <FormattedMessage
          id={
            isOptimizing
              ? 'quality_gates.cayc.review_optimize_modal.description1'
              : 'quality_gates.cayc.review_update_modal.description1'
          }
          values={{
            cayc_link: (
              <Link to={getDocUrl('/user-guide/clean-as-you-code/')}>
                {translate('quality_gates.cayc')}
              </Link>
            ),
          }}
        />
      </SubHeading>

      {sortedMissingConditions.length > 0 && (
        <>
          <Title as="h4" className="sw-mb-2 sw-mt-4 sw-body-sm-highlight">
            {translateWithParameters(
              'quality_gates.cayc.review_update_modal.add_condition.header',
              sortedMissingConditions.length,
            )}
          </Title>
          <ConditionsTable
            {...props}
            conditions={sortedMissingConditions}
            showEdit={false}
            isCaycModal
          />
        </>
      )}

      {sortedWeakConditions.length > 0 && (
        <>
          <Title as="h4" className="sw-mb-2 sw-mt-4 sw-body-sm-highlight">
            {translateWithParameters(
              'quality_gates.cayc.review_update_modal.modify_condition.header',
              sortedWeakConditions.length,
            )}
          </Title>
          <ConditionsTable
            {...props}
            conditions={sortedWeakConditions}
            showEdit={false}
            isCaycModal
          />
        </>
      )}

      <Title as="h4" className="sw-mb-2 sw-mt-4 sw-body-sm-highlight">
        {translate('quality_gates.cayc.review_update_modal.description2')}
      </Title>
    </div>
  );

  return (
    <Modal
      isLarge
      headerTitle={translateWithParameters(
        isOptimizing
          ? 'quality_gates.cayc.review_optimize_modal.header'
          : 'quality_gates.cayc.review_update_modal.header',
        qualityGate.name,
      )}
      onClose={onClose}
      body={body}
      primaryButton={
        <ButtonPrimary
          autoFocus
          id="fix-quality-gate"
          type="submit"
          onClick={updateCaycQualityGate}
        >
          {translate(
            isOptimizing
              ? 'quality_gates.cayc.review_optimize_modal.confirm_text'
              : 'quality_gates.cayc.review_update_modal.confirm_text',
          )}
        </ButtonPrimary>
      }
      secondaryButtonLabel={translate('close')}
    />
  );
}
