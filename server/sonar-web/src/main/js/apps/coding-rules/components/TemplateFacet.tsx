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
import { translate } from 'sonar-ui-common/helpers/l10n';
import DocTooltip from '../../../components/docs/DocTooltip';
import Facet, { BasicProps } from './Facet';

interface Props extends T.Omit<BasicProps, 'onChange' | 'values'> {
  onChange: (changes: { template: boolean | undefined }) => void;
  value: boolean | undefined;
}

export default class TemplateFacet extends React.PureComponent<Props> {
  handleChange = (changes: { template: string | any[] }) => {
    const template =
      // empty array is returned when a user cleared the facet
      // otherwise `"true"`, `"false"` or `undefined` can be returned
      Array.isArray(changes.template) || changes.template === undefined
        ? undefined
        : changes.template === 'true';
    this.props.onChange({ ...changes, template });
  };

  renderName = (template: string) =>
    template === 'true'
      ? translate('coding_rules.filters.template.is_template')
      : translate('coding_rules.filters.template.is_not_template');

  render() {
    const { onChange, value, ...props } = this.props;

    return (
      <Facet
        {...props}
        onChange={this.handleChange}
        options={['true', 'false']}
        property="template"
        renderName={this.renderName}
        renderTextName={this.renderName}
        singleSelection={true}
        values={value !== undefined ? [String(value)] : []}>
        <DocTooltip
          className="spacer-left"
          doc={import(/* webpackMode: "eager" */ 'Docs/tooltips/rules/rule-templates.md')}
        />
      </Facet>
    );
  }
}
