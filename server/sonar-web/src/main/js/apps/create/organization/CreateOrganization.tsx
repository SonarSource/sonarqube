/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import {differenceInMinutes} from 'date-fns';
import {connect} from 'react-redux';
import {Helmet} from 'react-helmet-async';
import {withRouter, WithRouterProps} from 'react-router';
import {createOrganization, updateOrganization} from './actions';
import {ORGANIZATION_IMPORT_REDIRECT_TO_PROJECT_TIMESTAMP, Step} from './utils';
import ManualOrganizationCreate from './ManualOrganizationCreate';
import DeferredSpinner from 'sonar-ui-common/components/ui/DeferredSpinner';
import {whenLoggedIn} from '../../../components/hoc/whenLoggedIn';
import {withUserOrganizations} from '../../../components/hoc/withUserOrganizations';
import {deleteOrganization} from '../../organizations/actions';
import {translate} from 'sonar-ui-common/helpers/l10n';
import {addWhitePageClass, removeWhitePageClass} from 'sonar-ui-common/helpers/pages';
import {get, remove} from 'sonar-ui-common/helpers/storage';
import {getOrganizationUrl} from '../../../helpers/urls';
// import { skipOnboarding } from '../../../store/users';

interface Props {
    createOrganization: (
        organization: T.Organization & { installationId?: string }
    ) => Promise<string>;
    currentUser: T.LoggedInUser;
    deleteOrganization: (key: string) => Promise<void>;
    updateOrganization: (
        organization: T.Organization & { installationId?: string }
    ) => Promise<string>;
    userOrganizations: T.Organization[];
    // skipOnboarding: () => void;
}

interface State {
    boundOrganization?: T.OrganizationBase;
    loading: boolean;
    organization?: T.Organization;
    step: Step;
    subscriptionPlans?: T.SubscriptionPlan[];
}

export class CreateOrganization extends React.PureComponent<Props & WithRouterProps, State> {
    mounted = false;
    state: State = {
        loading: true,
        step: Step.OrganizationDetails
    };

    componentDidMount() {
        this.mounted = true;
        addWhitePageClass();

        const initRequests = [this.fetchSubscriptionPlans()];
        Promise.all(initRequests).then(this.stopLoading, this.stopLoading);
    }

    componentWillUnmount() {
        this.mounted = false;
        removeWhitePageClass();
    }

    deleteOrganization = () => {
        if (this.state.organization) {
            this.props.deleteOrganization(this.state.organization.key);
        }
    };

    fetchSubscriptionPlans = () => {
        this.setState({
            subscriptionPlans: []
        });
    };

    handleOrgCreated = (organization: string) => {
        // this.props.skipOnboarding();
        if (this.isStoredTimestampValid(ORGANIZATION_IMPORT_REDIRECT_TO_PROJECT_TIMESTAMP)) {
            this.props.router.push({
                pathname: '/projects/create',
                state: {organization}
            });
        } else {
            this.props.router.push({pathname: getOrganizationUrl(organization)});
        }
    };

    handleOrgDetailsFinish = (organization: T.Organization) => {
        this.setState({organization, step: Step.Plan});
        return Promise.resolve();
    };

    handleOrgDetailsStepOpen = () => {
        this.setState({step: Step.OrganizationDetails});
    };

    isStoredTimestampValid = (timestampKey: string) => {
        const storedTimestamp = get(timestampKey);
        remove(timestampKey);
        return storedTimestamp && differenceInMinutes(Date.now(), Number(storedTimestamp)) < 10;
    };

    stopLoading = () => {
        if (this.mounted) {
            this.setState({loading: false});
        }
    };

    handlePlanDone = () => {
        if (this.state.organization) {
            this.handleOrgCreated(this.state.organization.key);
        }
    };

    renderContent = () => {
        const {state} = this;
        const {organization, step, subscriptionPlans} = state;

        const commonProps = {
            handleOrgDetailsFinish: this.handleOrgDetailsFinish,
            handleOrgDetailsStepOpen: this.handleOrgDetailsStepOpen,
            onDone: this.handlePlanDone,
            organization,
            step,
            subscriptionPlans
        };

        return (
            <ManualOrganizationCreate
                {...commonProps}
                createOrganization={this.props.createOrganization}
                onUpgradeFail={this.deleteOrganization}
                organization={this.state.organization}
                step={this.state.step}
            />
        );
    };

    render() {
        const header = translate('onboarding.create_organization.page.header');

        return (
            <>
                <Helmet title={header} titleTemplate="%s"/>
                <div className="page page-limited huge-spacer-top huge-spacer-bottom">
                    <header className="page-header huge-spacer-bottom">
                        <h1 className="page-title huge big-spacer-bottom">
                            <strong>{header}</strong>
                        </h1>
                    </header>
                    {this.state.loading ? (
                        <DeferredSpinner/>
                    ) : (
                        this.renderContent()
                    )}
                </div>
            </>
        );
    }
}

const mapDispatchToProps = {
    // skipOnboarding: skipOnboarding as any,
    createOrganization: createOrganization as any,
    deleteOrganization: deleteOrganization as any,
    updateOrganization: updateOrganization as any
};

export default whenLoggedIn(
    withUserOrganizations(
        withRouter(
            connect(
                null,
                mapDispatchToProps
            )(CreateOrganization)
        )
    )
);
