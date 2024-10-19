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

import { endOfDay, startOfDay, subDays } from 'date-fns';
import { ButtonPrimary } from 'design-system';
import * as React from 'react';
import { now } from '../../../helpers/dates';
import { translate } from '../../../helpers/l10n';
import '../style.css';
import { RangeOption } from '../utils';

export interface DownloadButtonProps {
  dateRange?: { from?: Date; to?: Date };
  downloadStarted: boolean;
  onStartDownload: () => void;
  selection: RangeOption;
}

const RANGE_OPTION_START = {
  [RangeOption.Today]: () => now(),
  [RangeOption.Week]: () => subDays(now(), 7),
  [RangeOption.Month]: () => subDays(now(), 30),
  [RangeOption.Trimester]: () => subDays(now(), 90),
};

const toISODateString = (date: Date) => date.toISOString();

function getRangeParams(selection: RangeOption, dateRange?: { from?: Date; to?: Date }) {
  if (selection === RangeOption.Custom) {
    // dateRange should be complete if 'custom' is selected
    // This is not strickly necessary since submit is disable
    // when the if condition is true.
    if (!(dateRange?.to && dateRange?.from)) {
      return '';
    }

    return new URLSearchParams({
      from: toISODateString(startOfDay(dateRange.from)),
      to: toISODateString(endOfDay(dateRange.to)),
    }).toString();
  }

  return new URLSearchParams({
    from: toISODateString(startOfDay(RANGE_OPTION_START[selection]())),
    to: toISODateString(now()),
  }).toString();
}

export default function DownloadButton(props: Readonly<DownloadButtonProps>) {
  const { dateRange, downloadStarted, selection } = props;

  const downloadDisabled =
    downloadStarted ||
    (selection === RangeOption.Custom &&
      (dateRange?.from === undefined || dateRange?.to === undefined));

  const downloadUrl = downloadDisabled
    ? '#'
    : `/api/audit_logs/download?${getRangeParams(selection, dateRange)}`;

  return (
    <>
      <ButtonPrimary
        download="audit_logs.json"
        disabled={downloadDisabled}
        aria-disabled={downloadDisabled}
        onClick={downloadDisabled ? undefined : props.onStartDownload}
        to={downloadUrl}
        target="_blank"
      >
        {translate('download_verb')}
      </ButtonPrimary>

      {downloadStarted && (
        <div className="sw-mt-2">
          <p>{translate('audit_logs.download_start.sentence.1')}</p>
          <p>{translate('audit_logs.download_start.sentence.2')}</p>
          <br />
          <p>{translate('audit_logs.download_start.sentence.3')}</p>
        </div>
      )}
    </>
  );
}
