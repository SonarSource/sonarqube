/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import { setHomePage,skipOnboarding } from '../../api/users';
import { AppState } from '../../types/appstate';
import { CurrentUser } from '../../types/users';
import withAppStateContext from './app-state/withAppStateContext';
import withCurrentUserContext from './current-user/withCurrentUserContext';

import "../styles/components/home.css";
import { isNonStandardUser } from '../utils/userAccess';


interface Props {
  appState: AppState;
  currentUser: CurrentUser
  fetchLanguages: () => Promise<void>;
  fetchMyOrganizations: () => Promise<void>;
}

interface State {
    loading:boolean;
}

class Home extends React.PureComponent<Props, State> {
    mounted = false;
    state : State = {
        loading:false
    }

    componentDidMount() {
        this.mounted = true;
    }

    componentWillUnmount() {
        this.mounted = false;
    }

    handleProjectsClick = async() => {
        const url = "projects";
        const type: any = {type:"PROJECTS"}

        await setHomePage(type);
        await skipOnboarding();

        if (isNonStandardUser(this.props.currentUser))
            window.location.href = '/account';
        else
            window.location.href = window.location.href.replace("home", url);
    }

    handlePolicyClick = async() => { 
        const defaultOrg = (this.props.currentUser as any).orgGroups[0].organizationKey;
        const type: any = {type:"POLICY_RESULTS", organization: defaultOrg}
        
        await setHomePage(type);
        await skipOnboarding();
        
        const url = "organizations/"+defaultOrg+"/policy-results";
        window.location.href = window.location.href.replace("home",url);  
    }

    render() {
        const {loading} = this.state;
        return (
            <div className="landing">
                <div className="home">
                    <img className="light-emblem" src='/images/grc/CodeScanShieldEmblem.svg' alt="" />
                    <h1>Welcome to CodeScan</h1>
                    {
                        loading?(<div className="welcome-block"><i className="spinner"></i></div>):(
                            <div className="welcome-block">
                        <div className="block" style={{ marginRight: "20px" }}>
                            <span onClick={this.handleProjectsClick} className="icon-card">
                                <img className="grc-icon" src='/images/grc/codescan-dashboard.svg' alt="" /><br/>
                                <p>Application Security Testing</p>
                            </span >
                        </div>
                        <div className="block">
                            <span onClick={this.handlePolicyClick} className="icon-card">
                                <img className="grc-icon" src='/images/grc/orgscan-dashboard.svg' alt="" /><br/>
                                <p>Policy Management</p>
                            </span>
                        </div>        
                    </div>
                        )
                    }
                </div>
            </div>
        );
    }
}

export default withCurrentUserContext(withAppStateContext(Home));
