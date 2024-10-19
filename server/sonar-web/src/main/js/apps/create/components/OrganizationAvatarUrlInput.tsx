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
import classNames from 'classnames';
import { isWebUri } from 'valid-url';
import { translate } from "../../../helpers/l10n";
import { getWhiteListDomains } from '../../../api/organizations';
import { throwGlobalError } from '~sonar-aligned/helpers/error';
import withAppStateContext from '../../../../js/app/components/app-state/withAppStateContext';
import { AppState } from '../../../types/appstate';
import { allowSpecificDomains } from '../../../helpers/urls';
import { FormField, InputField } from "design-system";

interface Props {
  initialValue?: string;
  appState: AppState;
  onChange: (value: string | undefined) => void;
}

interface State {
  editing: boolean;
  error?: string;
  touched: boolean;
  value: string;
}

class OrganizationAvatarUrlInput extends React.PureComponent<Props, State> {
  state: State = {error: undefined, editing: false, touched: false, value: ''};
  whiteListDomains: string[] = [];

  async fetchWhiteListDomains() {
    await getWhiteListDomains().then((data : string[])=>{
      this.whiteListDomains = data;
    },
    throwGlobalError)
  }

  async componentDidMount() {
    await this.fetchWhiteListDomains();
    setTimeout(()=>{
      if (this.props.initialValue) {
        const value = this.props.initialValue;
        const error = this.validateUrl(value);
        this.setState({error, touched: Boolean(error), value});
      }
    },0)
  }  

  handleChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const value = event.currentTarget.value.trim();
    const error = this.validateUrl(value);
    this.setState({error, touched: true, value});
    this.props.onChange(error === undefined ? value : undefined);
  };

  handleBlur = () => {
    this.setState({editing: false});
  };

  handleFocus = () => {
    this.setState({editing: true});
  };

  domainFromUrl = (url:string) => {
    let result;
    let match;
    if (match = url.match(/^(?:https?:\/\/)?(?:[^@\n]+@)?(?:www\.)?([^:\/\n\?\=]+)/im)) {
        result = match[1]
        if (match = result.match(/^[^\.]+\.(.+\..+)$/)) {
            result = match[1]
        }
    }
    return result
  }
  
  isValidDomain = (url : string) => {
    const validDomainUrls = this.whiteListDomains;
    let isUrlValid = false;
    
    let domain = this.domainFromUrl(url);
    for(const element of validDomainUrls) {
      if(domain?.endsWith(element)) {
        isUrlValid = true;
        break;
      }  
    }
    return isUrlValid;
  }
  

  validateUrl=(url: string)=> {
    const { whiteLabel } = this.props.appState
    if (url.length > 0 && !isWebUri(url) ){
      return translate('onboarding.create_organization.url.error');
    }
    if(allowSpecificDomains(whiteLabel)  && url.length > 0 && !this.isValidDomain(url)){
      return translate('onboarding.create_organization.url.domain.error');
    }
    return undefined;
  }

  render() {
    const isInvalid = this.state.touched && !this.state.editing && this.state.error !== undefined;
    const isValid = this.state.touched && this.state.error === undefined && this.state.value !== '';
    return (
        <FormField
          htmlFor="organization-avatar"
          label={translate('onboarding.create_organization.avatar')}
          error={this.state.error}
          isInvalid={isInvalid}
          isValid={isValid}
        >
          <InputField
            className={classNames('input-super-large', 'text-middle', {
              'is-invalid': isInvalid,
              'is-valid': isValid
            })}
            id="organization-avatar"
            onBlur={this.handleBlur}
            onChange={this.handleChange}
            onFocus={this.handleFocus}
            type="text"
            value={this.state.value}
          />
        </FormField>
    );
  }
}


export default withAppStateContext(OrganizationAvatarUrlInput);