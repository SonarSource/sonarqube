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

import { useState } from 'react';
import { useIntl } from 'react-intl';
import {
  ButtonPrimary,
  ButtonSecondary,
  InputTextArea,
  ItemDivider,
  PageContentFontWrapper,
  Spinner,
} from '~design-system';
import { translate } from '../../../helpers/l10n';
import { IssueActions, IssueTransition } from '../../../types/issues';
import { Issue } from '../../../types/types';
import { isTransitionDeprecated, isTransitionHidden, transitionRequiresComment } from '../helpers';
import { IssueTransitionItem } from './IssueTransitionItem';
import './IssueTransitionOverlay.css';

export type Props = {
  issue: Pick<Issue, 'transitions' | 'actions'>;
  loading?: boolean;
  onClose: () => void;
  onSetTransition: (transition: IssueTransition, comment?: string) => void;
};

export function IssueTransitionOverlay(props: Readonly<Props>) {
  const { issue, onClose, onSetTransition, loading } = props;
  const intl = useIntl();

  const [comment, setComment] = useState('');
  const [selectedTransition, setSelectedTransition] = useState<IssueTransition>();

  const hasCommentAction = issue.actions.includes(IssueActions.Comment);

  function selectTransition(transition: IssueTransition) {
    if (!transitionRequiresComment(transition) || !hasCommentAction) {
      onSetTransition(transition);
    } else {
      setSelectedTransition(transition);
    }
  }

  function handleResolve() {
    if (selectedTransition) {
      onSetTransition(selectedTransition, comment);
    }
  }

  // Filter out hidden transitions and separate deprecated transitions in a different list
  const filteredTransitions = issue.transitions.filter(
    (transition) => !isTransitionHidden(transition),
  );
  const filteredTransitionsRecommended = filteredTransitions.filter(
    (t) => !isTransitionDeprecated(t),
  );
  const filteredTransitionsDeprecated = filteredTransitions.filter(isTransitionDeprecated);

  return (
    <ul className="sw-flex sw-flex-col issues-status-panel">
      {filteredTransitionsRecommended.map((transition) => (
        <IssueTransitionItem
          key={transition}
          transition={transition}
          selected={selectedTransition === transition}
          onSelectTransition={selectTransition}
        />
      ))}
      {filteredTransitionsRecommended.length > 0 && filteredTransitionsDeprecated.length > 0 && (
        <ItemDivider />
      )}
      {filteredTransitionsDeprecated.map((transition) => (
        <IssueTransitionItem
          key={transition}
          transition={transition}
          selected={selectedTransition === transition}
          onSelectTransition={selectTransition}
        />
      ))}

      {selectedTransition && (
        <>
          <ItemDivider />
          <div className="sw-mx-4 sw-mt-2">
            <PageContentFontWrapper className="sw-font-semibold">
              {intl.formatMessage({ id: 'issue.transition.comment' })}
            </PageContentFontWrapper>
            <InputTextArea
              autoFocus
              onChange={(event) => setComment(event.currentTarget.value)}
              placeholder={translate(
                'issue.transition.comment.placeholder',
                selectedTransition ?? '',
              )}
              rows={5}
              value={comment}
              size="large"
              className="sw-mt-2"
            />
            <Spinner loading={loading} className="sw-float-right sw-m-2">
              <div className="sw-mt-2 sw-flex sw-gap-3 sw-justify-end">
                <ButtonPrimary onClick={handleResolve}>{translate('resolve')}</ButtonPrimary>
                <ButtonSecondary onClick={onClose}>{translate('cancel')}</ButtonSecondary>
              </div>
            </Spinner>
          </div>
        </>
      )}

      {!selectedTransition && loading && (
        <div className="sw-flex sw-justify-center sw-m-2">
          <Spinner loading className="sw-float-right sw-2" />
        </div>
      )}
    </ul>
  );
}
