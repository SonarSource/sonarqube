/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import DeferredSpinner from '../../../components/common/DeferredSpinner';
import OrganizationSelect from '../components/OrganizationSelect';
import { SubmitButton } from '../../../components/ui/buttons';
import { translate } from '../../../helpers/l10n';

interface Props {
  onBindOrganization: (organization: string) => Promise<void>;
  unboundOrganizations: T.Organization[];
}

interface State {
  organization: string;
  submitting: boolean;
}

export default class AutoOrganizationBind extends React.PureComponent<Props, State> {
  mounted = false;

  constructor(props: Props) {
    super(props);
    this.state = { organization: this.getInitialSelectedOrganization(props), submitting: false };
  }

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  getInitialSelectedOrganization(props: Props) {
    if (props.unboundOrganizations.length === 1) {
      return props.unboundOrganizations[0].key;
    }
    return '';
  }

  handleChange = ({ key }: T.Organization) => {
    this.setState({ organization: key });
  };

  handleSubmit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const { organization } = this.state;
    if (organization) {
      this.setState({ submitting: true });
      this.props.onBindOrganization(organization).then(this.stopSubmitting, this.stopSubmitting);
    }
  };

  stopSubmitting = () => {
    if (this.mounted) {
      this.setState({ submitting: false });
    }
  };

  render() {
    const { organization, submitting } = this.state;
    return (
      <form id="bind-organization-form" onSubmit={this.handleSubmit}>
        <OrganizationSelect
          onChange={this.handleChange}
          organization={organization}
          organizations={this.props.unboundOrganizations}
        />
        <div className="display-flex-center big-spacer-top">
          <SubmitButton disabled={submitting || !organization}>
            {translate('onboarding.import_organization.bind')}
          </SubmitButton>
          {submitting && <DeferredSpinner className="spacer-left" />}
        </div>
      </form>
    );
  }
}
