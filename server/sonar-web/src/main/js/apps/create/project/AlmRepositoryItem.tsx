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
import { identity } from 'lodash';
import { FormattedMessage } from 'react-intl';
import { Link } from 'react-router';
import * as theme from '../../../app/theme';
import Checkbox from '../../../components/controls/Checkbox';
import CheckIcon from '../../../components/icons-components/CheckIcon';
import Tooltip from '../../../components/controls/Tooltip';
import { getBaseUrl, getProjectUrl } from '../../../helpers/urls';
import { translate } from '../../../helpers/l10n';
import LockIcon from '../../../components/icons-components/LockIcon';

interface Props {
  disabled: boolean;
  highlightUpgradeBox: (highlight: boolean) => void;
  identityProvider: T.IdentityProvider;
  repository: T.AlmRepository;
  selected: boolean;
  toggleRepository: (repository: T.AlmRepository) => void;
}

export default class AlmRepositoryItem extends React.PureComponent<Props> {
  handleMouseEnter = () => {
    this.props.highlightUpgradeBox(true);
  };

  handleMouseLeave = () => {
    this.props.highlightUpgradeBox(false);
  };

  handleToggle = () => {
    if (!this.props.disabled && !this.props.repository.linkedProjectKey) {
      this.props.toggleRepository(this.props.repository);
    }
  };

  render() {
    const { disabled, identityProvider, repository, selected } = this.props;
    const alreadyImported = Boolean(repository.linkedProjectKey);
    return (
      <Tooltip
        overlay={
          disabled
            ? translate('onboarding.create_project.subscribe_to_import_private_repositories')
            : undefined
        }>
        <li>
          <div
            className={classNames('create-project-repository', {
              disabled,
              imported: alreadyImported,
              selected
            })}
            onClick={this.handleToggle}
            onMouseEnter={disabled ? this.handleMouseEnter : undefined}
            onMouseLeave={disabled ? this.handleMouseLeave : undefined}
            role="listitem">
            <div className="flex-1 display-flex-center">
              {disabled ? (
                <LockIcon fill={theme.disableGrayText} />
              ) : (
                <Checkbox
                  checked={selected || alreadyImported}
                  disabled={disabled || alreadyImported}
                  onCheck={identity}
                />
              )}
              <img
                alt={identityProvider.name}
                className={classNames('spacer-left', { 'icon-half-transparent': disabled })}
                height={14}
                src={`${getBaseUrl()}/images/sonarcloud/${identityProvider.key}.svg`}
                width={14}
              />
              <span className="spacer-left">{this.props.repository.label}</span>
              {repository.private && (
                <div className="outline-badge spacer-left">{translate('visibility.private')}</div>
              )}
            </div>

            {repository.linkedProjectKey && (
              <span>
                <CheckIcon className="little-spacer-right" fill={theme.green} />
                <FormattedMessage
                  defaultMessage={translate('onboarding.create_project.repository_imported')}
                  id="onboarding.create_project.repository_imported"
                  values={{
                    link: (
                      <Link to={getProjectUrl(repository.linkedProjectKey)}>
                        {translate('onboarding.create_project.see_project')}
                      </Link>
                    )
                  }}
                />
              </span>
            )}
          </div>
        </li>
      </Tooltip>
    );
  }
}
