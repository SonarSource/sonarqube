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
import classNames from 'classnames';
import { sortBy } from 'lodash';
import Step from './Step';
import NewOrganizationForm from './NewOrganizationForm';
import { getOrganizations } from '../../../api/organizations';
import Select from '../../../components/controls/Select';
import { translate } from '../../../helpers/l10n';

/*::
type Props = {|
  currentUser: { login: string, isLoggedIn: boolean },
  finished: boolean,
  onOpen: () => void,
  onContinue: (organization: string) => void,
  open: boolean,
  stepNumber: number
|};
*/

/*::
type State = {
  loading: boolean,
  newOrganization?: string,
  existingOrganization?: string,
  existingOrganizations: Array<string>,
  personalOrganization?: string,
  selection: 'personal' | 'existing' | 'new'
};
*/

export default class OrganizationStep extends React.PureComponent {
  /*:: mounted: boolean; */
  /*:: props: Props; */
  state /*: State */ = {
    loading: true,
    existingOrganizations: [],
    selection: 'personal'
  };

  componentDidMount() {
    this.mounted = true;
    this.fetchOrganizations();
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchOrganizations = () => {
    getOrganizations({ member: true }).then(
      ({ organizations }) => {
        if (this.mounted) {
          const organizationKeys = organizations.filter(o => o.isAdmin).map(o => o.key);
          // best guess: if there is only one organization, then it is personal
          // otherwise, we can't guess, let's display them all as just "existing organizations"
          const personalOrganization =
            organizationKeys.length === 1 ? organizationKeys[0] : undefined;
          const existingOrganizations = organizationKeys.length > 1 ? sortBy(organizationKeys) : [];
          const selection = personalOrganization
            ? 'personal'
            : existingOrganizations.length > 0 ? 'existing' : 'new';
          this.setState({
            loading: false,
            existingOrganizations,
            personalOrganization,
            selection
          });
        }
      },
      () => {
        if (this.mounted) {
          this.setState({ loading: false });
        }
      }
    );
  };

  getSelectedOrganization = () => {
    switch (this.state.selection) {
      case 'personal':
        return this.state.personalOrganization;
      case 'existing':
        return this.state.existingOrganization;
      case 'new':
        return this.state.newOrganization;
      default:
        return null;
    }
  };

  handlePersonalClick = (event /*: Event */) => {
    event.preventDefault();
    this.setState({ selection: 'personal' });
  };

  handleExistingClick = (event /*: Event */) => {
    event.preventDefault();
    this.setState({ selection: 'existing' });
  };

  handleNewClick = (event /*: Event */) => {
    event.preventDefault();
    this.setState({ selection: 'new' });
  };

  handleOrganizationCreate = (newOrganization /*: string */) => {
    this.setState({ newOrganization });
  };

  handleOrganizationDelete = () => {
    this.setState({ newOrganization: undefined });
  };

  handleExistingOrganizationSelect = ({ value } /*: { value: string } */) => {
    this.setState({ existingOrganization: value });
  };

  handleContinueClick = (event /*: Event */) => {
    event.preventDefault();
    const organization = this.getSelectedOrganization();
    if (organization) {
      this.props.onContinue(organization);
    }
  };

  renderPersonalOrganizationOption = () => (
    <div>
      <a className="link-base-color link-no-underline" href="#" onClick={this.handlePersonalClick}>
        <i
          className={classNames('icon-radio', 'spacer-right', {
            'is-checked': this.state.selection === 'personal'
          })}
        />
        {translate('onboarding.organization.my_personal_organization')}
        <span className="note spacer-left">{this.state.personalOrganization}</span>
      </a>
    </div>
  );

  renderExistingOrganizationOption = () => (
    <div className="big-spacer-top">
      <a
        className="js-existing link-base-color link-no-underline"
        href="#"
        onClick={this.handleExistingClick}>
        <i
          className={classNames('icon-radio', 'spacer-right', {
            'is-checked': this.state.selection === 'existing'
          })}
        />
        {translate('onboarding.organization.exising_organization')}
      </a>
      {this.state.selection === 'existing' && (
        <div className="big-spacer-top">
          <Select
            className="input-super-large"
            clearable={false}
            onChange={this.handleExistingOrganizationSelect}
            options={this.state.existingOrganizations.map(organization => ({
              label: organization,
              value: organization
            }))}
            value={this.state.existingOrganization}
          />
        </div>
      )}
    </div>
  );

  renderNewOrganizationOption = () => (
    <div className="big-spacer-top">
      <a
        className="js-new link-base-color link-no-underline"
        href="#"
        onClick={this.handleNewClick}>
        <i
          className={classNames('icon-radio', 'spacer-right', {
            'is-checked': this.state.selection === 'new'
          })}
        />
        {translate('onboarding.organization.create_another_organization')}
      </a>
      {this.state.selection === 'new' && (
        <div className="big-spacer-top">
          <NewOrganizationForm
            onDelete={this.handleOrganizationDelete}
            onDone={this.handleOrganizationCreate}
            organization={this.state.newOrganization}
          />
        </div>
      )}
    </div>
  );

  renderForm = () => {
    return (
      <div className="boxed-group-inner">
        <div className="big-spacer-bottom width-50">
          {translate('onboarding.organization.text')}
        </div>

        {this.state.loading ? (
          <i className="spinner" />
        ) : (
          <div>
            {this.state.personalOrganization && this.renderPersonalOrganizationOption()}
            {this.state.existingOrganizations.length > 0 && this.renderExistingOrganizationOption()}
            {this.renderNewOrganizationOption()}
          </div>
        )}

        {this.getSelectedOrganization() != null &&
          !this.state.loading && (
            <div className="big-spacer-top">
              <button className="js-continue" onClick={this.handleContinueClick}>
                {translate('continue')}
              </button>
            </div>
          )}
      </div>
    );
  };

  renderResult = () => {
    const result = this.getSelectedOrganization();

    return result != null ? (
      <div className="boxed-group-actions">
        <i className="icon-check spacer-right" />
        <strong>{result}</strong>
      </div>
    ) : null;
  };

  render() {
    return (
      <Step
        finished={this.props.finished}
        onOpen={this.props.onOpen}
        open={this.props.open}
        renderForm={this.renderForm}
        renderResult={this.renderResult}
        stepNumber={this.props.stepNumber}
        stepTitle={translate('onboarding.organization.header')}
      />
    );
  }
}
