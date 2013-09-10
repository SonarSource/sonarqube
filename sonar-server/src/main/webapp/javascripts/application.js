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
function toggleFav(resourceId, elt) {
  $j.ajax({type: 'POST', dataType: 'json', url: baseUrl + '/favourites/toggle/' + resourceId,
    success: function (data) {
      var star = $j(elt);
      star.removeClass('fav notfav');
      star.addClass(data['css']);
      star.attr('title', data['title']);
    }});
}

function autocompleteResources() {
  $('searchInput').value = '';
  new Ajax.Autocompleter('searchInput', 'searchResourcesResults', baseUrl + '/search', {
    method: 'post',
    minChars: 2,
    indicator: 'searchingResources',
    paramName: 's',
    updateElement: function (item) {
      if (item.id) {
        window.location = baseUrl + '/dashboard/index/' + item.id;
      }
    },
    onShow: function (element, update) { /* no update */
      update.show();
    }
  });
}

var SelectBox = {
  cache: new Object(),
  init: function (id) {
    var box = document.getElementById(id);
    var node;
    SelectBox.cache[id] = [];
    var cache = SelectBox.cache[id];
    for (var i = 0; (node = box.options[i]); i++) {
      cache.push({value: node.value, text: node.text, displayed: 1});
    }
  },
  redisplay: function (id) {
    // Repopulate HTML select box from cache
    var box = document.getElementById(id);
    // clear all options
    box.options.length = 0;
    for (var i = 0, j = SelectBox.cache[id].length; i < j; i++) {
      var node = SelectBox.cache[id][i];
      if (node.displayed) {
        box.options[box.options.length] = new Option(node.text, node.value, false, false);
      }
    }
  },
  filter: function (id, text) {
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
  delete_from_cache: function (id, value) {
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
  add_to_cache: function (id, option) {
    SelectBox.cache[id].push({value: option.value, text: option.text, displayed: 1});
  },
  cache_contains: function (id, value) {
    // Check if an item is contained in the cache
    var node;
    for (var i = 0; (node = SelectBox.cache[id][i]); i++) {
      if (node.value == value) {
        return true;
      }
    }
    return false;
  },
  move: function (from, to) {
    var from_box = document.getElementById(from);
    var option;
    for (var i = 0; (option = from_box.options[i]); i++) {
      if (option.selected && SelectBox.cache_contains(from, option.value)) {
        SelectBox.add_to_cache(to, {value: option.value, text: option.text, displayed: 1});
        SelectBox.delete_from_cache(from, option.value);
      }
    }
    SelectBox.redisplay(from);
    SelectBox.redisplay(to);
  },
  move_all: function (from, to) {
    var from_box = document.getElementById(from);
    var option;
    for (var i = 0; (option = from_box.options[i]); i++) {
      if (SelectBox.cache_contains(from, option.value)) {
        SelectBox.add_to_cache(to, {value: option.value, text: option.text, displayed: 1});
        SelectBox.delete_from_cache(from, option.value);
      }
    }
    SelectBox.redisplay(from);
    SelectBox.redisplay(to);
  },
  sort: function (id) {
    SelectBox.cache[id].sort(function (a, b) {
      a = a.text.toLowerCase();
      b = b.text.toLowerCase();
      try {
        if (a > b) {
          return 1;
        }
        if (a < b) {
          return -1;
        }
      }
      catch (e) {
        // silently fail on IE 'unknown' exception
      }
      return 0;
    });
  },
  select_all: function (id) {
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
var TreemapContext = function (rid, label) {
  this.rid = rid;
  this.label = label;
};

/**
 * HTML elements :
 * tm-#{id} : required treemap container
 * tm-bc-#{id} : required breadcrumb
 * tm-loading-#{id} : optional loading icon
 */
var Treemap = function (id, sizeMetric, colorMetric, heightPercents) {
  this.id = id;
  this.sizeMetric = sizeMetric;
  this.colorMetric = colorMetric;
  this.breadcrumb = [];
  treemaps[id] = this;
  this.rootNode().height(this.rootNode().width() * heightPercents / 100);
  this.initNodes();

};
Treemap.prototype.currentContext = function () {
  if (this.breadcrumb.length > 0) {
    return this.breadcrumb[this.breadcrumb.length - 1];
  }
  return null;
};
Treemap.prototype.load = function () {
  var context = this.currentContext();
  var output = '';
  this.breadcrumb.each(function (ctx) {
    output += ctx.label + '&nbsp;/&nbsp;';
  });
  $j('#tm-bc-' + this.id).html(output);
  $j('#tm-loading-' + this.id).show();
  var self = this;
  $j.ajax({
    type: "GET",
    url: baseUrl + '/treemap/index?html_id=' + this.id + '&size_metric=' + this.sizeMetric + '&color_metric=' + this.colorMetric + '&resource=' + context.rid,
    dataType: "html",
    success: function (data) {
      if (data.length > 1) {
        self.rootNode().html(data);
        self.initNodes();
      } else {
        // SONAR-3524
        // When data is empty, do not display it, revert breadcrumb state and display a message to user
        self.breadcrumb.pop();
        $j("#tm-bottom-level-reached-msg-" + self.id).show();
      }
      $j("#tm-loading-" + self.id).hide();
    }
  });
};
Treemap.prototype.rootNode = function () {
  return $j('#tm-' + this.id);
};

Treemap.prototype.initNodes = function () {
  var self = this;
  $j('#tm-' + this.id).find('a').each(function (index) {
    $j(this).on("click", function (event) {
      event.stopPropagation();
    });
  });
  $j('#tm-' + this.id).find('[rid]').each(function (index) {
    $j(this).on("contextmenu", function (event) {
      event.stopPropagation();
      event.preventDefault();
      $j("#tm-bottom-level-reached-msg-" + self.id).hide();
      // right click
      if (self.breadcrumb.length > 1) {
        self.breadcrumb.pop();
        self.load();
      } else if (self.breadcrumb.length == 1) {
        $j("#tm-loading-" + self.id).show();
        location.reload();
      }
      return false;
    });
    $j(this).on("click", function (event) {
        var source = $j(this);
        var rid = source.attr('rid');
        var context = new TreemapContext(rid, source.text());
        self.breadcrumb.push(context);
        self.load();
      }
    );
  });
};

function openModalWindow(url, options) {
  var width = options['width']||540;
  var $dialog = $j('#modal');
  if (!$dialog.length) {
    $dialog = $j('<div id="modal" class="ui-widget-overlay"></div>').appendTo('body');
  }
  $j.get(url,function (html) {
    $dialog.removeClass('ui-widget-overlay');
    $dialog.html(html);
    $dialog
      .dialog({
        dialogClass: "no-close",
        width: width,
        draggable: false,
        autoOpen: false,
        modal: true,
        minHeight: 50,
        resizable: false,
        title: null,
        close: function () {
          $j('#modal').remove();
        }
      });
    $dialog.dialog("open");
  }).fail(function () {
      alert("Server error. Please contact your administrator.");
    }).always(function () {
      $dialog.removeClass('ui-widget-overlay');
    });
  return false;
}

(function ($j) {
  $j.fn.extend({
    openModal: function() {
      return this.each(function () {
        var obj = $j(this);
        var url = obj.attr('modal-url') || obj.attr('href');
        return openModalWindow(url, {'width': obj.attr('modal-width')});
      });
    },
    modal: function () {
      return this.each(function () {
        var obj = $j(this);
        obj.unbind('click');
        var $link = obj.bind('click', function () {
          $link.openModal();
          return false;
        });
      });
    },
    modalForm: function (ajax_options) {
      return this.each(function () {
        var obj = $j(this);
        obj.submit(function (event) {
          $j('input[type=submit]', this).attr('disabled', 'disabled');
          $j.ajax($j.extend({
            type: 'POST',
            url: obj.attr('action'),
            data: obj.serialize(),
            success: function (data) {
              window.location.reload();
            },
            error: function (xhr, textStatus, errorThrown) {
              // If the modal window has defined a modal-error element, then returned text must be displayed in it
              var errorElt = obj.find(".modal-error");
              if (errorElt.length) {
                // Hide all loading images
                $j('.loading-image').addClass("hidden");
                // Re activate submit button
                $j('input[type=submit]', obj).removeAttr('disabled');
                errorElt.show();
                errorElt.html(xhr.responseText);
              } else {
                // otherwise replace modal window by the returned text
                $j("#modal").html(xhr.responseText);
              }
            }
          }, ajax_options));
          return false;
        });
      });
    }
  });
})(jQuery);

function closeModalWindow() {
  $j('#modal').dialog('close');
  return false;
}

function supports_html5_storage() {
  try {
    return 'localStorage' in window && window['localStorage'] !== null;
  } catch (e) {
    return false;
  }
}

//******************* HANDLING OF ACCORDION NAVIGATION [BEGIN] ******************* //

function openAccordionItem(url, elt, updateCurrentElement) {
  var htmlClass = 'accordion-item';
  var currentElement = $j(elt).closest('.'+ htmlClass);

  // Display loading image
  var loadingImg = new Image();
  loadingImg.src = baseUrl + "/images/loading.gif";
  loadingImg.className = 'accordion-loading';
  var loading = $j(loadingImg);
  var existingLoading = currentElement.find('.accordion-loading');
  if (updateCurrentElement && existingLoading.length) {
    existingLoading.show();
    loading.hide();
  }

  // Remove elements under current element
  if (currentElement.length) {
    // Fix the height in order to not change the position on the screen when removing elements under current element
    var elementToRemove = currentElement.nextAll('.'+ htmlClass);
    if (elementToRemove.height()) {
      $j("#accordion-panel").height($j("#accordion-panel").height() + elementToRemove.height());
    }
    // Remove all accordion items after current element
    elementToRemove.remove();
    loading.insertAfter(currentElement);
  } else {
    // Current element is not in a working view, remove all working views
    $j('.'+ htmlClass).remove();
    loading.insertAfter($j("#accordion-panel"));
  }

  // Get content from url
  var ajaxRequest = $j.ajax({
      url: url
      }).fail(function (jqXHR, textStatus) {
        var error = "Server error. Please contact your administrator. The status of the error is : "+ jqXHR.status + ", textStatus is : "+ textStatus;
        console.log(error);
        $j("#accordion-panel").append($j('<div class="error">').append(error));
      }).done(function (html) {
        if (currentElement.length) {
          var body = currentElement.find('.accordion-item-body');
          if (!updateCurrentElement && !body.hasClass('accordion-item-body-medium')) {
            body.addClass("accordion-item-body-medium");
            elt.scrollIntoView(false);
          }
        } else {
          $j("#accordion-panel").height('auto');

          // Current element is not in a working view, remove again all working views to purge elements that could be added just before this one
          $j('.'+ htmlClass).remove();
        }

        if (updateCurrentElement) {
          // Fix the height in order to not change the position on the screen
          var prevHeight = $j("#accordion-panel").height();
          currentElement.html(html);
          $j("#accordion-panel").height('auto');
          var newHeight = $j("#accordion-panel").height();
          if (prevHeight > newHeight) {
            $j("#accordion-panel").height(prevHeight);
          } else {
            $j("#accordion-panel").height(newHeight);
          }
        } else {
          // Add new item add the end of the panel and restore the height param
          var nbElement = $j("."+htmlClass).size();
          var newElement = $j('<div id="'+ htmlClass + nbElement +'" class="'+ htmlClass +'">');
          $j("#accordion-panel").append(newElement);

          // Add html after having adding the new element in the page in order to scripts (for instance for GWT) to be well executed
          newElement.append(html);
          $j("#accordion-panel").height('auto');

          // Set the focus on the top of the current item with animation
          if (currentElement.length) {
            $j('html, body').animate({
              scrollTop: currentElement.offset().top}, 500
            );
          }
        }
        loading.remove();
      });
  return ajaxRequest;
}


function expandAccordionItem(elt) {
  var currentElement = $j(elt).closest('.accordion-item');
  currentElement.find('.accordion-item-body').removeClass("accordion-item-body-medium");
}

//******************* HANDLING OF ACCORDION NAVIGATION [END] ******************* //


//******************* HANDLING OF DROPDOWN MENUS [BEGIN] ******************* //

var currentlyDisplayedDropdownMenu;

var hideCurrentDropdownMenu = function () {
  if (currentlyDisplayedDropdownMenu) {
    currentlyDisplayedDropdownMenu.hide();
  }
  $j(document).unbind('mouseup', hideCurrentDropdownMenu);
};

var clickOnDropdownMenuLink = function (event) {
  var link = $j(event.target).children('a');
  if (link) {
    var href = link.attr('href');
    if (href && href.length > 1) {
      // there's a real link, not a href="#"
      window.location = href;
    } else {
      // otherwise, this means that the link is handled with an onclick event (for Ajax calls)
      link.click();
    }
  }
};

function showDropdownMenu(menuId) {
  showDropdownMenuOnElement($j('#' + menuId));
}

function showDropdownMenuOnElement(elt) {
  var dropdownElt = $j(elt);

  if (dropdownElt == currentlyDisplayedDropdownMenu) {
    currentlyDisplayedDropdownMenu = "";
  } else {
    currentlyDisplayedDropdownMenu = dropdownElt;
    $j(document).mouseup(hideCurrentDropdownMenu);

    var dropdownChildren = dropdownElt.find('li');
    dropdownChildren.unbind('click');
    dropdownChildren.click(clickOnDropdownMenuLink);
    dropdownElt.show();
  }
}

//******************* HANDLING OF DROPDOWN MENUS [END] ******************* //

function openPopup(url, popupId) {
  window.open(url,popupId,'height=800,width=900,scrollbars=1,resizable=1');
  return false;
}
