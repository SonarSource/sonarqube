import $ from 'jquery';
import _ from 'underscore';
import Backbone from 'backbone';
import BaseFilters from './base-filters';
import Template from '../templates/choice-filter.hbs';
import ItemTemplate from '../templates/choice-filter-item.hbs';

var DetailsChoiceFilterView = BaseFilters.DetailsFilterView.extend({
  template: Template,
  itemTemplate: ItemTemplate,


  events: function () {
    return {
      'click label': 'onCheck'
    };
  },


  render: function () {
    BaseFilters.DetailsFilterView.prototype.render.apply(this, arguments);
    this.updateLists();
  },


  renderList: function (collection, selector) {
    var that = this,
        container = this.$(selector);

    container.empty().toggleClass('hidden', collection.length === 0);
    collection.each(function (item) {
      container.append(
          that.itemTemplate(_.extend(item.toJSON(), {
            multiple: that.model.get('multiple') && item.get('id')[0] !== '!'
          }))
      );
    });
  },


  updateLists: function () {
    var choices = new Backbone.Collection(this.options.filterView.choices.reject(function (item) {
          return item.get('id')[0] === '!';
        })),
        opposite = new Backbone.Collection(this.options.filterView.choices.filter(function (item) {
          return item.get('id')[0] === '!';
        }));

    this.renderList(choices, '.choices');
    this.renderList(opposite, '.opposite');

    var current = this.currentChoice || 0;
    this.updateCurrent(current);
  },


  onCheck: function (e) {
    var checkbox = $(e.currentTarget),
        id = checkbox.data('id'),
        checked = checkbox.find('.icon-checkbox-checked').length > 0;

    if (this.model.get('multiple')) {
      if (checkbox.closest('.opposite').length > 0) {
        this.options.filterView.choices.each(function (item) {
          item.set('checked', false);
        });
      } else {
        this.options.filterView.choices.filter(function (item) {
          return item.get('id')[0] === '!';
        }).forEach(function (item) {
          item.set('checked', false);
        });
      }
    } else {
      this.options.filterView.choices.each(function (item) {
        item.set('checked', false);
      });
    }

    this.options.filterView.choices.get(id).set('checked', !checked);
    this.updateValue();
    this.updateLists();
  },


  updateValue: function () {
    this.model.set('value', this.options.filterView.getSelected().map(function (m) {
      return m.get('id');
    }));
  },


  updateCurrent: function (index) {
    this.currentChoice = index;
    this.$('label').removeClass('current')
        .eq(this.currentChoice).addClass('current');
  },


  onShow: function () {
    this.bindedOnKeyDown = _.bind(this.onKeyDown, this);
    $('body').on('keydown', this.bindedOnKeyDown);
  },


  onHide: function () {
    $('body').off('keydown', this.bindedOnKeyDown);
  },


  onKeyDown: function (e) {
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
      default:
        // Not a functional key - then skip
        break;
    }
  },


  selectNextChoice: function () {
    if (this.$('label').length > this.currentChoice + 1) {
      this.updateCurrent(this.currentChoice + 1);
      this.scrollNext();
    }
  },


  scrollNext: function () {
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


  selectPrevChoice: function () {
    if (this.currentChoice > 0) {
      this.updateCurrent(this.currentChoice - 1);
      this.scrollPrev();
    }
  },


  scrollPrev: function () {
    var currentLabel = this.$('label').eq(this.currentChoice);
    if (currentLabel.length > 0) {
      var list = currentLabel.closest('ul'),
          labelPos = currentLabel.offset().top - list.offset().top;

      if (labelPos < 0) {
        list.scrollTop(list.scrollTop() + labelPos);
      }
    }
  },


  selectCurrent: function () {
    var cb = this.$('label').eq(this.currentChoice);
    cb.click();
  },


  serializeData: function () {
    return _.extend({}, this.model.toJSON(), {
      choices: new Backbone.Collection(this.options.filterView.choices.reject(function (item) {
        return item.get('id')[0] === '!';
      })).toJSON(),
      opposite: new Backbone.Collection(this.options.filterView.choices.filter(function (item) {
        return item.get('id')[0] === '!';
      })).toJSON()
    });
  }

});


