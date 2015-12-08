import $ from 'jquery';
import _ from 'underscore';
import Backbone from 'backbone';
import Marionette from 'backbone.marionette';

var defaults = {
  queue: {},
  timeout: 300,
  fadeTimeout: 100
};

var Process = Backbone.Model.extend({
      defaults: {
        state: 'ok'
      },

      timeout: function () {
        this.set({
          state: 'timeout',
          message: 'Still Working...'
        });
      },

      finish: function (options) {
        options = _.defaults(options || {}, { force: false });
        if (this.get('state') !== 'failed' || !!options.force) {
          this.trigger('destroy', this, this.collection, options);
        }
      },

      fail: function (message) {
        var that = this,
            msg = message || window.t('process.fail');
        if (msg === 'process.fail') {
          // no translation
          msg = 'An error happened, some parts of the page might not render correctly. ' +
              'Please contact the administrator if you keep on experiencing this error.';
        }
        clearInterval(this.get('timer'));
        this.set({
          state: 'failed',
          message: msg
        });
        this.set('state', 'failed');
        setTimeout(function () {
          that.finish({ force: true });
        }, 5000);
      }
    }),

    Processes = Backbone.Collection.extend({
      model: Process
    }),

    ProcessesView = Marionette.ItemView.extend({
      tagName: 'ul',
      className: 'processes-container',

      collectionEvents: {
        'all': 'render'
      },

      render: function () {
        var failed = this.collection.findWhere({ state: 'failed' }),
            timeout = this.collection.findWhere({ state: 'timeout' }),
            el;
        this.$el.empty();
        if (failed != null) {
          el = $('<li></li>')
              .html(failed.get('message'))
              .addClass('process-spinner process-spinner-failed shown');
          var close = $('<button></button>').html('<i class="icon-close"></i>').addClass('process-spinner-close');
          close.appendTo(el);
          close.on('click', function () {
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


var processes = new Processes(),
    processesView = new ProcessesView({
      collection: processes
    });

/**
 * Add background process
 * @returns {number}
 */
function addBackgroundProcess () {
  var uid = _.uniqueId('process'),
      process = new Process({
        id: uid,
        timer: setTimeout(function () {
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
function finishBackgroundProcess (uid) {
  var process = processes.get(uid);
  if (process != null) {
    process.finish();
  }
}

/**
 * Fail background process
 * @param {number} uid
 * @param {string} message
 */
function failBackgroundProcess (uid, message) {
  var process = processes.get(uid);
  if (process != null) {
    process.fail(message);
  }
}

/**
 * Handle ajax error
 * @param jqXHR
 */
function handleAjaxError (jqXHR) {
  if (jqXHR.processId != null) {
    var message = null;
    if (jqXHR != null && jqXHR.responseJSON != null && jqXHR.responseJSON.errors != null) {
      message = _.pluck(jqXHR.responseJSON.errors, 'msg').join('. ');
    }
    failBackgroundProcess(jqXHR.processId, message);
  }
}


$.ajaxSetup({
  beforeSend: function (jqXHR) {
    jqXHR.processId = addBackgroundProcess();
  },
  complete: function (jqXHR) {
    if (jqXHR.processId != null) {
      finishBackgroundProcess(jqXHR.processId);
    }
  },
  statusCode: {
    400: handleAjaxError,
    401: handleAjaxError,
    403: handleAjaxError,
    500: handleAjaxError
  }
});


$(function () {
  processesView.render().$el.insertBefore('#footer');
});
