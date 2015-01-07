function showMessage(div_id, message) {
  $j('#' + div_id + 'msg').html(message);
  $j('#' + div_id).show();
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
      star.removeClass('icon-favorite icon-not-favorite');
      star.addClass(data.css);
      star.attr('title', data.title);
    }});
}

function dashboardParameters (urlHasSomething) {
  var queryString = window.location.search;
  var parameters = [];

  var matchDashboard = queryString.match(/did=\d+/);
  if (matchDashboard && $j('#is-project-dashboard').length === 1) {
    parameters.push(matchDashboard[0]);
  }

  var matchPeriod = queryString.match(/period=\d+/);
  if (matchPeriod) {
    // If we have a match for period, check that it is not project-specific
    var period = parseInt(/period=(\d+)/.exec(queryString)[1]);
    if (period <= 3) {
      parameters.push(matchPeriod[0]);
    }
  }

  var query = parameters.join('&');
  if (query !== '') {
    query = (urlHasSomething ? '&' : '?') + query;
  }
  return query;
}


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
  this.breadcrumb.forEach(function (ctx) {
    output += ctx.label + '&nbsp;/&nbsp;';
  });
  $j('#tm-bc-' + this.id).html(output);
  $j('#tm-loading-' + this.id).show();
  var self = this;
  $j.ajax({
    type: 'GET',
    url: baseUrl + '/treemap/index?html_id=' + this.id + '&size_metric=' + this.sizeMetric +
        '&color_metric=' + this.colorMetric + '&resource=' + context.rid,
    dataType: 'html',
    success: function (data) {
      if (data.length > 1) {
        self.rootNode().html(data);
        self.initNodes();
      } else {
        // SONAR-3524
        // When data is empty, do not display it, revert breadcrumb state and display a message to user
        self.breadcrumb.pop();
        $j('#tm-bottom-level-reached-msg-' + self.id).show();
      }
      $j('#tm-loading-' + self.id).hide();
    }
  });
};
Treemap.prototype.rootNode = function () {
  return $j('#tm-' + this.id);
};

Treemap.prototype.initNodes = function () {
  var self = this;
  $j('#tm-' + this.id).find('a').each(function () {
    $j(this).on('click', function (event) {
      event.stopPropagation();
    });
  });
  $j('#tm-' + this.id).find('[rid]').each(function () {
    $j(this).on('contextmenu', function (event) {
      event.stopPropagation();
      event.preventDefault();
      $j('#tm-bottom-level-reached-msg-' + self.id).hide();
      // right click
      if (self.breadcrumb.length > 1) {
        self.breadcrumb.pop();
        self.load();
      } else if (self.breadcrumb.length === 1) {
        $j('#tm-loading-' + self.id).show();
        location.reload();
      }
      return false;
    });
    $j(this).on('click', function () {
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
  var width = (options && options.width) || 540;
  var $dialog = $j('#modal');
  if (!$dialog.length) {
    $dialog = $j('<div id="modal" class="ui-widget-overlay ui-front"></div>').appendTo('body');
  }
  $j.get(url, function (html) {
    $dialog.removeClass('ui-widget-overlay');
    $dialog.html(html);
    $dialog
        .dialog({
          dialogClass: 'no-close',
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
    $dialog.dialog('open');
  }).fail(function () {
    alert('Server error. Please contact your administrator.');
  }).always(function () {
    $dialog.removeClass('ui-widget-overlay');
  });
  return false;
}

(function ($j) {
  $j.fn.extend({
    openModal: function () {
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
        obj.submit(function () {
          $j('input[type=submit]', this).attr('disabled', 'disabled');
          $j.ajax($j.extend({
            type: 'POST',
            url: obj.attr('action'),
            data: obj.serialize(),
            success: function () {
              window.location.reload();
            },
            error: function (xhr) {
              // If the modal window has defined a modal-error element, then returned text must be displayed in it
              var errorElt = obj.find('.modal-error');
              if (errorElt.length) {
                // Hide all loading images
                $j('.loading-image').addClass('hidden');
                // Re activate submit button
                $j('input[type=submit]', obj).removeAttr('disabled');
                errorElt.show();
                errorElt.html($j('<div/>').html(xhr.responseText).text());
              } else {
                // otherwise replace modal window by the returned text
                $j('#modal').html(xhr.responseText);
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

function supportsHTML5Storage() {
  try {
    return 'localStorage' in window && window.localStorage !== null;
  } catch (e) {
    return false;
  }
}

//******************* HANDLING OF ACCORDION NAVIGATION [BEGIN] ******************* //

function openAccordionItem(url) {
  return $j.ajax({
    url: url
  }).fail(function (jqXHR, textStatus) {
    var error = 'Server error. Please contact your administrator. The status of the error is : ' +
        jqXHR.status + ', textStatus is : ' + textStatus;
    console.log(error);
    $j('#accordion-panel').append($j('<div class="error">').append(error));
  }).done(function (html) {
    var panel = $j('#accordion-panel');
    panel.html(html);
    panel.scrollIntoView(false);
  });
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

  if (dropdownElt === currentlyDisplayedDropdownMenu) {
    currentlyDisplayedDropdownMenu = '';
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
  window.open(url, popupId, 'height=800,width=900,scrollbars=1,resizable=1');
  return false;
}


jQuery(function () {

  // Initialize top search
  jQuery('#searchInput').topSearch({
    minLength: 2,
    results: '#searchResourcesResults',
    spinner: '#searchingResources'
  });


  // Process login link in order to add the anchor
  jQuery('#login-link').on('click', function (e) {
    e.preventDefault();
    var href = jQuery(this).prop('href'),
        hash = window.location.hash;
    if (hash.length > 0) {
      href += decodeURIComponent(hash);
    }
    window.location = href;
  });


  // Define global shortcuts
  key('s', function () {
    jQuery('.js-search-dropdown-toggle').dropdown('toggle');
    return false;
  });
});
