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
import * as classNames from 'classnames';
import { debounce } from 'lodash';
import * as React from 'react';
import ValidationInput from 'sonar-ui-common/components/controls/ValidationInput';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { doesComponentExists } from '../../../api/components';

interface Props {
  className?: string;
  value: string;
  onChange: (value: string | undefined) => void;
}

interface State {
  error?: string;
  touched: boolean;
  validating: boolean;
}

export default class ProjectKeyInput extends React.PureComponent<Props, State> {
  mounted = false;
  constructor(props: Props) {
    super(props);
    this.state = { error: undefined, touched: false, validating: false };
    this.checkFreeKey = debounce(this.checkFreeKey, 250);
  }

  componentDidMount() {
    this.mounted = true;
    if (this.props.value) {
      this.validateKey(this.props.value);
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  checkFreeKey = (key: string) => {
    this.setState({ validating: true });
    return doesComponentExists({ component: key })
      .then(alreadyExist => {
        if (this.mounted && key === this.props.value) {
          if (!alreadyExist) {
            this.setState({ error: undefined, validating: false });
          } else {
            this.setState({
              error: translate('onboarding.create_project.project_key.taken'),
              touched: true,
              validating: false
            });
          }
        }
      })
      .catch(() => {
        if (this.mounted && key === this.props.value) {
          this.setState({ error: undefined, validating: false });
        }
      });
  };

  handleChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const { value } = event.currentTarget;
    this.setState({ touched: true });
    this.validateKey(value);
    this.props.onChange(value);
  };

  validateKey(key: string) {
    if (key.length > 400 || !/^[\w-.:]*[a-zA-Z]+[\w-.:]*$/.test(key)) {
      this.setState({
        error: translate('onboarding.create_project.project_key.error'),
        touched: true
      });
    } else {
      this.checkFreeKey(key);
    }
  }

  render() {
    const isInvalid = this.state.touched && this.state.error !== undefined;
    const isValid = this.state.touched && !this.state.validating && this.state.error === undefined;
    return (
      <ValidationInput
        className={this.props.className}
        description={translate('onboarding.create_project.project_key.description')}
        error={this.state.error}
        help={translate('onboarding.create_project.project_key.help')}
        id="project-key"
        isInvalid={isInvalid}
        isValid={isValid}
        label={translate('onboarding.create_project.project_key')}
        required={true}>
        <input
          autoFocus={true}
          className={classNames('input-super-large', {
            'is-invalid': isInvalid,
            'is-valid': isValid
          })}
          id="project-key"
          maxLength={400}
          minLength={1}
          onChange={this.handleChange}
          type="text"
          value={this.props.value}
        />
      </ValidationInput>
    );
  }
}
