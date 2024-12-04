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

import { Button, ButtonVariety, DropdownMenu, Spinner } from '@sonarsource/echoes-react';
import { useState } from 'react';
import { InputTextArea } from '~design-system';
import { translate } from '../../../helpers/l10n';
import { IssueActions, IssueTransition } from '../../../types/issues';
import { Issue } from '../../../types/types';
import { isTransitionHidden, transitionRequiresComment } from '../helpers';
import { IssueTransitionItem } from './IssueTransitionItem';
import IssueTransitionOverlayHeader from './IssueTransitionOverlayHeader';
import { SelectedTransitionItem } from './SelectedTransitionItem';

export type Props = {
  issue: Pick<Issue, 'transitions' | 'actions'>;
  loading?: boolean;
  onClose: () => void;
  onSetTransition: (transition: IssueTransition, comment?: string) => void;
};

export function IssueTransitionOverlay(props: Readonly<Props>) {
  const { issue, onClose, onSetTransition, loading } = props;

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

  return (
    <Spinner isLoading={!selectedTransition && loading} className="sw-ml-4">
      <IssueTransitionOverlayHeader
        onBack={() => setSelectedTransition(undefined)}
        onClose={onClose}
        selected={Boolean(selectedTransition)}
      />
      <DropdownMenu.Separator />
      <ul className="sw-flex sw-flex-col">
        {!selectedTransition &&
          filteredTransitions.map((transition, index) => (
            <div key={transition}>
              <IssueTransitionItem
                transition={transition}
                selected={selectedTransition === transition}
                hasCommentAction={transitionRequiresComment(transition)}
                onSelectTransition={selectTransition}
              />
              {index !== filteredTransitions.length - 1 && <DropdownMenu.Separator />}
            </div>
          ))}

        {selectedTransition && (
          <>
            <SelectedTransitionItem transition={selectedTransition} />
            <DropdownMenu.Separator />
            <div className="sw-mx-3 sw-mt-2">
              <div className="sw-font-semibold">{translate('issue.transition.comment')}</div>
              <div className="sw-flex sw-flex-col">
                <InputTextArea
                  autoFocus
                  className="sw-mt-2 sw-resize"
                  onChange={(event) => setComment(event.currentTarget.value)}
                  placeholder={translate(
                    'issue.transition.comment.placeholder',
                    selectedTransition ?? '',
                  )}
                  rows={3}
                  size="large"
                  value={comment}
                />
              </div>
              <div className="sw-mt-2 sw-flex sw-gap-3 sw-justify-end">
                <Button variety={ButtonVariety.Primary} onClick={handleResolve}>
                  {translate('issue.transition.change_status')}
                </Button>
                <Button variety={ButtonVariety.Default} onClick={onClose}>
                  {translate('cancel')}
                </Button>
              </div>
            </div>
          </>
        )}
      </ul>
    </Spinner>
  );
}
