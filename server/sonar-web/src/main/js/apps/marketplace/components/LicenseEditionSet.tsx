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
import { debounce } from 'lodash';
import { Edition, getLicensePreview } from '../../../api/marketplace';
import { translate, translateWithParameters } from '../../../helpers/l10n';

export interface Props {
  className?: string;
  edition: Edition;
  updateLicense: (license?: string, status?: string) => void;
}

interface State {
  license: string;
  loading: boolean;
  previewStatus?: string;
}

export default class LicenseEditionSet extends React.PureComponent<Props, State> {
  mounted: boolean;

  constructor(props: Props) {
    super(props);
    this.state = { license: '', loading: false };
    this.fetchLicensePreview = debounce(this.fetchLicensePreview, 250);
  }

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchLicensePreview = (license: string) =>
    getLicensePreview({ license }).then(
      r => {
        if (this.mounted) {
          this.updateLicense(license, r.previewStatus);
        }
      },
      () => {
        if (this.mounted) {
          this.updateLicense(license, undefined);
        }
      }
    );

  handleLicenseChange = (event: React.SyntheticEvent<HTMLTextAreaElement>) => {
    const license = event.currentTarget.value;
    if (license) {
      this.fetchLicensePreview(license);
      this.setState({ license });
    } else {
      this.updateLicense(license, undefined);
    }
  };

  updateLicense = (license: string, previewStatus?: string) => {
    this.setState({ license, previewStatus });
    this.props.updateLicense(license, previewStatus);
  };

  render() {
    const { className, edition } = this.props;
    const { license, previewStatus } = this.state;
    return (
      <div className={className}>
        <label htmlFor="set-license">
          {translateWithParameters('marketplace.enter_license_for_x', edition.name)}
          <em className="mandatory">*</em>
        </label>
        <textarea
          autoFocus={true}
          id="set-license"
          className="spacer-top display-block"
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
              edition.name
            )}
            {previewStatus === 'MANUAL_INSTALL' && (
              <p className="spacer-top">
                <a
                  className="button"
                  download={`sonarqube-${edition.name}.zip`}
                  href={edition.download_link}
                  target="_blank">
                  {translate('marketplace.download_package')}
                </a>
                <a
                  className="spacer-left"
                  href="https://redirect.sonarsource.com/doc/how-to-install-an-edition.html"
                  target="_blank">
                  {translate('marketplace.how_to_install')}
                </a>
              </p>
            )}
          </p>
        )}
        <a
          className="display-inline-block spacer-top"
          href={edition.request_license_link}
          target="_blank">
          {translate('marketplace.i_need_a_license')}
        </a>
      </div>
    );
  }
}
