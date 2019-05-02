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

// TODO Test this function, but I don't get the logic behind it
export function filterDuplicationBlocksByLine(blocks: T.DuplicationBlock[], line: number) {
  /* eslint-disable no-underscore-dangle */
  let foundOne = false;
  return blocks.filter(b => {
    const outOfBounds = b.from > line || b.from + b.size < line;
    const currentFile = b._ref === '1';
    const shouldDisplayForCurrentFile = outOfBounds || foundOne;
    const shouldDisplay = !currentFile || shouldDisplayForCurrentFile;
    const isOk = b._ref !== undefined && shouldDisplay;
    if (b._ref === '1' && !outOfBounds) {
      foundOne = true;
    }
    return isOk;
  });
  /* eslint-enable no-underscore-dangle */
}

export function getDuplicationBlocksForIndex(
  duplications: T.Duplication[] | undefined,
  index: number
) {
  return (duplications && duplications[index] && duplications[index].blocks) || [];
}

export function isDuplicationBlockInRemovedComponent(blocks: T.DuplicationBlock[]) {
  return blocks.some(b => b._ref === undefined); // eslint-disable-line no-underscore-dangle
}
