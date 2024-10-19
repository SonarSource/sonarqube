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
  ButtonSecondary,
  ChevronDownIcon,
  Dropdown,
  ItemButton,
  ItemDownload,
  PopupPlacement,
  PopupZLevel,
} from 'design-system';
import * as React from 'react';
import { getReportUrl } from '../../api/component-report';
import { translate, translateWithParameters } from '../../helpers/l10n';
import { Branch } from '../../types/branch-like';
import { Component } from '../../types/types';

export interface ComponentReportActionsRendererProps {
  branch?: Branch;
  canSubscribe: boolean;
  component: Component;
  currentUserHasEmail: boolean;
  frequency: string;
  handleSubscription: () => void;
  handleUnsubscription: () => void;
  subscribed: boolean;
}

const getSubscriptionText = ({
  currentUserHasEmail,
  frequency,
  subscribed,
}: Pick<
  ComponentReportActionsRendererProps,
  'currentUserHasEmail' | 'frequency' | 'subscribed'
>) => {
  if (!currentUserHasEmail) {
    return translate('component_report.no_email_to_subscribe');
  }

  const translationKey = subscribed
    ? 'component_report.unsubscribe_x'
    : 'component_report.subscribe_x';
  const frequencyTranslation = translate('report.frequency', frequency).toLowerCase();

  return translateWithParameters(translationKey, frequencyTranslation);
};

export default function ComponentReportActionsRenderer(props: ComponentReportActionsRendererProps) {
  const { branch, component, frequency, subscribed, canSubscribe, currentUserHasEmail } = props;

  const downloadName = [component.name, branch?.name, 'PDF Report.pdf']
    .filter((s) => !!s)
    .join(' - ');
  const reportUrl = getReportUrl(component.key, branch?.name);

  return canSubscribe ? (
    <Dropdown
      id="component-report"
      size="auto"
      allowResizing
      placement={PopupPlacement.BottomRight}
      zLevel={PopupZLevel.Default}
      overlay={
        <>
          <ItemDownload download={downloadName} href={reportUrl}>
            {translate('download_verb')}
          </ItemDownload>
          <ItemButton
            disabled={!currentUserHasEmail}
            data-test="overview__subscribe-to-report-button"
            onClick={subscribed ? props.handleUnsubscription : props.handleSubscription}
          >
            {getSubscriptionText({ currentUserHasEmail, frequency, subscribed })}
          </ItemButton>
        </>
      }
    >
      <ButtonSecondary>
        {translateWithParameters(
          'component_report.report',
          translate('qualifier', component.qualifier),
        )}
        <ChevronDownIcon className="sw-ml-1" />
      </ButtonSecondary>
    </Dropdown>
  ) : (
    <a download={downloadName} href={reportUrl} target="_blank" rel="noopener noreferrer">
      {translateWithParameters(
        'component_report.download',
        translate('qualifier', component.qualifier).toLowerCase(),
      )}
    </a>
  );
}
