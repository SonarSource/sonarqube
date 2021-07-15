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

import * as React from 'react';
import './MetaData.css';
import MetaDataVersions from './MetaDataVersions';
import { MetaDataInformation } from './update-center-metadata';
import { isSuccessStatus } from '../../../helpers/request';

interface Props {
  updateCenterKey?: string;
}

interface State {
  data?: MetaDataInformation;
}

export default class MetaData extends React.Component<Props, State> {
  mounted = false;
  state: State = {};

  componentDidMount() {
    this.mounted = true;
    this.fetchData();
  }

  componentDidUpdate(prevProps: Props) {
    if (prevProps.updateCenterKey !== this.props.updateCenterKey) {
      this.fetchData();
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchData() {
    const { updateCenterKey } = this.props;

    if (updateCenterKey) {
      window
        .fetch(`https://update.sonarsource.org/${updateCenterKey}.json`)
        .then((response: Response) => {
          if (isSuccessStatus(response.status)) {
            return response.json();
          } else {
            return Promise.reject(response);
          }
        })
        .then((data) => {
          if (this.mounted) {
            this.setState({ data });
          }
        })
        .catch(() => {
          if (this.mounted) {
            this.setState({ data: undefined });
          }
        });
    } else {
      this.setState({ data: undefined });
    }
  }

  render() {
    const { data } = this.state;

    if (!data) {
      return null;
    }

    const { isSonarSourceCommercial, issueTrackerURL, license, organization, versions } = data;

    let vendor;
    if (organization) {
      vendor = organization.name;
      if (organization.url) {
        vendor = (
          <a href={organization.url} rel="noopener noreferrer" target="_blank">
            {vendor}
          </a>
        );
      }
    }

    return (
      <div className="update-center-meta-data">
        <div className="update-center-meta-data-header">
          {vendor && <span className="update-center-meta-data-vendor">By {vendor}</span>}
          {license && <span className="update-center-meta-data-license">{license}</span>}
          {issueTrackerURL && (
            <span className="update-center-meta-data-issue-tracker">
              <a href={issueTrackerURL} rel="noopener noreferrer" target="_blank">
                Issue Tracker
              </a>
            </span>
          )}
          {isSonarSourceCommercial && (
            <span className="update-center-meta-data-supported">Supported by SonarSource</span>
          )}
        </div>
        {versions && versions.length > 0 && <MetaDataVersions versions={versions} />}
      </div>
    );
  }
}
