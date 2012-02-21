function showMessage(div_id, message) {
  $(div_id + 'msg').innerHTML = message;
  $(div_id).show();
}
function error(message) {
  showMessage('error', message);
}
function warning(message) {
  showMessage('warning', message);
}
function info(message) {
  showMessage('info', message);
}

function autocompleteResources() {
  $('searchInput').value = '';
  new Ajax.Autocompleter('searchInput', 'searchResourcesResults', baseUrl + '/search', {
    method:'post',
    minChars:3,
    indicator:'searchingResources',
    paramName:'s',
    updateElement:function (item) {
      if (item.id) {
        window.location = baseUrl + '/dashboard/index/' + item.id;
      }
    },
    onShow:function (element, update) { /* no update */
      update.show();
    }
  });
}

var SelectBox = {
  cache:new Object(),
  init:function (id) {
    var box = document.getElementById(id);
    var node;
    SelectBox.cache[id] = new Array();
    var cache = SelectBox.cache[id];
    for (var i = 0; (node = box.options[i]); i++) {
      cache.push({value:node.value, text:node.text, displayed:1});
    }
  },
  redisplay:function (id) {
    // Repopulate HTML select box from cache
    var box = document.getElementById(id);
    box.options.length = 0; // clear all options
    for (var i = 0, j = SelectBox.cache[id].length; i < j; i++) {
      var node = SelectBox.cache[id][i];
      if (node.displayed) {
        box.options[box.options.length] = new Option(node.text, node.value, false, false);
      }
    }
  },
  filter:function (id, text) {
    // Redisplay the HTML select box, displaying only the choices containing ALL
    // the words in text. (It's an AND search.)
    var tokens = text.toLowerCase().split(/\s+/);
    var node, token;
    for (var i = 0; (node = SelectBox.cache[id][i]); i++) {
      node.displayed = 1;
      for (var j = 0; (token = tokens[j]); j++) {
        if (node.text.toLowerCase().indexOf(token) == -1) {
          node.displayed = 0;
        }
      }
    }
    SelectBox.redisplay(id);
  },
  delete_from_cache:function (id, value) {
    var node, delete_index = null;
    for (var i = 0; (node = SelectBox.cache[id][i]); i++) {
      if (node.value == value) {
        delete_index = i;
        break;
      }
    }
    var j = SelectBox.cache[id].length - 1;
    for (var i = delete_index; i < j; i++) {
      SelectBox.cache[id][i] = SelectBox.cache[id][i + 1];
    }
    SelectBox.cache[id].length--;
  },
  add_to_cache:function (id, option) {
    SelectBox.cache[id].push({value:option.value, text:option.text, displayed:1});
  },
  cache_contains:function (id, value) {
    // Check if an item is contained in the cache
    var node;
    for (var i = 0; (node = SelectBox.cache[id][i]); i++) {
      if (node.value == value) {
        return true;
      }
    }
    return false;
  },
  move:function (from, to) {
    var from_box = document.getElementById(from);
    var option;
    for (var i = 0; (option = from_box.options[i]); i++) {
      if (option.selected && SelectBox.cache_contains(from, option.value)) {
        SelectBox.add_to_cache(to, {value:option.value, text:option.text, displayed:1});
        SelectBox.delete_from_cache(from, option.value);
      }
    }
    SelectBox.redisplay(from);
    SelectBox.redisplay(to);
  },
  move_all:function (from, to) {
    var from_box = document.getElementById(from);
    var option;
    for (var i = 0; (option = from_box.options[i]); i++) {
      if (SelectBox.cache_contains(from, option.value)) {
        SelectBox.add_to_cache(to, {value:option.value, text:option.text, displayed:1});
        SelectBox.delete_from_cache(from, option.value);
      }
    }
    SelectBox.redisplay(from);
    SelectBox.redisplay(to);
  },
  sort:function (id) {
    SelectBox.cache[id].sort(function (a, b) {
      a = a.text.toLowerCase();
      b = b.text.toLowerCase();
      try {
        if (a > b) return 1;
        if (a < b) return -1;
      }
      catch (e) {
        // silently fail on IE 'unknown' exception
      }
      return 0;
    });
  },
  select_all:function (id) {
    var box = document.getElementById(id);
    for (var i = 0; i < box.options.length; i++) {
      box.options[i].selected = 'selected';
    }
  }
};


