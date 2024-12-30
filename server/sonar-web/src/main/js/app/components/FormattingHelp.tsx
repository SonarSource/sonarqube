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

import { Helmet } from 'react-helmet-async';
import {
  CellComponent,
  ContentCell,
  HtmlFormatter,
  PageContentFontWrapper,
  Table,
  TableRow,
  Title,
} from '~design-system';
import { translate } from '../../helpers/l10n';

const COLUMNS = ['50%', '50%'];

export default function FormattingHelp() {
  return (
    <PageContentFontWrapper className="sw-typo-lg sw-p-6 sw-h-screen">
      <Helmet defer={false} title={translate('formatting.page')} />
      <Title>Formatting Syntax</Title>
      <Table
        columnCount={COLUMNS.length}
        columnWidths={COLUMNS}
        header={
          <TableRow>
            <ContentCell>Write:</ContentCell>
            <ContentCell>To display:</ContentCell>
          </TableRow>
        }
      >
        <TableRow>
          <ContentCell>*this text is bold*</ContentCell>
          <ContentCell>
            <HtmlFormatter>
              <strong>this text is bold</strong>
            </HtmlFormatter>
          </ContentCell>
        </TableRow>
        <TableRow>
          <ContentCell>https://knowledgebase.autorabit.com/codescan/en</ContentCell>
          <ContentCell>
            <HtmlFormatter>
              <a href="https://knowledgebase.autorabit.com/codescan/en">
                https://knowledgebase.autorabit.com/codescan/en
              </a>
            </HtmlFormatter>
          </ContentCell>
        </TableRow>
        <TableRow>
          <ContentCell>
            [SonarQube™ Home Page](https://knowledgebase.autorabit.com/codescan/en)
          </ContentCell>
          <ContentCell>
            <HtmlFormatter>
              <a href="https://knowledgebase.autorabit.com/codescan/en">SonarQube™ Home Page</a>
            </HtmlFormatter>
          </ContentCell>
        </TableRow>
        <TableRow>
          <ContentCell>
            * first item
            <br />* second item
          </ContentCell>
          <ContentCell>
            <HtmlFormatter>
              <ul>
                <li>first item</li>
                <li>second item</li>
              </ul>
            </HtmlFormatter>
          </ContentCell>
        </TableRow>
        <TableRow>
          <CellComponent>
            1. first item
            <br />
            1. second item
          </CellComponent>
          <ContentCell>
            <HtmlFormatter>
              <ol>
                <li>first item</li>
                <li>second item</li>
              </ol>
            </HtmlFormatter>
          </ContentCell>
        </TableRow>
        <TableRow>
          <ContentCell>
            = Heading Level 1<br />
            == Heading Level 2<br />
            === Heading Level 3<br />
            ==== Heading Level 4<br />
            ===== Heading Level 5<br />
            ====== Heading Level 6<br />
          </ContentCell>
          <ContentCell>
            <HtmlFormatter>
              <h1>Heading Level 1</h1>
              <h2>Heading Level 2</h2>
              <h3>Heading Level 3</h3>
              <h4>Heading Level 4</h4>
              <h5>Heading Level 5</h5>
              <h6>Heading Level 6</h6>
            </HtmlFormatter>
          </ContentCell>
        </TableRow>
        <TableRow>
          <ContentCell>``Lists#newArrayList()``</ContentCell>
          <ContentCell>
            <HtmlFormatter>
              <code>Lists#newArrayList()</code>
            </HtmlFormatter>
          </ContentCell>
        </TableRow>
        <TableRow>
          <ContentCell>
            ``
            <br />
            {'// code on multiple lines'}
            <br />
            {'public void foo() {'}
            <br />
            &nbsp;&nbsp;
            {'// do some logic here'}
            <br />
            {'}'}
            <br />
            ``
          </ContentCell>
          <ContentCell>
            <HtmlFormatter>
              <pre>
                {'// code on multiple lines\npublic void foo() {\n  // do some logic here\n}'}
              </pre>
            </HtmlFormatter>
          </ContentCell>
        </TableRow>
        <TableRow>
          <ContentCell>
            Standard text
            <br />
            &gt; Blockquoted text
            <br />
            &gt; that spans multiple lines
            <br />
          </ContentCell>
          <ContentCell>
            <HtmlFormatter>
              <p>Standard text</p>
              <blockquote>
                Blockquoted text
                <br />
                that spans multiple lines
                <br />
              </blockquote>
            </HtmlFormatter>
          </ContentCell>
        </TableRow>
      </Table>
    </PageContentFontWrapper>
  );
}
