(function ($) {

  var options = {
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
          clearInterval(this.get('timer'));
          this.set({
            state: 'failed',
            message: message || t('process.fail')
          });
          this.set('state', 'failed');
        }
      }),

      Processes = Backbone.Collection.extend({
        model: Process
      }),

      ProcessView = Marionette.ItemView.extend({
        tagName: 'li',
        className: 'process-spinner',

        modelEvents: {
          'change': 'render'
        },

        render: function () {
          var that = this;
          switch (this.model.get('state')) {
            case 'timeout':
              this.$el.html(this.model.get('message')).addClass('shown');
              break;
            case 'failed':
              this.$el.html(this.model.get('message')).addClass('process-spinner-failed shown');
              var close = $('<button></button>').html('<i class="icon-close"></i>').addClass('process-spinner-close');
              close.appendTo(this.$el);
              close.on('click', function () {
                var a = { force: true };
                that.model.finish(a);
              });
              break;
            case 'finished':
              this.$el.addClass('hidden');
              break;
          }
          return this;
        }
      }),

      ProcessesView = Marionette.CollectionView.extend({
        tagName: 'ul',
        className: 'processes-container',
        itemView: ProcessView
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
          }, options.timeout)
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

    processesView.render().$el.appendTo(document.body);

  });

})(window.jQuery);
