/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import * as classNames from 'classnames';
import { stringify } from 'querystring';
import { debounce } from 'lodash';
import { omitNil } from '../../../helpers/request';
import { Edition, getFormData, getLicensePreview } from '../../../api/marketplace';
import { translate, translateWithParameters } from '../../../helpers/l10n';

export interface Props {
  className?: string;
  edition?: Edition;
  editions: Edition[];
  updateLicense: (license?: string, status?: string) => void;
}

interface State {
  license: string;
  licenseEdition?: Edition;
  previewStatus?: string;
  formData?: {
    serverId?: string;
    ncloc?: number;
  };
}

export default class LicenseEditionSet extends React.PureComponent<Props, State> {
  mounted: boolean;

  constructor(props: Props) {
    super(props);
    this.state = { license: '' };
    this.fetchLicensePreview = debounce(this.fetchLicensePreview, 100);
  }

  componentDidMount() {
    this.mounted = true;
    this.fetchFormData();
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchLicensePreview = (license: string) =>
    getLicensePreview({ license }).then(
      ({ previewStatus, nextEditionKey }) => {
        if (this.mounted) {
          this.updateLicense(
            license,
            this.props.editions.find(edition => edition.key === nextEditionKey),
            previewStatus
          );
        }
      },
      () => {
        if (this.mounted) {
          this.updateLicense(license, undefined, undefined);
        }
      }
    );

  fetchFormData = () => {
    getFormData().then(
      formData => {
        if (this.mounted) {
          this.setState({ formData });
        }
      },
      () => {}
    );
  };

  getLicenseFormUrl = (edition: Edition) => {
    let url = edition.requestUrl;
    if (this.state.formData) {
      const query = stringify(omitNil(this.state.formData));
      if (query) {
        url += '?' + query;
      }
    }
    return url;
  };

  handleLicenseChange = (event: React.SyntheticEvent<HTMLTextAreaElement>) => {
    const license = event.currentTarget.value;
    if (license) {
      this.fetchLicensePreview(license);
      this.setState({ license });
    } else {
      this.updateLicense(license, undefined, undefined);
    }
  };

  updateLicense = (license: string, licenseEdition?: Edition, previewStatus?: string) => {
    this.setState({ license, licenseEdition, previewStatus });
    this.props.updateLicense(license, previewStatus);
  };

  render() {
    const { className, edition } = this.props;
    const { license, licenseEdition, previewStatus } = this.state;

    return (
      <div className={className}>
        {edition && (
          <label className="display-inline-block spacer-bottom" htmlFor="set-license">
            {translateWithParameters('marketplace.enter_license_for_x', edition.name)}
            <em className="mandatory">*</em>
          </label>
        )}
        <textarea
          autoFocus={true}
          id="set-license"
          className="display-block"
          cols={62}
          onChange={this.handleLicenseChange}
          required={true}
          rows={6}
          value={license}
        />
        {previewStatus && (
          <p
            className={classNames('alert spacer-top', {
              'alert-warning': previewStatus === 'AUTOMATIC_INSTALL',
              'alert-success': previewStatus === 'NO_INSTALL',
              'alert-danger': previewStatus === 'MANUAL_INSTALL'
            })}>
            {translateWithParameters(
              'marketplace.license_preview_status.' + previewStatus,
              licenseEdition ? licenseEdition.name : translate('marketplace.commercial_edition')
            )}
          </p>
        )}
        {edition && (
          <a
            className="display-inline-block spacer-top"
            href={this.getLicenseFormUrl(edition)}
            target="_blank">
            {translate('marketplace.i_need_a_license')}
          </a>
        )}
      </div>
    );
  }
}
