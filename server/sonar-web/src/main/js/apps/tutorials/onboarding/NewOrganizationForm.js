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
// @flow
import React from 'react';
import { debounce } from 'lodash';
import {
  createOrganization,
  deleteOrganization,
  getOrganization
} from '../../../api/organizations';
import { DeleteButton } from '../../../components/ui/buttons';
import { translate } from '../../../helpers/l10n';

/*::
type Props = {|
  onDelete: () => void,
  onDone: (organization: string) => void,
  organization?: string
|};
*/

/*::
type State = {
  done: boolean,
  loading: boolean,
  organization: string,
  unique: boolean
};
*/

export default class NewOrganizationForm extends React.PureComponent {
  /*:: mounted: boolean; */
  /*:: props: Props; */
  /*:: state: State; */

  constructor(props /*: Props */) {
    super(props);
    this.state = {
      done: props.organization != null,
      loading: false,
      organization: props.organization || '',
      unique: true
    };
    this.validateOrganization = debounce(this.validateOrganization, 500);
  }

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  stopLoading = () => {
    if (this.mounted) {
      this.setState({ loading: false });
    }
  };

  validateOrganization = (organization /*: string */) => {
    getOrganization(organization).then(response => {
      if (this.mounted) {
        this.setState({ unique: response == null });
      }
    });
  };

  sanitizeOrganization = (organization /*: string */) =>
    organization
      .toLowerCase()
      .replace(/[^a-z0-9-]/, '')
      .replace(/^-/, '');

  handleOrganizationChange = (event /*: { target: HTMLInputElement } */) => {
    const organization = this.sanitizeOrganization(event.target.value);
    this.setState({ organization });
    this.validateOrganization(organization);
  };

  handleOrganizationCreate = (event /*: Event */) => {
    event.preventDefault();
    const { organization } = this.state;
    if (organization) {
      this.setState({ loading: true });
      createOrganization({ key: organization, name: organization }).then(() => {
        if (this.mounted) {
          this.setState({ done: true, loading: false });
          this.props.onDone(organization);
        }
      }, this.stopLoading);
    }
  };

  handleOrganizationDelete = () => {
    const { organization } = this.state;
    if (organization) {
      this.setState({ loading: true });
      deleteOrganization(organization).then(() => {
        if (this.mounted) {
          this.setState({ done: false, loading: false, organization: '' });
          this.props.onDelete();
        }
      }, this.stopLoading);
    }
  };

  render() {
    const { done, loading, organization, unique } = this.state;

    const valid = unique && organization.length >= 2;

    return done ? (
      <div>
        <span className="spacer-right text-middle">{organization}</span>
        {loading ? (
          <i className="spinner text-middle" />
        ) : (
          <DeleteButton className="button-small" onClick={this.handleOrganizationDelete} />
        )}
      </div>
    ) : (
      <form onSubmit={this.handleOrganizationCreate}>
        <input
          autoFocus={true}
          className="input-super-large spacer-right text-middle"
          onChange={this.handleOrganizationChange}
          maxLength={32}
          minLength={2}
          placeholder={translate('onboarding.organization.placeholder')}
          required={true}
          type="text"
          value={organization}
        />
        {loading ? (
          <i className="spinner text-middle" />
        ) : (
          <button className="text-middle" disabled={!valid}>
            {translate('create')}
          </button>
        )}
        {!unique && (
          <span className="big-spacer-left text-danger text-middle">
            <i className="icon-alert-error little-spacer-right text-text-top" />
            {translate('this_name_is_already_taken')}
          </span>
        )}
        <div className="note spacer-top abs-width-300">
          {translate('onboarding.organization.key_requirement')}
        </div>
      </form>
    );
  }
}