var ChoiceFilterView = BaseFilters.BaseFilterView.extend({

  initialize: function (options) {
    BaseFilters.BaseFilterView.prototype.initialize.call(this, {
      detailsView: (options && options.detailsView) ? options.detailsView : DetailsChoiceFilterView
    });

    var index = 0,
        icons = this.model.get('choiceIcons');

    this.choices = new Backbone.Collection(
        _.map(this.model.get('choices'), function (value, key) {
          var model = new Backbone.Model({
            id: key,
            text: value,
            checked: false,
            index: index++
          });

          if (icons && icons[key]) {
            model.set('icon', icons[key]);
          }

          return model;
        }), { comparator: 'index' }
    );
  },


  getSelected: function () {
    return this.choices.filter(function (m) {
      return m.get('checked');
    });
  },


  renderInput: function () {
    var input = $('<select>')
        .prop('name', this.model.get('property'))
        .prop('multiple', true)
        .css('display', 'none');
    this.choices.each(function (item) {
      var option = $('<option>')
          .prop('value', item.get('id'))
          .prop('selected', item.get('checked'))
          .text(item.get('text'));
      option.appendTo(input);
    });
    input.appendTo(this.$el);
  },


  renderValue: function () {
    var value = this.getSelected().map(function (item) {
          return item.get('text');
        }),
        defaultValue = this.model.has('defaultValue') ?
            this.model.get('defaultValue') :
            this.model.get('multiple') ? window.t('all') : window.t('any');

    return this.isDefaultValue() ? defaultValue : value.join(', ');
  },


  isDefaultValue: function () {
    var selected = this.getSelected();
    return selected.length === 0;
  },


  disable: function () {
    this.choices.each(function (item) {
      item.set('checked', false);
    });
    BaseFilters.BaseFilterView.prototype.disable.apply(this, arguments);
  },


  restoreFromQuery: function (q) {
    var param = _.findWhere(q, { key: this.model.get('property') });

    if (this.choices) {
      this.choices.forEach(function (item) {
        if (item.get('id')[0] === '!') {
          var x = _.findWhere(q, { key: item.get('id').substr(1) });
          if (item.get('id').indexOf('=') >= 0) {
            var key = item.get('id').split('=')[0].substr(1);
            var value = item.get('id').split('=')[1];
            x = _.findWhere(q, { key: key, value: value });
          }
          if (x == null) {
            return;
          }
          if (!param) {
            param = { value: item.get('id') };
          } else {
            param.value += ',' + item.get('id');
          }
        }
      });
    }

    if (param && param.value) {
      this.model.set('enabled', true);
      this.restore(param.value, param);
    } else {
      this.clear();
    }
  },


  restore: function (value) {
    if (_.isString(value)) {
      value = value.split(',');
    }

    if (this.choices && value.length > 0) {
      var that = this;

      that.choices.each(function (item) {
        item.set('checked', false);
      });

      var unknownValues = [];

      _.each(value, function (v) {
        var cModel = that.choices.findWhere({ id: v });
        if (cModel) {
          cModel.set('checked', true);
        } else {
          unknownValues.push(v);
        }
      });

      value = _.difference(value, unknownValues);

      this.model.set({
        value: value,
        enabled: true
      });

      this.render();
    } else {
      this.clear();
    }
  },


  clear: function () {
    if (this.choices) {
      this.choices.each(function (item) {
        item.set('checked', false);
      });
    }
    this.model.unset('value');
    this.detailsView.render();
    if (this.detailsView.updateCurrent) {
      this.detailsView.updateCurrent(0);
    }
  },


  formatValue: function () {
    var q = {};
    if (this.model.has('property') && this.model.has('value') && this.model.get('value').length > 0) {
      var opposite = _.filter(this.model.get('value'), function (item) {
        return item[0] === '!';
      });
      if (opposite.length > 0) {
        opposite.forEach(function (item) {
          if (item.indexOf('=') >= 0) {
            var paramValue = item.split('=');
            q[paramValue[0].substr(1)] = paramValue[1];
          } else {
            q[item.substr(1)] = false;
          }
        });
      } else {
        q[this.model.get('property')] = this.model.get('value').join(',');
      }
    }
    return q;
  }

});


/*
 * Export public classes
 */

export default {
  DetailsChoiceFilterView: DetailsChoiceFilterView,
  ChoiceFilterView: ChoiceFilterView
};
