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
import {
  HelperHintIcon,
  ItemButton,
  PageContentFontWrapper,
  PopupPlacement,
  TextBold,
  TextMuted,
  Tooltip,
} from 'design-system';
import * as React from 'react';
import { useIntl } from 'react-intl';
import { translate } from '../../../helpers/l10n';
import { IssueTransition } from '../../../types/issues';

type Props = {
  transition: IssueTransition;
  selected: boolean;
  onSelectTransition: (transition: IssueTransition) => void;
};

export function IssueTransitionItem({ transition, selected, onSelectTransition }: Readonly<Props>) {
  const intl = useIntl();

  const tooltips: Record<string, React.JSX.Element> = {
    [IssueTransition.Confirm]: (
      <div className="sw-flex sw-flex-col sw-gap-2">
        <span>{translate('issue.transition.confirm.deprecated_tooltip.1')}</span>
        <span>{translate('issue.transition.confirm.deprecated_tooltip.2')}</span>
        <span>{translate('issue.transition.confirm.deprecated_tooltip.3')}</span>
      </div>
    ),
    [IssueTransition.Resolve]: (
      <div className="sw-flex sw-flex-col sw-gap-2">
        <span>{translate('issue.transition.resolve.deprecated_tooltip.1')}</span>
        <span>{translate('issue.transition.resolve.deprecated_tooltip.2')}</span>
        <span>{translate('issue.transition.resolve.deprecated_tooltip.3')}</span>
      </div>
    ),
  };

  return (
    <ItemButton
      key={transition}
      onClick={() => onSelectTransition(transition)}
      selected={selected}
      className="sw-px-4"
    >
      <div className="it__issue-transition-option sw-flex sw-flex-col">
        <PageContentFontWrapper className="sw-font-semibold sw-flex sw-gap-1 sw-items-center">
          <TextBold name={intl.formatMessage({ id: `issue.transition.${transition}` })} />
          {tooltips[transition] && (
            <Tooltip overlay={<div>{tooltips[transition]}</div>} placement={PopupPlacement.Right}>
              <HelperHintIcon />
            </Tooltip>
          )}
        </PageContentFontWrapper>
        <TextMuted text={translate('issue.transition', transition, 'description')} />
      </div>
    </ItemButton>
  );
}
