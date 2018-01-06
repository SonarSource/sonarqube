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
import React from 'react';
import PropTypes from 'prop-types';
import Defaults from './Defaults';

export default class TemplateDetails extends React.PureComponent {
  static propTypes = {
    organization: PropTypes.object,
    template: PropTypes.object.isRequired
  };

  render() {
    const { template } = this.props;

    return (
      <div className="big-spacer-bottom">
        {template.defaultFor.length > 0 && (
          <div className="spacer-top js-defaults">
            <Defaults permissionTemplate={template} organization={this.props.organization} />
          </div>
        )}

        {!!template.description && (
          <div className="spacer-top js-description">{template.description}</div>
        )}

        {!!template.projectKeyPattern && (
          <div className="spacer-top js-project-key-pattern">
            Project Key Pattern: <code>{template.projectKeyPattern}</code>
          </div>
        )}
      </div>
    );
  }
}