var treemaps = {};

function treemapById(id) {
  return treemaps[id];
}
var TreemapContext = function (type, id, label) {
  this.type = type;
  this.id = id;
  this.label = label;
};

/**
 * HTML elements :
 * tm-#{id} : required treemap container
 * tm-bc-#{id} : required breadcrumb
 * tm-loading-#{id} : optional loading icon
 */
var Treemap = function (id, sizeMetric, colorMetric, heightInPercents) {
  this.id = id;
  this.sizeMetric = sizeMetric;
  this.colorMetric = colorMetric;
  this.heightInPercents = heightInPercents;
  this.breadcrumb = [];
  treemaps[id] = this;
};
Treemap.prototype.initResource = function (resourceId) {
  this.breadcrumb.push(new TreemapContext('resource', resourceId, ''));
  return this;
};
Treemap.prototype.initFilter = function (filterId) {
  this.breadcrumb.push(new TreemapContext('filter', filterId, ''));
  return this;
};
Treemap.prototype.init = function (type, id) {
  this.breadcrumb.push(new TreemapContext(type, id, ''));
  return this;
};
Treemap.prototype.changeSizeMetric = function (metric) {
  this.sizeMetric = metric;
  this.load();
  return false;
};
Treemap.prototype.changeColorMetric = function (metric) {
  this.colorMetric = metric;
  this.load();
  return false;
};
Treemap.prototype.currentContext = function () {
  if (this.breadcrumb.length > 0) {
    return this.breadcrumb[this.breadcrumb.length - 1];
  }
  return null;
};
Treemap.prototype.width = function () {
  return $('tm-' + this.id).getWidth() - 10;
};
Treemap.prototype.load = function () {
  var context = this.currentContext();
  var width = this.width();
  var height = Math.round(width * Math.abs(this.heightInPercents / 100.0));

  var output = '';
  this.breadcrumb.each(function (ctx) {
    output += ctx.label + '&nbsp;/&nbsp;';
  });
  if ($('tm-bc-' + this.id)!=null) {
  $('tm-bc-' + this.id).innerHTML = output;}
  var loadingIcon = $('tm-loading-' + this.id);
  if (loadingIcon != null) {
    loadingIcon.show();
  }

  new Ajax.Request(
    baseUrl + '/treemap/index?id=' + this.id + '&width=' + width + '&height=' + height + '&size_metric=' + this.sizeMetric + '&color_metric=' + this.colorMetric + '&' + context.type + '=' + context.id,
    {
      asynchronous:true,
      evalScripts:true
    });
};
Treemap.prototype.htmlNode = function (nodeId) {
  return $('tm-node-' + this.id + '-' + nodeId);
};
Treemap.prototype.handleClick = function (event) {
  if (Event.isLeftClick(event)) {
    var link = event.findElement('a');
    if (link != null) {
      event.stopPropagation();
      return false;
    }

    var elt = event.findElement('div');
    var rid = elt.readAttribute('rid');
    var leaf = elt.hasAttribute('l');
    if (!leaf) {
      var label = elt.innerText || elt.textContent;
      var context = new TreemapContext('resource', rid, label);
      this.breadcrumb.push(context);
      this.load();
    }

  } else if (Event.isRightClick(event)) {
    if (this.breadcrumb.length > 1) {
      this.breadcrumb.pop();
      this.load();
    }
  }
};
Treemap.prototype.onLoaded = function (componentsSize) {
  for (var i = 1; i <= componentsSize; i++) {
    var elt = this.htmlNode(i);
    elt.oncontextmenu = function () {
      return false
    };
    elt.observe('mouseup', this.handleClick.bind(this));
  }
};