/* global _:false, $j:false, Backbone:false, baseUrl:false */

window.SS = typeof window.SS === 'object' ? window.SS : {};

(function() {

  var DetailsSelectFilterView = window.SS.DetailsFilterView.extend({
    template: '#selectFilterTemplate',
    itemTemplate: '#selectFilterItemTemplate',


    events: function() {
      return {
        'change .choices input[type=checkbox]': 'addToSelection',
        'change .selection input[type=checkbox]': 'removeFromSelection'
      };
    },


    render: function() {
      window.SS.DetailsFilterView.prototype.render.apply(this, arguments);
      this.updateLists();
    },


    renderList: function(collection, selector, checked) {
      var that = this,
          container = this.$(selector),
          t = _.template($j(this.itemTemplate).html());

      container.empty().toggleClass('hidden', collection.length === 0);
      collection.each(function(item) {
        container.append(t(_.extend(
            {
              item: item.toJSON(),
              checked: checked
            }, that.model.toJSON())));
      });
    },


    updateLists: function() {
      this.renderList(this.options.filterView.selection, '.selection', true);
      this.renderList(this.options.filterView.choices, '.choices', false);

      var current = this.currentChoice || 0;
      this.updateCurrent(current);
    },


    addToSelection: function(e) {
      var id = $j(e.target).val(),
          model = this.options.filterView.choices.findWhere({ id: id });

      if (this.model.get('multiple')) {
        this.options.filterView.selection.add(model);
        this.options.filterView.choices.remove(model);
      } else {
        this.options.filterView.choices.add(this.options.filterView.selection.models);
        this.options.filterView.choices.remove(model);
        this.options.filterView.selection.reset([model]);
      }

      this.updateValue();
      this.updateLists();
    },


    removeFromSelection: function(e) {
      var id = $j(e.target).val(),
          model = this.options.filterView.selection.findWhere({ id: id });

      this.options.filterView.choices.add(model);
      this.options.filterView.selection.remove(model);

      this.updateValue();
      this.updateLists();
    },


    updateValue: function() {
      this.model.set('value', this.options.filterView.selection.map(function(m) {
        return m.get('id');
      }));
    },


    updateCurrent: function(index) {
      this.currentChoice = index;
      this.$('label').removeClass('current')
          .eq(this.currentChoice).addClass('current');
    },


    onShow: function() {
      this.bindedOnKeyDown = _.bind(this.onKeyDown, this);
      $j('body').on('keydown', this.bindedOnKeyDown);
    },


    onHide: function() {
      $j('body').off('keydown', this.bindedOnKeyDown);
    },


    onKeyDown: function(e) {
      switch (e.keyCode) {
        case 38:
          e.preventDefault();
          this.selectPrevChoice();
          break;
        case 40:
          e.preventDefault();
          this.selectNextChoice();
          break;
        case 13:
          e.preventDefault();
          this.selectCurrent();
          break;
      }
    },


    selectNextChoice: function() {
      if (this.$('label').length > this.currentChoice + 1) {
        this.updateCurrent(this.currentChoice + 1);
        this.scrollNext();
      }
    },


    scrollNext: function() {
      var currentLabel = this.$('label').eq(this.currentChoice);
      if (currentLabel.length > 0) {
        var list = currentLabel.closest('ul'),
            labelPos = currentLabel.offset().top - list.offset().top + list.scrollTop(),
            deltaScroll = labelPos - list.height() + currentLabel.outerHeight();

        if (deltaScroll > 0) {
          list.scrollTop(deltaScroll);
        }
      }
    },


    selectPrevChoice: function() {
      if (this.currentChoice > 0) {
        this.updateCurrent(this.currentChoice - 1);
        this.scrollPrev();
      }
    },


    scrollPrev: function() {
      var currentLabel = this.$('label').eq(this.currentChoice);
      if (currentLabel.length > 0) {
        var list = currentLabel.closest('ul'),
            labelPos = currentLabel.offset().top - list.offset().top;

        if (labelPos < 0) {
          list.scrollTop(list.scrollTop() + labelPos);
        }
      }
    },


    selectCurrent: function() {
      this.$('label').eq(this.currentChoice).click();
    },


    serializeData: function() {
      return _.extend({}, this.model.toJSON(), {
        selection: this.options.filterView.selection.toJSON(),
        choices: this.options.filterView.choices.toJSON()
      });
    }
  });



  var SelectFilterView = window.SS.BaseFilterView.extend({
    className: 'navigator-filter',


    initialize: function() {
      window.SS.BaseFilterView.prototype.initialize.call(this, {
        detailsView: DetailsSelectFilterView
      });


      this.selection = new Backbone.Collection([], { comparator: 'index' });

      var index = 0,
          icons = this.model.get('choiceIcons');

      this.choices = new Backbone.Collection(
          _.map(this.model.get('choices'), function(value, key) {
            var model = new Backbone.Model({
              id: key,
              text: value,
              index: index++
            });

            if (icons && icons[key]) {
              model.set('icon', icons[key]);
            }

            return model;
          }), { comparator: 'index' }
      );
    },


    renderInput: function() {
      var input = $j('<select>')
          .prop('name', this.model.get('property'))
          .prop('multiple', true)
          .css('display', 'none');
      this.selection.each(function(item) {
        var option = $j('<option>')
            .prop('value', item.get('id'))
            .prop('selected', true)
            .text(item.get('text'));
        option.appendTo(input);
      });
      this.choices.each(function(item) {
        var option = $j('<option>')
            .prop('value', item.get('id'))
            .text(item.get('text'));
        option.appendTo(input);
      });
      input.appendTo(this.$el);
    },


    renderValue: function() {
      var value = this.selection.map(function(item) {
            return item.get('text');
          }),
          defaultValue = this.model.has('defaultValue') ?
              this.model.get('defaultValue') :
              this.model.get('multiple') ? window.SS.phrases.all : window.SS.phrases.any;

      return this.isDefaultValue() ? defaultValue : value.join(', ');
    },


    isDefaultValue: function() {
      return this.selection.length === 0 || this.choices.length === 0;
    },


    disable: function() {
      this.choices.add(this.selection.models);
      this.selection.reset([]);
      window.SS.BaseFilterView.prototype.disable.apply(this, arguments);
    },


    restore: function(value) {
      if (_.isString(value)) {
        value = value.split(',');
      }

      if (this.choices && this.selection && value.length > 0) {
        var that = this;
        this.choices.add(this.selection.models);
        this.selection.reset([]);

        _.each(value, function(v) {
          var cModel = that.choices.findWhere({ id: v });

          if (cModel) {
            that.selection.add(cModel);
            that.choices.remove(cModel);
          }
        });

        this.detailsView.updateLists();

        this.model.set({
          value: value,
          enabled: true
        }, {
          silent: true
        });

        this.renderBase();
      } else {
        this.clear();
      }
    },


    clear: function() {
      var that = this;
      if (this.selection && this.choices) {
        this.selection.each(function(m) {
          that.choices.add(m);
        });
        this.selection.reset([]);
      }
      this.model.unset('value');
      this.detailsView.render();
      if (this.detailsView.updateCurrent) {
        this.detailsView.updateCurrent(0);
      }
    },


    formatValue: function() {
      var q = {};
      if (this.model.has('property') && this.model.has('value') && this.model.get('value').length > 0) {
        q[this.model.get('property')] = this.model.get('value').join(',');
      }
      return q;
    }

  });



  /*
   * Export public classes
   */

  _.extend(window.SS, {
    DetailsSelectFilterView: DetailsSelectFilterView,
    SelectFilterView: SelectFilterView
  });

})();
