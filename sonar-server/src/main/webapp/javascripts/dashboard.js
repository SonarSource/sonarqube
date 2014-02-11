(function($) {

  window.Portal = function(options) {
    this.initialize(options);
  };

  window.Portal.prototype = {

    initialize: function(options) {
      this.options = options;
      if (!this.options.editorEnabled) {
        return;
      }
      this.createAllSortables();
      this.lastSaveString = '';
      this.saveDashboardsState();
    },


    createAllSortables: function() {
      var that = this,
          blocks = $('.' + this.options.block),
          columnHandle = $('.' + this.options.columnHandle),
          draggable,

          onDragLeave = function(e) {
            $(e.currentTarget).removeClass(that.options.hoverClass);
          },

          onDrop = function(e) {
            e.preventDefault();
            draggable.detach().insertBefore($(e.currentTarget));
            onDragLeave(e);
            that.saveDashboardsState();
          };

      blocks
          .prop('draggable', true)
          .on('dragstart', function(e) {
            e.originalEvent.dataTransfer.setData('text/plain', 'drag');
            draggable = $(this);
            columnHandle.show();
          })
          .on('dragover', function(e) {
            if (draggable.prop('id') !== $(this).prop('id')) {
              e.preventDefault();
              $(e.currentTarget).addClass(that.options.hoverClass);
            }
          })
          .on('drop', onDrop)
          .on('dragleave', onDragLeave);

      columnHandle
          .on('dragover', function(e) {
            e.preventDefault();
            $(e.currentTarget).addClass(that.options.hoverClass);
          })
          .on('drop', onDrop)
          .on('dragleave', onDragLeave);
    },


    highlightWidget: function(widgetId) {
      var block = $('#block_' + widgetId),
          options = this.options;
      block.css('background-color', options.highlightStartColor);
      setTimeout(function() {
        block.css('background-color', options.highlightEndColor);
      }, this.options.highlightDuration);
    },


    saveDashboardsState: function() {
      var options = this.options,
          result = $('.' + this.options.column).map(function () {
            var blocks = $(this).find('.' + options.block);
            $(this).find('.' + options.columnHandle).toggle(blocks.length === 0);

            return blocks.map(function () {
              return $(this).prop('id').substring(options.block.length + 1);
            }).get().join(',');
          }).get().join(';');

      if (result === this.lastSaveString) {
        return;
      }

      var firstTime = this.lastSaveString === '';
      this.lastSaveString = result;

      if (firstTime) {
        return;
      }

      if (this.options.saveUrl) {
        var postBody = this.options.dashboardState + '=' + escape(result);

        $.ajax({
          url: this.options.saveUrl,
          type: 'POST',
          data: postBody
        });
      }
    },


    editWidget: function(widgetId) {
      $('#widget_title_' + widgetId).hide();
      $('#widget_' + widgetId).hide();
      $('#widget_props_' + widgetId).show();
    },


    cancelEditWidget: function(widgetId) {
      $('widget_title_' + widgetId).show();
      $('#widget_' + widgetId).show();
      $('#widget_props_' + widgetId).hide();
    },


    deleteWidget: function(element) {
      $(element).closest('.' + this.options.block).remove();
      this.saveDashboardsState();
    }
  };



  window.autoResize = function(everyMs, callback) {
    var debounce = _.debounce(callback, everyMs);
    $(window).on('resize', debounce);
  };

})(jQuery);
