/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import classNames from 'classnames';
import * as React from 'react';
import { getRegulatoryReportUrl } from '../../api/regulatory-report';
import { isBranch } from '../../helpers/branch-like';
import { translate } from '../../helpers/l10n';
import { BranchLike } from '../../types/branch-like';
import { Component } from '../../types/types';

interface Props {
  component: Pick<Component, 'key' | 'name'>;
  branchLike?: BranchLike;
}

function RegulatoryReport(props: Props) {
  const { component, branchLike } = props;
  const branchName = branchLike && isBranch(branchLike) ? branchLike.name : undefined;
  const [downloadStarted, setDownloadStarted] = React.useState(false);
  return (
    <div className="page page-limited">
      <header className="page-header">
        <h1 className="page-title">{translate('regulatory_report.page')}</h1>
      </header>
      <div className="page-description">
        <p>{translate('regulatory_report.description1')}</p>
        <p>{translate('regulatory_report.description2')}</p>
        <div className="big-spacer-top">
          <a
            className={classNames('button button-primary', { disabled: downloadStarted })}
            download={[component.name, branchName, 'PDF Report'].filter(s => !!s).join(' - ')}
            onClick={() => setDownloadStarted(true)}
            href={getRegulatoryReportUrl(component.key, branchName)}
            target="_blank"
            rel="noopener noreferrer">
            {translate('download_verb')}
          </a>
          {downloadStarted && (
            <div className="spacer-top">
              <p>{translate('regulatory_page.download_start.sentence')}</p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

export default RegulatoryReport;
