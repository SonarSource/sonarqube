// Place your application-specific JavaScript functions and classes here
// This file is automatically included by javascript_include_tag :defaults
function registerAjaxErrors() {
	var Messages = {
	  lastMessageId: 0,
	  lastRequestLastMessageId: 0,
	 
	  onComplete: function(request, transport, json) {
	    this.extractMessages(json);
	  },
	 
	  onFailure: function(request, transport, json) {
	    this.extractMessages(json);
	  },
	 
	  extractMessages: function(json) {
	    target = json['target'];
	    if ( target == null ) {
	    	target = 'messages';
	    }

	    for (msgClass in json) {
	      if ( msgClass == 'target' ) continue;
	      msgsOfClass = typeof( json[msgClass] ) == 'string' ? new Array(json[msgClass]) : json[msgClass];
	      for (var i = 0; i < msgsOfClass.length; i++) {
	        this.displayMessage(target,msgClass, msgsOfClass[i]);
	      }
	    }
	    this.lastRequestLastMessageId = this.lastMessageId;
	  },
	 
	  displayMessage: function(target, msgClass, msgText) {
	    if (this.lastMessageId == this.lastRequestLastMessageId) {
	      msgEls = $A($(target).getElementsByTagName('li'));
	      for (var i = 0; i < msgEls.length; i++) {
	        Effect.Fade(msgEls[i].id, { afterFinish: function() {Element.remove(msgEls[i]);} } );
	      }
	    }
	 
	    msgId = 'message-' + ++this.lastMessageId;
	    liEl = document.createElement('li');
	    liEl.setAttribute('id', msgId);
	    liEl.setAttribute('class', msgClass);
	    liEl.setAttribute('title', "Click to close.");
	    liEl.setAttribute('onclick', 'Element.remove(this);');
	    liEl.setAttribute('style', 'cursor: pointer;');
	    liEl.appendChild( document.createTextNode(msgText) );
	 
	    $(target).appendChild(liEl);
	  }
	};
	 
	Ajax.Responders.register(Messages);
}
function displayImage(imageId, imageUrl) {
  var newImage = new Image();
  newImage.src = imageUrl;
  //$(imageId).src = '../../images/loading.gif';
  new PeriodicalExecuter(function(pe) {
    if (newImage.complete) {
      $(imageId).src = imageUrl;
      fixTransparencyForImg($(imageId));
      pe.stop();
    }
  }, 0.5);
}
function showMessage(div_id,message) {
  $(div_id + 'msg').innerHTML=message;
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

function displayElement( elementId ) {
   	$( elementId ).style.visibility='visible';
   	$( elementId ).style.display='inline';
   	return false;
}

function hideElement( elementId ) {
   	$( elementId ).style.visibility='hidden';
   	$( elementId ).style.display='none';
   	return false;
}

var projects;
function autocompleteProjects(APIURL, projectURL, searchInput, searchResult) {
  if (projects != null) return;
  new Ajax.Request(APIURL, {
    method: 'get',
    requestHeaders: {Accept: 'application/json'},
	onComplete: function(transport) {
	  if (200 == transport.status) {
        // not handled automatically with current prototype version
        projects = eval(transport.responseText);
        var projectNames = new Array();
        for( i = 0; i < projects.length; i++ ) {
          projectNames[i] = projects[i].name
        }
        new Autocompleter.Local(searchInput.id, searchResult.id, projectNames,
          {afterUpdateElement:
            function(selectedLi) {
              for( i = 0; i < projects.length; i++ ) {
                if ( projects[i].name == searchInput.value ) {
                  searchInput.disable();
                  window.location= projectURL + projects[i].key;
                }
              }
            }, fullSearch: true, frequency: 0.1, choices: 50
          });
      } else {
	    error('Server error while searching projects.');
      }
    }
  });
}

var SelectBox = {
    cache: new Object(),
    init: function(id) {
        var box = document.getElementById(id);
        var node;
        SelectBox.cache[id] = new Array();
        var cache = SelectBox.cache[id];
        for (var i = 0; (node = box.options[i]); i++) {
            cache.push({value: node.value, text: node.text, displayed: 1});
        }
    },
    redisplay: function(id) {
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
    filter: function(id, text) {
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
    delete_from_cache: function(id, value) {
        var node, delete_index = null;
        for (var i = 0; (node = SelectBox.cache[id][i]); i++) {
            if (node.value == value) {
                delete_index = i;
                break;
            }
        }
        var j = SelectBox.cache[id].length - 1;
        for (var i = delete_index; i < j; i++) {
            SelectBox.cache[id][i] = SelectBox.cache[id][i+1];
        }
        SelectBox.cache[id].length--;
    },
    add_to_cache: function(id, option) {
        SelectBox.cache[id].push({value: option.value, text: option.text, displayed: 1});
    },
    cache_contains: function(id, value) {
        // Check if an item is contained in the cache
        var node;
        for (var i = 0; (node = SelectBox.cache[id][i]); i++) {
            if (node.value == value) {
                return true;
            }
        }
        return false;
    },
    move: function(from, to) {
        var from_box = document.getElementById(from);
        var to_box = document.getElementById(to);
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
    move_all: function(from, to) {
        var from_box = document.getElementById(from);
        var to_box = document.getElementById(to);
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
    sort: function(id) {
        SelectBox.cache[id].sort( function(a, b) {
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
        } );
    },
    select_all: function(id) {
        var box = document.getElementById(id);
        for (var i = 0; i < box.options.length; i++) {
            box.options[i].selected = 'selected';
        }
    }
}