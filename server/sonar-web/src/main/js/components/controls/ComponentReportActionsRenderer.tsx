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
import { getReportUrl } from '../../api/component-report';
import { Button } from '../../components/controls/buttons';
import Dropdown from '../../components/controls/Dropdown';
import DropdownIcon from '../../components/icons/DropdownIcon';
import { translate, translateWithParameters } from '../../helpers/l10n';
import { Branch } from '../../types/branch-like';
import { Component } from '../../types/types';

export interface ComponentReportActionsRendererProps {
  component: Component;
  branch?: Branch;
  frequency: string;
  subscribed: boolean;
  canSubscribe: boolean;
  currentUserHasEmail: boolean;
  handleSubscription: () => void;
  handleUnsubscription: () => void;
}

export default function ComponentReportActionsRenderer(props: ComponentReportActionsRendererProps) {
  const { branch, component, frequency, subscribed, canSubscribe, currentUserHasEmail } = props;

  const renderDownloadButton = (simple = false) => {
    return (
      <a
        download={[component.name, branch?.name, 'PDF Report.pdf'].filter((s) => !!s).join(' - ')}
        href={getReportUrl(component.key, branch?.name)}
        target="_blank"
        data-test="overview__download-pdf-report-button"
        rel="noopener noreferrer"
      >
        {simple
          ? translate('download_verb')
          : translateWithParameters(
              'component_report.download',
              translate('qualifier', component.qualifier).toLowerCase()
            )}
      </a>
    );
  };

  const renderSubscriptionButton = () => {
    if (!currentUserHasEmail) {
      return (
        <span className="text-muted-2">{translate('component_report.no_email_to_subscribe')}</span>
      );
    }

    const translationKey = subscribed
      ? 'component_report.unsubscribe_x'
      : 'component_report.subscribe_x';
    const onClickHandler = subscribed ? props.handleUnsubscription : props.handleSubscription;
    const frequencyTranslation = translate('report.frequency', frequency).toLowerCase();

    return (
      <a href="#" onClick={onClickHandler} data-test="overview__subscribe-to-report-button">
        {translateWithParameters(translationKey, frequencyTranslation)}
      </a>
    );
  };

  return canSubscribe ? (
    <Dropdown
      overlay={
        <ul className="menu">
          <li>{renderDownloadButton(true)}</li>
          <li>{renderSubscriptionButton()}</li>
        </ul>
      }
    >
      <Button className="dropdown-toggle">
        {translateWithParameters(
          'component_report.report',
          translate('qualifier', component.qualifier)
        )}
        <DropdownIcon className="spacer-left" />
      </Button>
    </Dropdown>
  ) : (
    renderDownloadButton()
  );
}
