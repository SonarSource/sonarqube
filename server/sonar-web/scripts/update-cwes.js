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

/* eslint-disable no-console */

/**
 * Execute this script by passing the path to the CWE XML definition file.
 *
 * You can download the full CWE database in XML format here: https://cwe.mitre.org/data/downloads.html
 * Make sure to unzip the downloaded file first before passing it to this script.
 *
 * Usage:
 *     node scripts/update-cwes.js PATH
 *   or:
 *     yarn update-cwes PATH
 *
 * Example:
 *     node scripts/update-cwes.js ~/Downloads/cwec_v4.6.xml
 *   or:
 *     yarn update-cwes ~/Downloads/cwec_v4.6.xml
 */

const fs = require('fs');
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
const newCWEs = getCWEs(xmlContent);
writeToStandardsJson(newCWEs);

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

function getCWEs(xml) {
  const document = new jsdom.JSDOM(xml);
  const weaknesses = document.window.document.querySelectorAll('Weaknesses Weakness');
  const cwes = {
    unknown: {
      title: 'No CWE associated',
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
      console.log(chalk.yellow(`No Name attribute found for CWE '${id}'. Skipping.`));
      return;
    }

    const descriptionEl = weakness.querySelector('Description');
    if (descriptionEl) {
      description = trim(descriptionEl.textContent);
    }

    cwes[id] = { title, description };
  });

  return cwes;
}

function writeToStandardsJson(cwes) {
  try {
    fs.accessSync(STANDARDS_JSON_FILE, fs.constants.W_OK);
  } catch (e) {
    console.error(chalk.red(`No write access for standards.json ('${STANDARDS_JSON_FILE}') file`));
    throw e;
  }

  try {
    const json = JSON.parse(fs.readFileSync(STANDARDS_JSON_FILE).toString());
    json.cwe = cwes;
    fs.writeFileSync(STANDARDS_JSON_FILE, JSON.stringify(json, undefined, 2));
  } catch (e) {
    console.error(
      chalk.red(`Failed to write data to standards.json ('${STANDARDS_JSON_FILE}') file`),
    );
    throw e;
  }
}
