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

import {
  CellComponent,
  Note,
  SafeHTMLInjection,
  SanitizeLevel,
  SubHeadingHighlight,
  Table,
  TableRow,
} from '~design-system';
import { translate } from '../../../helpers/l10n';
import { RuleParameter } from '../../../types/types';

interface Props {
  params: RuleParameter[];
}

export default function RuleDetailsParameters({ params }: Props) {
  return (
    <div className="js-rule-parameters">
      <SubHeadingHighlight as="h3">{translate('coding_rules.parameters')}</SubHeadingHighlight>
      <Table className="sw-my-4" columnCount={2} columnWidths={[0, 'auto']}>
        {params.map((param) => (
          <TableRow key={param.key}>
            <CellComponent className="sw-align-top sw-font-semibold">{param.key}</CellComponent>
            <CellComponent>
              <div className="sw-flex sw-flex-col sw-gap-2">
                {param.htmlDesc !== undefined && (
                  <SafeHTMLInjection
                    htmlAsString={param.htmlDesc}
                    sanitizeLevel={SanitizeLevel.FORBID_SVG_MATHML}
                  >
                    <div />
                  </SafeHTMLInjection>
                )}
                {param.defaultValue !== undefined && (
                  <Note as="div">
                    {translate('coding_rules.parameters.default_value')}
                    <br />
                    <span className="coding-rules-detail-parameter-value">
                      {param.defaultValue}
                    </span>
                  </Note>
                )}
              </div>
            </CellComponent>
          </TableRow>
        ))}
      </Table>
    </div>
  );
}
