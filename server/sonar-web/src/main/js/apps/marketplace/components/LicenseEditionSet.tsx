/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import { stringify } from 'querystring';
import * as React from 'react';
import * as classNames from 'classnames';
import { FormattedMessage } from 'react-intl';
import { debounce } from 'lodash';
import Checkbox from '../../../components/controls/Checkbox';
import DeferredSpinner from '../../../components/common/DeferredSpinner';
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
  acceptTerms: boolean;
  license: string;
  licenseEdition?: Edition;
  loading: boolean;
  previewStatus?: string;
  formData?: {
    serverId?: string;
    ncloc?: number;
  };
}

export default class LicenseEditionSet extends React.PureComponent<Props, State> {
  mounted = false;

  constructor(props: Props) {
    super(props);
    this.state = { acceptTerms: false, license: '', loading: false };
    this.fetchLicensePreview = debounce(this.fetchLicensePreview, 100);
  }

  componentDidMount() {
    this.mounted = true;
    this.fetchFormData();
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchLicensePreview = (license: string) => {
    this.setState({ loading: true });
    getLicensePreview({ license }).then(
      ({ previewStatus, nextEditionKey }) => {
        if (this.mounted) {
          const { edition } = this.props;
          this.updateLicense(
            license,
            this.props.editions.find(edition => edition.key === nextEditionKey),
            edition && edition.key !== nextEditionKey ? undefined : previewStatus
          );
        }
      },
      () => {
        if (this.mounted) {
          this.updateLicense(license, undefined, undefined);
        }
      }
    );
  };

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
    let url = edition.licenseRequestUrl;
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

  handleTermsCheck = (checked: boolean) =>
    this.setState({ acceptTerms: checked }, () =>
      this.updateLicense(this.state.license, this.state.licenseEdition, this.state.previewStatus)
    );

  updateLicense = (license: string, licenseEdition?: Edition, previewStatus?: string) => {
    this.setState({ license, licenseEdition, loading: false, previewStatus });
    this.props.updateLicense(
      previewStatus !== 'NO_INSTALL' && !this.state.acceptTerms ? undefined : license,
      previewStatus
    );
  };

  renderAlert() {
    const { licenseEdition, previewStatus } = this.state;
    if (!previewStatus) {
      const { edition } = this.props;
      if (!edition) {
        return undefined;
      }

      return (
        <div className="spacer-top">
          {licenseEdition !== undefined &&
            edition.key !== licenseEdition.key && (
              <p className="alert alert-danger">
                {translateWithParameters('marketplace.wrong_license_type_x', edition.name)}
              </p>
            )}
          <a href={this.getLicenseFormUrl(edition)} target="_blank">
            {translate('marketplace.i_need_a_license')}
          </a>
        </div>
      );
    }

    return (
      <div className="spacer-top">
        <p
          className={classNames('alert', {
            'alert-warning': previewStatus === 'AUTOMATIC_INSTALL',
            'alert-success': previewStatus === 'NO_INSTALL',
            'alert-danger': previewStatus === 'MANUAL_INSTALL'
          })}>
          {translateWithParameters(
            'marketplace.license_preview_status.' + previewStatus,
            licenseEdition ? licenseEdition.name : translate('marketplace.commercial_edition')
          )}
          {licenseEdition &&
            licenseEdition.key === 'datacenter' &&
            previewStatus !== 'NO_INSTALL' && (
              <span className="little-spacer-left">
                <FormattedMessage
                  defaultMessage={translate('marketplace.how_to_setup_cluster_url')}
                  id="marketplace.how_to_setup_cluster_url"
                  values={{
                    url: (
                      <a
                        href="https://redirect.sonarsource.com/doc/data-center-edition.html"
                        target="_blank">
                        {licenseEdition.name}
                      </a>
                    )
                  }}
                />
              </span>
            )}
        </p>
        {previewStatus !== 'NO_INSTALL' && (
          <span className="js-edition-tos">
            <Checkbox
              checked={this.state.acceptTerms}
              id="edition-terms"
              onCheck={this.handleTermsCheck}>
              <label className="little-spacer-left" htmlFor="edition-terms">
                {translate('marketplace.i_accept_the')}
              </label>
            </Checkbox>
            <a
              className="nowrap little-spacer-left"
              href="http://dist.sonarsource.com/SonarSource_Terms_And_Conditions.pdf"
              target="_blank">
              {translate('marketplace.terms_and_conditions')}
            </a>
          </span>
        )}
      </div>
    );
  }

  render() {
    const { className, edition } = this.props;
    const { license, loading } = this.state;

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
          className="display-block input-super-large"
          onChange={this.handleLicenseChange}
          required={true}
          rows={8}
          style={{ resize: 'none' }}
          value={license}
        />

        <DeferredSpinner
          loading={loading}
          customSpinner={
            <p className="spacer-top">
              <i className="spinner spacer-right text-bottom" />
              {translate('marketplace.checking_license')}
            </p>
          }>
          {this.renderAlert()}
        </DeferredSpinner>
      </div>
    );
  }
}
