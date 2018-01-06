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
import $ from 'jquery';

const DEFAULTS = {
  title: 'Confirmation',
  html: '',
  yesLabel: 'Yes',
  noLabel: 'Cancel',
  yesHandler() {
    // no op
  },
  noHandler() {
    // no op
  },
  always() {
    // no op
  }
};

export default function(options) {
  const settings = { ...DEFAULTS, ...options };
  const dialog = $(
    '<div><div class="modal-head"><h2>' +
      settings.title +
      '</h2></div><div class="modal-body">' +
      settings.html +
      '</div><div class="modal-foot"><button data-confirm="yes">' +
      settings.yesLabel +
      '</button> <a data-confirm="no" class="action">' +
      settings.noLabel +
      '</a></div></div>'
  );

  $('[data-confirm=yes]', dialog).on('click', () => {
    dialog.dialog('close');
    settings.yesHandler();
    return settings.always();
  });

  $('[data-confirm=no]', dialog).on('click', () => {
    dialog.dialog('close');
    settings.noHandler();
    return settings.always();
  });

  return dialog.dialog({
    modal: true,
    minHeight: null,
    width: 540
  });
}
