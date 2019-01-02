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
import { translate } from '../../../helpers/l10n';

const languages = [
  { name: 'Java', url: 'https://redirect.sonarsource.com/plugins/java.html' },
  { name: 'C/C++', url: 'https://redirect.sonarsource.com/plugins/cpp.html' },
  { name: 'C#', url: 'https://redirect.sonarsource.com/plugins/csharp.html' },
  { name: 'COBOL', url: 'https://redirect.sonarsource.com/plugins/cobol.html' },
  { name: 'ABAP', url: 'https://redirect.sonarsource.com/plugins/abap.html' },
  { name: 'HTML', url: 'https://redirect.sonarsource.com/plugins/web.html' },
  { name: 'RPG', url: 'https://redirect.sonarsource.com/plugins/rpg.html' },
  { name: 'JavaScript', url: 'https://redirect.sonarsource.com/plugins/javascript.html' },
  { name: 'TypeScript', url: 'https://redirect.sonarsource.com/plugins/typescript.html' },
  { name: 'Objective C', url: 'https://redirect.sonarsource.com/plugins/objectivec.html' },
  { name: 'XML', url: 'https://redirect.sonarsource.com/plugins/xml.html' },
  { name: 'VB.NET', url: 'https://redirect.sonarsource.com/plugins/vbnet.html' },
  { name: 'PL/SQL', url: 'https://redirect.sonarsource.com/plugins/plsql.html' },
  { name: 'T-SQL', url: 'https://redirect.sonarsource.com/plugins/tsql.html' },
  { name: 'Flex', url: 'https://redirect.sonarsource.com/plugins/flex.html' },
  { name: 'Python', url: 'https://redirect.sonarsource.com/plugins/python.html' },
  { name: 'Groovy', url: 'https://redirect.sonarsource.com/plugins/groovy.html' },
  { name: 'PHP', url: 'https://redirect.sonarsource.com/plugins/php.html' },
  { name: 'Swift', url: 'https://redirect.sonarsource.com/plugins/swift.html' },
  { name: 'Visual Basic', url: 'https://redirect.sonarsource.com/plugins/vb.html' },
  { name: 'PL/I', url: 'https://redirect.sonarsource.com/plugins/pli.html' }
];

const half = (languages.length + 1) / 2;

export default function AboutLanguages() {
  return (
    <div className="boxed-group">
      <h2>{translate('about_page.languages')}</h2>
      <div className="boxed-group-inner">
        <p className="about-page-text">{translate('about_page.languages.text')}</p>
        <ul className="about-page-languages">
          {languages.slice(0, half).map((language, index) => (
            <li key={index}>
              <a href={language.url}>{language.name}</a>
              <br />
              {index + half < languages.length && (
                <a href={languages[index + half].url}>{languages[index + half].name}</a>
              )}
            </li>
          ))}
        </ul>
      </div>
    </div>
  );
}
