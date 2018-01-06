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
import Backbone from 'backbone';
import Marionette from 'backbone.marionette';
import escapeHtml from 'escape-html';
import { uniqueId } from 'lodash';
import { translate } from '../../helpers/l10n';
import { getCSRFTokenName, getCSRFTokenValue } from '../../helpers/request';

const defaults = {
  queue: {},
  timeout: 300,
  fadeTimeout: 100
};

const Process = Backbone.Model.extend({
  defaults: {
    state: 'ok'
  },

  timeout() {
    this.set({
      state: 'timeout',
      message: 'Still Working...'
    });
  },

  finish(options) {
    const finalOptions = { force: false, ...options };
    if (this.get('state') !== 'failed' || finalOptions.force) {
      this.trigger('destroy', this, this.collection, finalOptions);
    }
  },

  fail(message) {
    const that = this;
    let msg = message || translate('process.fail');
    if (msg === 'process.fail') {
      // no translation
      msg =
        'An error happened, some parts of the page might not render correctly. ' +
        'Please contact the administrator if you keep on experiencing this error.';
    }
    clearInterval(this.get('timer'));
    this.set({
      state: 'failed',
      message: msg
    });
    this.set('state', 'failed');
    setTimeout(() => {
      that.finish({ force: true });
    }, 5000);
  }
});

const Processes = Backbone.Collection.extend({
  model: Process
});

const ProcessesView = Marionette.ItemView.extend({
  tagName: 'ul',
  className: 'processes-container',

  collectionEvents: {
    all: 'render'
  },

  render() {
    const failed = this.collection.findWhere({ state: 'failed' });
    const timeout = this.collection.findWhere({ state: 'timeout' });
    let el;
    this.$el.empty();
    if (failed != null) {
      el = $('<li></li>')
        .html(failed.get('message'))
        .addClass('process-spinner process-spinner-failed shown');
      const close = $('<button></button>')
        .html('<i class="icon-close"></i>')
        .addClass('process-spinner-close');
      close.appendTo(el);
      close.on('click', () => {
        failed.finish({ force: true });
      });
      el.appendTo(this.$el);
    } else if (timeout != null) {
      el = $('<li></li>')
        .html(timeout.get('message'))
        .addClass('process-spinner shown');
      el.appendTo(this.$el);
    }
    return this;
  }
});

const processes = new Processes();
const processesView = new ProcessesView({
  collection: processes
});

/**
 * Add background process
 * @returns {number}
 */
function addBackgroundProcess() {
  const uid = uniqueId('process');
  const process = new Process({
    id: uid,
    timer: setTimeout(() => {
      process.timeout();
    }, defaults.timeout)
  });
  processes.add(process);
  return uid;
}

/**
 * Finish background process
 * @param {number} uid
 */
function finishBackgroundProcess(uid) {
  const process = processes.get(uid);
  if (process != null) {
    process.finish();
  }
}

/**
 * Fail background process
 * @param {number} uid
 * @param {string} message
 */
function failBackgroundProcess(uid, message) {
  const process = processes.get(uid);
  if (process != null) {
    process.fail(message);
  }
}

/**
 * Handle ajax error
 * @param jqXHR
 */
function handleAjaxError(jqXHR) {
  if (jqXHR != null && jqXHR.processId != null) {
    let message = null;
    if (jqXHR.responseJSON != null && jqXHR.responseJSON.errors != null) {
      message = jqXHR.responseJSON.errors.map(e => e.msg).join('. ');
    }
    failBackgroundProcess(jqXHR.processId, message ? escapeHtml(message) : null);
  }
}

function handleNotAuthenticatedError() {
  // workaround cyclic dependencies
  const handleRequiredAuthentication = require('./handleRequiredAuthentication').default;
  handleRequiredAuthentication();
}

$.ajaxSetup({
  beforeSend(jqXHR) {
    jqXHR.setRequestHeader(getCSRFTokenName(), getCSRFTokenValue());
    jqXHR.processId = addBackgroundProcess();
  },
  complete(jqXHR) {
    if (jqXHR.processId != null) {
      finishBackgroundProcess(jqXHR.processId);
    }
  },
  statusCode: {
    400: handleAjaxError,
    401: handleNotAuthenticatedError,
    403: handleAjaxError,
    500: handleAjaxError,
    502: handleAjaxError,
    503: handleAjaxError,
    504: handleAjaxError
  }
});

const startAjaxMonitoring = () => {
  processesView.render().$el.appendTo('body');
};

export default startAjaxMonitoring;
