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
import * as classNames from 'classnames';
import { debounce } from 'lodash';
import ValidationInput from '../../../components/controls/ValidationInput';
import { doesComponentExists } from '../../../api/components';
import { translate } from '../../../helpers/l10n';

interface Props {
  className?: string;
  initialValue?: string;
  onChange: (value: string | undefined) => void;
}

interface State {
  error?: string;
  touched: boolean;
  validating: boolean;
  value: string;
}

export default class ProjectKeyInput extends React.PureComponent<Props, State> {
  mounted = false;
  constructor(props: Props) {
    super(props);
    this.state = { error: undefined, touched: false, validating: false, value: '' };
    this.checkFreeKey = debounce(this.checkFreeKey, 250);
  }

  componentDidMount() {
    this.mounted = true;
    if (this.props.initialValue !== undefined) {
      this.setState({ value: this.props.initialValue });
      this.validateKey(this.props.initialValue);
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  checkFreeKey = (key: string) => {
    this.setState({ validating: true });
    return doesComponentExists({ component: key })
      .then(alreadyExist => {
        if (this.mounted) {
          if (!alreadyExist) {
            this.setState({ error: undefined, validating: false });
            this.props.onChange(key);
          } else {
            this.setState({
              error: translate('onboarding.create_project.project_key.taken'),
              touched: true,
              validating: false
            });
            this.props.onChange(undefined);
          }
        }
      })
      .catch(() => {
        if (this.mounted) {
          this.setState({ error: undefined, validating: false });
          this.props.onChange(key);
        }
      });
  };

  handleChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const { value } = event.currentTarget;
    this.setState({ touched: true, value });
    this.validateKey(value);
  };

  validateKey(key: string) {
    if (key.length > 400 || !/^[\w-.:]*[a-zA-Z]+[\w-.:]*$/.test(key)) {
      this.setState({
        error: translate('onboarding.create_project.project_key.error'),
        touched: true
      });
      this.props.onChange(undefined);
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
          value={this.state.value}
        />
      </ValidationInput>
    );
  }
}
