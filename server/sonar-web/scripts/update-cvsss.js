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
const fs =require('fs');
const chalk = require('chalk');
const jsdom = require('jsdom');
const { trim } = require('lodash');
const path = require('path');

const STANDARDS_JSON_FILE = path.join(
  __dirname,
  '..',
  'src',
  'main',
  'js',
  'helpers',
  'standards.json',
);

const xmlContent = readXMLContent(process.argv[2]);
const newCVSSs = getCVSSs(xmlContent);
writeToStandardsJson(newCVSSs);

function readXMLContent(xmlPath) {
  if (fs.existsSync(xmlPath)) {
    try {
      fs.accessSync(xmlPath, fs.constants.R_OK);
      return fs.readFileSync(xmlPath).toString();
    } catch (e) {
      console.error(chalk.red(`No read access for XML file '${xmlPath}'`));
      throw e;
    }
  } else {
    console.error(chalk.red(`Cannot find XML file '${xmlPath}'`));
    throw Error('');
  }
}

function getCVSSs(xml) {
  const document = new jsdom.JSDOM(xml);
  const weaknesses = document.window.document.querySelectorAll('Weaknesses Weakness');
  const cvsss = {
    unknown: {
      title: 'No CVSS associated',
    },
  };

  weaknesses.forEach((weakness) => {
    const id = weakness.getAttribute('ID');
    const title = weakness.getAttribute('Name');
    let description = '';

    if (!id) {
      return;
    }

    if (!title) {
      console.log(chalk.yellow(`No Name attribute found for CVSS '${id}'. Skipping.`));
      return;
    }

    const descriptionEl = weakness.querySelector('Description');
    if (descriptionEl) {
      description = trim(descriptionEl.textContent);
    }

    cvsss[id] = { title, description };
  });

  return cvsss;
}

function writeToStandardsJson(cvsss) {
  try {
    fs.accessSync(STANDARDS_JSON_FILE, fs.constants.W_OK);
  } catch (e) {
    console.error(chalk.red(`No write access for standards.json ('${STANDARDS_JSON_FILE}') file`));
    throw e;
  }

  try {
    const json = JSON.parse(fs.readFileSync(STANDARDS_JSON_FILE).toString());
    json.cvss = cvsss;
    fs.writeFileSync(STANDARDS_JSON_FILE, JSON.stringify(json, undefined, 2));
  } catch (e) {
    console.error(
      chalk.red(`Failed to write data to standards.json ('${STANDARDS_JSON_FILE}') file`),
    );
    throw e;
  }
}
