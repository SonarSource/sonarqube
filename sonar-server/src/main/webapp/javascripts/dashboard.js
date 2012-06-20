var Portal = Class.create();
Portal.prototype = {
    initialize: function (options) {
        this.setOptions(options);
        if (!this.options.editorEnabled) return;

        Droppables.add(this.options.blocklist, {
            containment : $A($$("."+this.options.column)),
            hoverclass  : this.options.hoverclass,
            overlap     : 'horizontal',
            onDrop: function(dragged, dropped) {
                $(dragged).remove();
            }
        });

        this.createAllSortables();

        this.lastSaveString = "";

        this.saveDashboardsState();
    },
    /****************************************************/

    createAllSortables: function () {
        var sortables = $$("."+this.options.column);
        $A(sortables).each(function (sortable) {
            Sortable.create(sortable, {
                containment: $A(sortables),
                constraint: false,
                tag: 'div',
                handle: this.options.handleClass,
                only: this.options.block,
                dropOnEmpty: true,
                hoverclass: this.options.hoverclass,
                starteffect: function(widget) {
                    $(widget).addClassName("shadow-block");
                }.bind(this),
                endeffect: function(widget) {
                    $(widget).removeClassName("shadow-block");
                }.bind(this),
                onUpdate: function () {
                    this.saveDashboardsState();
                }.bind(this)
            });
        }.bind(this));
    },

    highlightWidget: function(widgetId) {
      new Effect.Highlight($('block_' + widgetId), {duration: this.options.highlight_duration,
                                          startcolor: this.options.highlight_startcolor,
                                          endcolor: this.options.highlight_endcolor});
    },

    /****************************************************/
    saveDashboardsState: function () {
        var result = "";
        var index = 1;
        $$("."+this.options.column).each(function (sortable) {
            if ($(sortable).select("."+this.options.block).length == 0) {
                $(sortable).select("."+this.options.columnhandle)[0].show();
            } else {
                $(sortable).select("."+this.options.columnhandle)[0].hide();
            }
            if (index > 1) result += ";";
            result += Sortable.sequence($(sortable).identify());
            index++;
        });
        if (result==this.lastSaveString) {
            return;
        }
        var firstTime=this.lastSaveString=="";
        this.lastSaveString=result;

        if (firstTime) return;

        try {
            if ($(this.options.dashboardstate)) {
                $(this.options.dashboardstate).value = result;
            }
            if (this.options.saveurl) {
                var url = this.options.saveurl;
                var postBody = this.options.dashboardstate + '=' +escape(result);

                new Ajax.Request(url,
                {
                    evalscripts:false,
                    method: 'post',
                    postBody: postBody
                });
            }
        } catch(e) {
        }
    },

    setOptions: function (options) {
        this.options = {};
        Object.extend(this.options, options || {});
    },

    editWidget: function(id) {
      $('widget_title_' + id) && $('widget_title_' + id).hide();
      $('widget_' + id).hide();
      $('widget_props_' + id).show();
    },
    cancelEditWidget: function(id) {
      $('widget_title_' + id) && $('widget_title_' + id).show();
      $('widget_' + id).show();
      $('widget_props_' + id).hide();
    },
    deleteWidget: function(elt) {
      $(elt).up('.' + this.options.block).remove();
      this.saveDashboardsState();
    }
};

autoResize = function(everyMs, callback) {
  var resizeTimer = null;
  Event.observe(window, 'resize', function() {
    if (resizeTimer == null) {
      resizeTimer = window.setTimeout(function() {
        resizeTimer = null;
        callback();
      }, everyMs);
    }
  });
};
