/*
 * Sonar UI Common
 * Copyright (C) 2019-2020 SonarSource SA
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
import { AdvancedDownloadUrl, MetaDataVersionInformation } from './update-center-metadata';

export interface MetaDataVersionProps {
  versionInformation: MetaDataVersionInformation;
}

export default function MetaDataVersion(props: MetaDataVersionProps) {
  const {
    versionInformation: {
      archived,
      changeLogUrl,
      compatibility,
      date,
      description,
      downloadURL,
      version,
    },
  } = props;

  const fallbackLabel = 'Download';

  const advancedDownloadUrls = isAdvancedDownloadUrlArray(downloadURL)
    ? downloadURL.map((url) => ({ ...url, label: url.label || fallbackLabel }))
    : [{ label: fallbackLabel, url: downloadURL }];

  return (
    <div
      className={classNames('update-center-meta-data-version', {
        'update-center-meta-data-version-archived': archived,
      })}>
      <div className="update-center-meta-data-version-version">{version}</div>

      <div className="update-center-meta-data-version-release-info">
        {date && <time className="update-center-meta-data-version-release-date">{date}</time>}

        {compatibility && (
          <span className="update-center-meta-data-version-compatibility">{compatibility}</span>
        )}
      </div>

      {description && (
        <div className="update-center-meta-data-version-release-description">{description}</div>
      )}

      {(advancedDownloadUrls.length > 0 || changeLogUrl) && (
        <div className="update-center-meta-data-version-release-links">
          {advancedDownloadUrls.length > 0 &&
            advancedDownloadUrls.map(
              (advancedDownloadUrl, i) =>
                advancedDownloadUrl.url && (
                  // eslint-disable-next-line react/no-array-index-key
                  <span className="update-center-meta-data-version-download" key={i}>
                    <a href={advancedDownloadUrl.url} rel="noopener noreferrer" target="_blank">
                      {advancedDownloadUrl.label}
                    </a>
                  </span>
                )
            )}

          {changeLogUrl && (
            <span className="update-center-meta-data-version-release-notes">
              <a href={changeLogUrl} rel="noopener noreferrer" target="_blank">
                Release notes
              </a>
            </span>
          )}
        </div>
      )}
    </div>
  );
}

function isAdvancedDownloadUrlArray(
  downloadUrl: string | AdvancedDownloadUrl[] | undefined
): downloadUrl is AdvancedDownloadUrl[] {
  return !!downloadUrl && typeof downloadUrl !== 'string';
}
