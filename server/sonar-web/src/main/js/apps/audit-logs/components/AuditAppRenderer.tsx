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

import { Link, RadioButtonGroup } from '@sonarsource/echoes-react';
import { subDays } from 'date-fns';
import {
  DateRangePicker,
  LargeCenteredLayout,
  PageContentFontWrapper,
  PopupZLevel,
  Title,
} from 'design-system';
import * as React from 'react';
import { Helmet } from 'react-helmet-async';
import { FormattedMessage } from 'react-intl';
import Suggestions from '../../../components/embed-docs-modal/Suggestions';
import { now } from '../../../helpers/dates';
import { translate } from '../../../helpers/l10n';
import { queryToSearch } from '../../../helpers/urls';
import '../style.css';
import { HousekeepingPolicy, RangeOption } from '../utils';
import DownloadButton from './DownloadButton';

export interface AuditAppRendererProps {
  dateRange?: { from?: Date; to?: Date };
  downloadStarted: boolean;
  handleOptionSelection: (option: RangeOption) => void;
  handleDateSelection: (dateRange: { from?: Date; to?: Date }) => void;
  handleStartDownload: () => void;
  housekeepingPolicy: HousekeepingPolicy;
  selection: RangeOption;
}

const HOUSEKEEPING_MONTH_THRESHOLD = 30;
const HOUSEKEEPING_TRIMESTER_THRESHOLD = 90;

const HOUSEKEEPING_POLICY_VALUES = {
  [HousekeepingPolicy.Weekly]: 7,
  [HousekeepingPolicy.Monthly]: 30,
  [HousekeepingPolicy.Trimestrial]: 90,
  [HousekeepingPolicy.Yearly]: 365,
};

const getRangeOptions = (housekeepingPolicy: HousekeepingPolicy) => {
  const rangeOptions = [RangeOption.Today, RangeOption.Week];

  if (HOUSEKEEPING_POLICY_VALUES[housekeepingPolicy] >= HOUSEKEEPING_MONTH_THRESHOLD) {
    rangeOptions.push(RangeOption.Month);
  }

  if (HOUSEKEEPING_POLICY_VALUES[housekeepingPolicy] >= HOUSEKEEPING_TRIMESTER_THRESHOLD) {
    rangeOptions.push(RangeOption.Trimester);
  }

  rangeOptions.push(RangeOption.Custom);

  return rangeOptions;
};

export default function AuditAppRenderer(props: Readonly<AuditAppRendererProps>) {
  const { dateRange, downloadStarted, housekeepingPolicy, selection } = props;

  return (
    <LargeCenteredLayout as="main" id="audit-logs-page">
      <PageContentFontWrapper className="sw-body-sm sw-my-8">
        <Suggestions suggestions="audit-logs" />
        <Helmet title={translate('audit_logs.page')} />

        <Title>{translate('audit_logs.page')}</Title>

        <p className="sw-mb-4">
          {translate('audit_logs.page.description.1')}
          <br />
          <FormattedMessage
            id="audit_logs.page.description.2"
            defaultMessage={translate('audit_logs.page.description.2')}
            values={{
              housekeeping: translate('audit_logs.housekeeping_policy', housekeepingPolicy),
              link: (
                <Link
                  to={{
                    pathname: '/admin/settings',
                    search: queryToSearch({ category: 'housekeeping' }),
                    hash: '#auditLogs',
                  }}
                >
                  {translate('audit_logs.page.description.link')}
                </Link>
              ),
            }}
          />
        </p>

        <div className="sw-mb-6">
          <h3 className="sw-mb-4">{translate('audit_logs.download')}</h3>

          <RadioButtonGroup
            id="audit-logs-housekeeping-radio"
            options={getRangeOptions(housekeepingPolicy).map((option) => ({
              label: translate('audit_logs.range_option', option),
              value: option,
            }))}
            value={selection}
            onChange={props.handleOptionSelection}
          />

          <DateRangePicker
            className="sw-w-abs-350 sw-mt-4"
            clearButtonLabel={translate('clear')}
            fromLabel={translate('start_date')}
            onChange={props.handleDateSelection}
            separatorText={translate('to_')}
            toLabel={translate('end_date')}
            value={dateRange}
            minDate={subDays(now(), HOUSEKEEPING_POLICY_VALUES[housekeepingPolicy])}
            maxDate={now()}
            zLevel={PopupZLevel.Content}
          />
        </div>

        <DownloadButton
          dateRange={dateRange}
          downloadStarted={downloadStarted}
          onStartDownload={props.handleStartDownload}
          selection={selection}
        />
      </PageContentFontWrapper>
    </LargeCenteredLayout>
  );
}
