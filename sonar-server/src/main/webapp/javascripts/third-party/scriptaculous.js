// script.aculo.us builder.js v1.9.0, Thu Dec 23 16:54:48 -0500 2010

// Copyright (c) 2005-2010 Thomas Fuchs (http://script.aculo.us, http://mir.aculo.us)
//
// script.aculo.us is freely distributable under the terms of an MIT-style license.
// For details, see the script.aculo.us web site: http://script.aculo.us/

var Builder = {
  NODEMAP:{
    AREA:'map',
    CAPTION:'table',
    COL:'table',
    COLGROUP:'table',
    LEGEND:'fieldset',
    OPTGROUP:'select',
    OPTION:'select',
    PARAM:'object',
    TBODY:'table',
    TD:'table',
    TFOOT:'table',
    TH:'table',
    THEAD:'table',
    TR:'table'
  },
  // note: For Firefox < 1.5, OPTION and OPTGROUP tags are currently broken,
  //       due to a Firefox bug
  node:function (elementName) {
    elementName = elementName.toUpperCase();

    // try innerHTML approach
    var parentTag = this.NODEMAP[elementName] || 'div';
    var parentElement = document.createElement(parentTag);
    try { // prevent IE "feature": http://dev.rubyonrails.org/ticket/2707
      parentElement.innerHTML = "<" + elementName + "></" + elementName + ">";
    } catch (e) {
    }
    var element = parentElement.firstChild || null;

    // see if browser added wrapping tags
    if (element && (element.tagName.toUpperCase() != elementName))
      element = element.getElementsByTagName(elementName)[0];

    // fallback to createElement approach
    if (!element) element = document.createElement(elementName);

    // abort if nothing could be created
    if (!element) return;

    // attributes (or text)
    if (arguments[1])
      if (this._isStringOrNumber(arguments[1]) ||
        (arguments[1] instanceof Array) ||
        arguments[1].tagName) {
        this._children(element, arguments[1]);
      } else {
        var attrs = this._attributes(arguments[1]);
        if (attrs.length) {
          try { // prevent IE "feature": http://dev.rubyonrails.org/ticket/2707
            parentElement.innerHTML = "<" + elementName + " " +
              attrs + "></" + elementName + ">";
          } catch (e) {
          }
          element = parentElement.firstChild || null;
          // workaround firefox 1.0.X bug
          if (!element) {
            element = document.createElement(elementName);
            for (attr in arguments[1])
              element[attr == 'class' ? 'className' : attr] = arguments[1][attr];
          }
          if (element.tagName.toUpperCase() != elementName)
            element = parentElement.getElementsByTagName(elementName)[0];
        }
      }

    // text, or array of children
    if (arguments[2])
      this._children(element, arguments[2]);

    return $(element);
  },
  _text:function (text) {
    return document.createTextNode(text);
  },

  ATTR_MAP:{
    'className':'class',
    'htmlFor':'for'
  },

  _attributes:function (attributes) {
    var attrs = [];
    for (attribute in attributes)
      attrs.push((attribute in this.ATTR_MAP ? this.ATTR_MAP[attribute] : attribute) +
        '="' + attributes[attribute].toString().escapeHTML().gsub(/"/, '&quot;') + '"');
    return attrs.join(" ");
  },
  _children:function (element, children) {
    if (children.tagName) {
      element.appendChild(children);
      return;
    }
    if (typeof children == 'object') { // array can hold nodes and text
      children.flatten().each(function (e) {
        if (typeof e == 'object')
          element.appendChild(e);
        else
        if (Builder._isStringOrNumber(e))
          element.appendChild(Builder._text(e));
      });
    } else
    if (Builder._isStringOrNumber(children))
      element.appendChild(Builder._text(children));
  },
  _isStringOrNumber:function (param) {
    return(typeof param == 'string' || typeof param == 'number');
  },
  build:function (html) {
    var element = this.node('div');
    $(element).update(html.strip());
    return element.down();
  },
  dump:function (scope) {
    if (typeof scope != 'object' && typeof scope != 'function') scope = window; //global scope

    var tags = ("A ABBR ACRONYM ADDRESS APPLET AREA B BASE BASEFONT BDO BIG BLOCKQUOTE BODY " +
      "BR BUTTON CAPTION CENTER CITE CODE COL COLGROUP DD DEL DFN DIR DIV DL DT EM FIELDSET " +
      "FONT FORM FRAME FRAMESET H1 H2 H3 H4 H5 H6 HEAD HR HTML I IFRAME IMG INPUT INS ISINDEX " +
      "KBD LABEL LEGEND LI LINK MAP MENU META NOFRAMES NOSCRIPT OBJECT OL OPTGROUP OPTION P " +
      "PARAM PRE Q S SAMP SCRIPT SELECT SMALL SPAN STRIKE STRONG STYLE SUB SUP TABLE TBODY TD " +
      "TEXTAREA TFOOT TH THEAD TITLE TR TT U UL VAR").split(/\s+/);

    tags.each(function (tag) {
      scope[tag] = function () {
        return Builder.node.apply(Builder, [tag].concat($A(arguments)));
      };
    });
  }
};
// script.aculo.us effects.js v1.9.0, Thu Dec 23 16:54:48 -0500 2010

// Copyright (c) 2005-2010 Thomas Fuchs (http://script.aculo.us, http://mir.aculo.us)
// Contributors:
//  Justin Palmer (http://encytemedia.com/)
//  Mark Pilgrim (http://diveintomark.org/)
//  Martin Bialasinki
//
// script.aculo.us is freely distributable under the terms of an MIT-style license.
// For details, see the script.aculo.us web site: http://script.aculo.us/

// converts rgb() and #xxx to #xxxxxx format,
// returns self (or first argument) if not convertable
String.prototype.parseColor = function () {
  var color = '#';
  if (this.slice(0, 4) == 'rgb(') {
    var cols = this.slice(4, this.length - 1).split(',');
    var i = 0;
    do {
      color += parseInt(cols[i]).toColorPart()
    } while (++i < 3);
  } else {
    if (this.slice(0, 1) == '#') {
      if (this.length == 4) for (var i = 1; i < 4; i++) color += (this.charAt(i) + this.charAt(i)).toLowerCase();
      if (this.length == 7) color = this.toLowerCase();
    }
  }
  return (color.length == 7 ? color : (arguments[0] || this));
};

/*--------------------------------------------------------------------------*/

Element.collectTextNodes = function (element) {
  return $A($(element).childNodes).collect(
    function (node) {
      return (node.nodeType == 3 ? node.nodeValue :
        (node.hasChildNodes() ? Element.collectTextNodes(node) : ''));
    }).flatten().join('');
};

Element.collectTextNodesIgnoreClass = function (element, className) {
  return $A($(element).childNodes).collect(
    function (node) {
      return (node.nodeType == 3 ? node.nodeValue :
        ((node.hasChildNodes() && !Element.hasClassName(node, className)) ?
          Element.collectTextNodesIgnoreClass(node, className) : ''));
    }).flatten().join('');
};

Element.setContentZoom = function (element, percent) {
  element = $(element);
  element.setStyle({fontSize:(percent / 100) + 'em'});
  if (Prototype.Browser.WebKit) window.scrollBy(0, 0);
  return element;
};

Element.getInlineOpacity = function (element) {
  return $(element).style.opacity || '';
};

Element.forceRerendering = function (element) {
  try {
    element = $(element);
    var n = document.createTextNode(' ');
    element.appendChild(n);
    element.removeChild(n);
  } catch (e) {
  }
};

/*--------------------------------------------------------------------------*/

var Effect = {
  _elementDoesNotExistError:{
    name:'ElementDoesNotExistError',
    message:'The specified DOM element does not exist, but is required for this effect to operate'
  },
  Transitions:{
    linear:Prototype.K,
    sinoidal:function (pos) {
      return (-Math.cos(pos * Math.PI) / 2) + .5;
    },
    reverse:function (pos) {
      return 1 - pos;
    },
    flicker:function (pos) {
      var pos = ((-Math.cos(pos * Math.PI) / 4) + .75) + Math.random() / 4;
      return pos > 1 ? 1 : pos;
    },
    wobble:function (pos) {
      return (-Math.cos(pos * Math.PI * (9 * pos)) / 2) + .5;
    },
    pulse:function (pos, pulses) {
      return (-Math.cos((pos * ((pulses || 5) - .5) * 2) * Math.PI) / 2) + .5;
    },
    spring:function (pos) {
      return 1 - (Math.cos(pos * 4.5 * Math.PI) * Math.exp(-pos * 6));
    },
    none:function (pos) {
      return 0;
    },
    full:function (pos) {
      return 1;
    }
  },
  DefaultOptions:{
    duration:1.0, // seconds
    fps:100, // 100= assume 66fps max.
    sync:false, // true for combining
    from:0.0,
    to:1.0,
    delay:0.0,
    queue:'parallel'
  },
  tagifyText:function (element) {
    var tagifyStyle = 'position:relative';
    if (Prototype.Browser.IE) tagifyStyle += ';zoom:1';

    element = $(element);
    $A(element.childNodes).each(function (child) {
      if (child.nodeType == 3) {
        child.nodeValue.toArray().each(function (character) {
          element.insertBefore(
            new Element('span', {style:tagifyStyle}).update(
              character == ' ' ? String.fromCharCode(160) : character),
            child);
        });
        Element.remove(child);
      }
    });
  },
  multiple:function (element, effect) {
    var elements;
    if (((typeof element == 'object') ||
      Object.isFunction(element)) &&
      (element.length))
      elements = element;
    else
      elements = $(element).childNodes;

    var options = Object.extend({
      speed:0.1,
      delay:0.0
    }, arguments[2] || { });
    var masterDelay = options.delay;

    $A(elements).each(function (element, index) {
      new effect(element, Object.extend(options, { delay:index * options.speed + masterDelay }));
    });
  },
  PAIRS:{
    'slide':['SlideDown', 'SlideUp'],
    'blind':['BlindDown', 'BlindUp'],
    'appear':['Appear', 'Fade']
  },
  toggle:function (element, effect, options) {
    element = $(element);
    effect = (effect || 'appear').toLowerCase();

    return Effect[ Effect.PAIRS[ effect ][ element.visible() ? 1 : 0 ] ](element, Object.extend({
      queue:{ position:'end', scope:(element.id || 'global'), limit:1 }
    }, options || {}));
  }
};

Effect.DefaultOptions.transition = Effect.Transitions.sinoidal;

/* ------------- core effects ------------- */

Effect.ScopedQueue = Class.create(Enumerable, {
  initialize:function () {
    this.effects = [];
    this.interval = null;
  },
  _each:function (iterator) {
    this.effects._each(iterator);
  },
  add:function (effect) {
    var timestamp = new Date().getTime();

    var position = Object.isString(effect.options.queue) ?
      effect.options.queue : effect.options.queue.position;

    switch (position) {
      case 'front':
        // move unstarted effects after this effect
        this.effects.findAll(
          function (e) {
            return e.state == 'idle'
          }).each(function (e) {
          e.startOn += effect.finishOn;
          e.finishOn += effect.finishOn;
        });
        break;
      case 'with-last':
        timestamp = this.effects.pluck('startOn').max() || timestamp;
        break;
      case 'end':
        // start effect after last queued effect has finished
        timestamp = this.effects.pluck('finishOn').max() || timestamp;
        break;
    }

    effect.startOn += timestamp;
    effect.finishOn += timestamp;

    if (!effect.options.queue.limit || (this.effects.length < effect.options.queue.limit))
      this.effects.push(effect);

    if (!this.interval)
      this.interval = setInterval(this.loop.bind(this), 15);
  },
  remove:function (effect) {
    this.effects = this.effects.reject(function (e) {
      return e == effect
    });
    if (this.effects.length == 0) {
      clearInterval(this.interval);
      this.interval = null;
    }
  },
  loop:function () {
    var timePos = new Date().getTime();
    for (var i = 0, len = this.effects.length; i < len; i++)
      this.effects[i] && this.effects[i].loop(timePos);
  }
});

Effect.Queues = {
  instances:$H(),
  get:function (queueName) {
    if (!Object.isString(queueName)) return queueName;

    return this.instances.get(queueName) ||
      this.instances.set(queueName, new Effect.ScopedQueue());
  }
};
Effect.Queue = Effect.Queues.get('global');

Effect.Base = Class.create({
  position:null,
  start:function (options) {
    if (options && options.transition === false) options.transition = Effect.Transitions.linear;
    this.options = Object.extend(Object.extend({ }, Effect.DefaultOptions), options || { });
    this.currentFrame = 0;
    this.state = 'idle';
    this.startOn = this.options.delay * 1000;
    this.finishOn = this.startOn + (this.options.duration * 1000);
    this.fromToDelta = this.options.to - this.options.from;
    this.totalTime = this.finishOn - this.startOn;
    this.totalFrames = this.options.fps * this.options.duration;

    this.render = (function () {
      function dispatch(effect, eventName) {
        if (effect.options[eventName + 'Internal'])
          effect.options[eventName + 'Internal'](effect);
        if (effect.options[eventName])
          effect.options[eventName](effect);
      }

      return function (pos) {
        if (this.state === "idle") {
          this.state = "running";
          dispatch(this, 'beforeSetup');
          if (this.setup) this.setup();
          dispatch(this, 'afterSetup');
        }
        if (this.state === "running") {
          pos = (this.options.transition(pos) * this.fromToDelta) + this.options.from;
          this.position = pos;
          dispatch(this, 'beforeUpdate');
          if (this.update) this.update(pos);
          dispatch(this, 'afterUpdate');
        }
      };
    })();

    this.event('beforeStart');
    if (!this.options.sync)
      Effect.Queues.get(Object.isString(this.options.queue) ?
        'global' : this.options.queue.scope).add(this);
  },
  loop:function (timePos) {
    if (timePos >= this.startOn) {
      if (timePos >= this.finishOn) {
        this.render(1.0);
        this.cancel();
        this.event('beforeFinish');
        if (this.finish) this.finish();
        this.event('afterFinish');
        return;
      }
      var pos = (timePos - this.startOn) / this.totalTime,
        frame = (pos * this.totalFrames).round();
      if (frame > this.currentFrame) {
        this.render(pos);
        this.currentFrame = frame;
      }
    }
  },
  cancel:function () {
    if (!this.options.sync)
      Effect.Queues.get(Object.isString(this.options.queue) ?
        'global' : this.options.queue.scope).remove(this);
    this.state = 'finished';
  },
  event:function (eventName) {
    if (this.options[eventName + 'Internal']) this.options[eventName + 'Internal'](this);
    if (this.options[eventName]) this.options[eventName](this);
  },
  inspect:function () {
    var data = $H();
    for (property in this)
      if (!Object.isFunction(this[property])) data.set(property, this[property]);
    return '#<Effect:' + data.inspect() + ',options:' + $H(this.options).inspect() + '>';
  }
});

Effect.Parallel = Class.create(Effect.Base, {
  initialize:function (effects) {
    this.effects = effects || [];
    this.start(arguments[1]);
  },
  update:function (position) {
    this.effects.invoke('render', position);
  },
  finish:function (position) {
    this.effects.each(function (effect) {
      effect.render(1.0);
      effect.cancel();
      effect.event('beforeFinish');
      if (effect.finish) effect.finish(position);
      effect.event('afterFinish');
    });
  }
});

Effect.Tween = Class.create(Effect.Base, {
  initialize:function (object, from, to) {
    object = Object.isString(object) ? $(object) : object;
    var args = $A(arguments), method = args.last(),
      options = args.length == 5 ? args[3] : null;
    this.method = Object.isFunction(method) ? method.bind(object) :
      Object.isFunction(object[method]) ? object[method].bind(object) :
        function (value) {
          object[method] = value
        };
    this.start(Object.extend({ from:from, to:to }, options || { }));
  },
  update:function (position) {
    this.method(position);
  }
});

Effect.Event = Class.create(Effect.Base, {
  initialize:function () {
    this.start(Object.extend({ duration:0 }, arguments[0] || { }));
  },
  update:Prototype.emptyFunction
});

Effect.Opacity = Class.create(Effect.Base, {
  initialize:function (element) {
    this.element = $(element);
    if (!this.element) throw(Effect._elementDoesNotExistError);
    // make this work on IE on elements without 'layout'
    if (Prototype.Browser.IE && (!this.element.currentStyle.hasLayout))
      this.element.setStyle({zoom:1});
    var options = Object.extend({
      from:this.element.getOpacity() || 0.0,
      to:1.0
    }, arguments[1] || { });
    this.start(options);
  },
  update:function (position) {
    this.element.setOpacity(position);
  }
});

Effect.Move = Class.create(Effect.Base, {
  initialize:function (element) {
    this.element = $(element);
    if (!this.element) throw(Effect._elementDoesNotExistError);
    var options = Object.extend({
      x:0,
      y:0,
      mode:'relative'
    }, arguments[1] || { });
    this.start(options);
  },
  setup:function () {
    this.element.makePositioned();
    this.originalLeft = parseFloat(this.element.getStyle('left') || '0');
    this.originalTop = parseFloat(this.element.getStyle('top') || '0');
    if (this.options.mode == 'absolute') {
      this.options.x = this.options.x - this.originalLeft;
      this.options.y = this.options.y - this.originalTop;
    }
  },
  update:function (position) {
    this.element.setStyle({
      left:(this.options.x * position + this.originalLeft).round() + 'px',
      top:(this.options.y * position + this.originalTop).round() + 'px'
    });
  }
});

// for backwards compatibility
Effect.MoveBy = function (element, toTop, toLeft) {
  return new Effect.Move(element,
    Object.extend({ x:toLeft, y:toTop }, arguments[3] || { }));
};

Effect.Scale = Class.create(Effect.Base, {
  initialize:function (element, percent) {
    this.element = $(element);
    if (!this.element) throw(Effect._elementDoesNotExistError);
    var options = Object.extend({
      scaleX:true,
      scaleY:true,
      scaleContent:true,
      scaleFromCenter:false,
      scaleMode:'box', // 'box' or 'contents' or { } with provided values
      scaleFrom:100.0,
      scaleTo:percent
    }, arguments[2] || { });
    this.start(options);
  },
  setup:function () {
    this.restoreAfterFinish = this.options.restoreAfterFinish || false;
    this.elementPositioning = this.element.getStyle('position');

    this.originalStyle = { };
    ['top', 'left', 'width', 'height', 'fontSize'].each(function (k) {
      this.originalStyle[k] = this.element.style[k];
    }.bind(this));

    this.originalTop = this.element.offsetTop;
    this.originalLeft = this.element.offsetLeft;

    var fontSize = this.element.getStyle('font-size') || '100%';
    ['em', 'px', '%', 'pt'].each(function (fontSizeType) {
      if (fontSize.indexOf(fontSizeType) > 0) {
        this.fontSize = parseFloat(fontSize);
        this.fontSizeType = fontSizeType;
      }
    }.bind(this));

    this.factor = (this.options.scaleTo - this.options.scaleFrom) / 100;

    this.dims = null;
    if (this.options.scaleMode == 'box')
      this.dims = [this.element.offsetHeight, this.element.offsetWidth];
    if (/^content/.test(this.options.scaleMode))
      this.dims = [this.element.scrollHeight, this.element.scrollWidth];
    if (!this.dims)
      this.dims = [this.options.scaleMode.originalHeight,
        this.options.scaleMode.originalWidth];
  },
  update:function (position) {
    var currentScale = (this.options.scaleFrom / 100.0) + (this.factor * position);
    if (this.options.scaleContent && this.fontSize)
      this.element.setStyle({fontSize:this.fontSize * currentScale + this.fontSizeType });
    this.setDimensions(this.dims[0] * currentScale, this.dims[1] * currentScale);
  },
  finish:function (position) {
    if (this.restoreAfterFinish) this.element.setStyle(this.originalStyle);
  },
  setDimensions:function (height, width) {
    var d = { };
    if (this.options.scaleX) d.width = width.round() + 'px';
    if (this.options.scaleY) d.height = height.round() + 'px';
    if (this.options.scaleFromCenter) {
      var topd = (height - this.dims[0]) / 2;
      var leftd = (width - this.dims[1]) / 2;
      if (this.elementPositioning == 'absolute') {
        if (this.options.scaleY) d.top = this.originalTop - topd + 'px';
        if (this.options.scaleX) d.left = this.originalLeft - leftd + 'px';
      } else {
        if (this.options.scaleY) d.top = -topd + 'px';
        if (this.options.scaleX) d.left = -leftd + 'px';
      }
    }
    this.element.setStyle(d);
  }
});

Effect.Highlight = Class.create(Effect.Base, {
  initialize:function (element) {
    this.element = $(element);
    if (!this.element) throw(Effect._elementDoesNotExistError);
    var options = Object.extend({ startcolor:'#ffff99' }, arguments[1] || { });
    this.start(options);
  },
  setup:function () {
    // Prevent executing on elements not in the layout flow
    if (this.element.getStyle('display') == 'none') {
      this.cancel();
      return;
    }
    // Disable background image during the effect
    this.oldStyle = { };
    if (!this.options.keepBackgroundImage) {
      this.oldStyle.backgroundImage = this.element.getStyle('background-image');
      this.element.setStyle({backgroundImage:'none'});
    }
    if (!this.options.endcolor)
      this.options.endcolor = this.element.getStyle('background-color').parseColor('#ffffff');
    if (!this.options.restorecolor)
      this.options.restorecolor = this.element.getStyle('background-color');
    // init color calculations
    this._base = $R(0, 2).map(function (i) {
      return parseInt(this.options.startcolor.slice(i * 2 + 1, i * 2 + 3), 16)
    }.bind(this));
    this._delta = $R(0, 2).map(function (i) {
      return parseInt(this.options.endcolor.slice(i * 2 + 1, i * 2 + 3), 16) - this._base[i]
    }.bind(this));
  },
  update:function (position) {
    this.element.setStyle({backgroundColor:$R(0, 2).inject('#', function (m, v, i) {
      return m + ((this._base[i] + (this._delta[i] * position)).round().toColorPart());
    }.bind(this)) });
  },
  finish:function () {
    this.element.setStyle(Object.extend(this.oldStyle, {
      backgroundColor:this.options.restorecolor
    }));
  }
});

Effect.ScrollTo = function (element) {
  var options = arguments[1] || { },
    scrollOffsets = document.viewport.getScrollOffsets(),
    elementOffsets = $(element).cumulativeOffset();

  if (options.offset) elementOffsets[1] += options.offset;

  return new Effect.Tween(null,
    scrollOffsets.top,
    elementOffsets[1],
    options,
    function (p) {
      scrollTo(scrollOffsets.left, p.round());
    }
  );
};

/* ------------- combination effects ------------- */

Effect.Fade = function (element) {
  element = $(element);
  var oldOpacity = element.getInlineOpacity();
  var options = Object.extend({
    from:element.getOpacity() || 1.0,
    to:0.0,
    afterFinishInternal:function (effect) {
      if (effect.options.to != 0) return;
      effect.element.hide().setStyle({opacity:oldOpacity});
    }
  }, arguments[1] || { });
  return new Effect.Opacity(element, options);
};

Effect.Appear = function (element) {
  element = $(element);
  var options = Object.extend({
    from:(element.getStyle('display') == 'none' ? 0.0 : element.getOpacity() || 0.0),
    to:1.0,
    // force Safari to render floated elements properly
    afterFinishInternal:function (effect) {
      effect.element.forceRerendering();
    },
    beforeSetup:function (effect) {
      effect.element.setOpacity(effect.options.from).show();
    }}, arguments[1] || { });
  return new Effect.Opacity(element, options);
};

Effect.Puff = function (element) {
  element = $(element);
  var oldStyle = {
    opacity:element.getInlineOpacity(),
    position:element.getStyle('position'),
    top:element.style.top,
    left:element.style.left,
    width:element.style.width,
    height:element.style.height
  };
  return new Effect.Parallel(
    [ new Effect.Scale(element, 200,
      { sync:true, scaleFromCenter:true, scaleContent:true, restoreAfterFinish:true }),
      new Effect.Opacity(element, { sync:true, to:0.0 }) ],
    Object.extend({ duration:1.0,
      beforeSetupInternal:function (effect) {
        Position.absolutize(effect.effects[0].element);
      },
      afterFinishInternal:function (effect) {
        effect.effects[0].element.hide().setStyle(oldStyle);
      }
    }, arguments[1] || { })
  );
};

Effect.BlindUp = function (element) {
  element = $(element);
  element.makeClipping();
  return new Effect.Scale(element, 0,
    Object.extend({ scaleContent:false,
      scaleX:false,
      restoreAfterFinish:true,
      afterFinishInternal:function (effect) {
        effect.element.hide().undoClipping();
      }
    }, arguments[1] || { })
  );
};

Effect.BlindDown = function (element) {
  element = $(element);
  var elementDimensions = element.getDimensions();
  return new Effect.Scale(element, 100, Object.extend({
    scaleContent:false,
    scaleX:false,
    scaleFrom:0,
    scaleMode:{originalHeight:elementDimensions.height, originalWidth:elementDimensions.width},
    restoreAfterFinish:true,
    afterSetup:function (effect) {
      effect.element.makeClipping().setStyle({height:'0px'}).show();
    },
    afterFinishInternal:function (effect) {
      effect.element.undoClipping();
    }
  }, arguments[1] || { }));
};

Effect.SwitchOff = function (element) {
  element = $(element);
  var oldOpacity = element.getInlineOpacity();
  return new Effect.Appear(element, Object.extend({
    duration:0.4,
    from:0,
    transition:Effect.Transitions.flicker,
    afterFinishInternal:function (effect) {
      new Effect.Scale(effect.element, 1, {
        duration:0.3, scaleFromCenter:true,
        scaleX:false, scaleContent:false, restoreAfterFinish:true,
        beforeSetup:function (effect) {
          effect.element.makePositioned().makeClipping();
        },
        afterFinishInternal:function (effect) {
          effect.element.hide().undoClipping().undoPositioned().setStyle({opacity:oldOpacity});
        }
      });
    }
  }, arguments[1] || { }));
};

Effect.DropOut = function (element) {
  element = $(element);
  var oldStyle = {
    top:element.getStyle('top'),
    left:element.getStyle('left'),
    opacity:element.getInlineOpacity() };
  return new Effect.Parallel(
    [ new Effect.Move(element, {x:0, y:100, sync:true }),
      new Effect.Opacity(element, { sync:true, to:0.0 }) ],
    Object.extend(
      { duration:0.5,
        beforeSetup:function (effect) {
          effect.effects[0].element.makePositioned();
        },
        afterFinishInternal:function (effect) {
          effect.effects[0].element.hide().undoPositioned().setStyle(oldStyle);
        }
      }, arguments[1] || { }));
};

Effect.Shake = function (element) {
  element = $(element);
  var options = Object.extend({
    distance:20,
    duration:0.5
  }, arguments[1] || {});
  var distance = parseFloat(options.distance);
  var split = parseFloat(options.duration) / 10.0;
  var oldStyle = {
    top:element.getStyle('top'),
    left:element.getStyle('left') };
  return new Effect.Move(element,
    { x:distance, y:0, duration:split, afterFinishInternal:function (effect) {
      new Effect.Move(effect.element,
        { x:-distance * 2, y:0, duration:split * 2, afterFinishInternal:function (effect) {
          new Effect.Move(effect.element,
            { x:distance * 2, y:0, duration:split * 2, afterFinishInternal:function (effect) {
              new Effect.Move(effect.element,
                { x:-distance * 2, y:0, duration:split * 2, afterFinishInternal:function (effect) {
                  new Effect.Move(effect.element,
                    { x:distance * 2, y:0, duration:split * 2, afterFinishInternal:function (effect) {
                      new Effect.Move(effect.element,
                        { x:-distance, y:0, duration:split, afterFinishInternal:function (effect) {
                          effect.element.undoPositioned().setStyle(oldStyle);
                        }});
                    }});
                }});
            }});
        }});
    }});
};

Effect.SlideDown = function (element) {
  element = $(element).cleanWhitespace();
  // SlideDown need to have the content of the element wrapped in a container element with fixed height!
  var oldInnerBottom = element.down().getStyle('bottom');
  var elementDimensions = element.getDimensions();
  return new Effect.Scale(element, 100, Object.extend({
    scaleContent:false,
    scaleX:false,
    scaleFrom:window.opera ? 0 : 1,
    scaleMode:{originalHeight:elementDimensions.height, originalWidth:elementDimensions.width},
    restoreAfterFinish:true,
    afterSetup:function (effect) {
      effect.element.makePositioned();
      effect.element.down().makePositioned();
      if (window.opera) effect.element.setStyle({top:''});
      effect.element.makeClipping().setStyle({height:'0px'}).show();
    },
    afterUpdateInternal:function (effect) {
      effect.element.down().setStyle({bottom:(effect.dims[0] - effect.element.clientHeight) + 'px' });
    },
    afterFinishInternal:function (effect) {
      effect.element.undoClipping().undoPositioned();
      effect.element.down().undoPositioned().setStyle({bottom:oldInnerBottom});
    }
  }, arguments[1] || { })
  );
};

Effect.SlideUp = function (element) {
  element = $(element).cleanWhitespace();
  var oldInnerBottom = element.down().getStyle('bottom');
  var elementDimensions = element.getDimensions();
  return new Effect.Scale(element, window.opera ? 0 : 1,
    Object.extend({ scaleContent:false,
      scaleX:false,
      scaleMode:'box',
      scaleFrom:100,
      scaleMode:{originalHeight:elementDimensions.height, originalWidth:elementDimensions.width},
      restoreAfterFinish:true,
      afterSetup:function (effect) {
        effect.element.makePositioned();
        effect.element.down().makePositioned();
        if (window.opera) effect.element.setStyle({top:''});
        effect.element.makeClipping().show();
      },
      afterUpdateInternal:function (effect) {
        effect.element.down().setStyle({bottom:(effect.dims[0] - effect.element.clientHeight) + 'px' });
      },
      afterFinishInternal:function (effect) {
        effect.element.hide().undoClipping().undoPositioned();
        effect.element.down().undoPositioned().setStyle({bottom:oldInnerBottom});
      }
    }, arguments[1] || { })
  );
};

// Bug in opera makes the TD containing this element expand for a instance after finish
Effect.Squish = function (element) {
  return new Effect.Scale(element, window.opera ? 1 : 0, {
    restoreAfterFinish:true,
    beforeSetup:function (effect) {
      effect.element.makeClipping();
    },
    afterFinishInternal:function (effect) {
      effect.element.hide().undoClipping();
    }
  });
};

Effect.Grow = function (element) {
  element = $(element);
  var options = Object.extend({
    direction:'center',
    moveTransition:Effect.Transitions.sinoidal,
    scaleTransition:Effect.Transitions.sinoidal,
    opacityTransition:Effect.Transitions.full
  }, arguments[1] || { });
  var oldStyle = {
    top:element.style.top,
    left:element.style.left,
    height:element.style.height,
    width:element.style.width,
    opacity:element.getInlineOpacity() };

  var dims = element.getDimensions();
  var initialMoveX, initialMoveY;
  var moveX, moveY;

  switch (options.direction) {
    case 'top-left':
      initialMoveX = initialMoveY = moveX = moveY = 0;
      break;
    case 'top-right':
      initialMoveX = dims.width;
      initialMoveY = moveY = 0;
      moveX = -dims.width;
      break;
    case 'bottom-left':
      initialMoveX = moveX = 0;
      initialMoveY = dims.height;
      moveY = -dims.height;
      break;
    case 'bottom-right':
      initialMoveX = dims.width;
      initialMoveY = dims.height;
      moveX = -dims.width;
      moveY = -dims.height;
      break;
    case 'center':
      initialMoveX = dims.width / 2;
      initialMoveY = dims.height / 2;
      moveX = -dims.width / 2;
      moveY = -dims.height / 2;
      break;
  }

  return new Effect.Move(element, {
    x:initialMoveX,
    y:initialMoveY,
    duration:0.01,
    beforeSetup:function (effect) {
      effect.element.hide().makeClipping().makePositioned();
    },
    afterFinishInternal:function (effect) {
      new Effect.Parallel(
        [ new Effect.Opacity(effect.element, { sync:true, to:1.0, from:0.0, transition:options.opacityTransition }),
          new Effect.Move(effect.element, { x:moveX, y:moveY, sync:true, transition:options.moveTransition }),
          new Effect.Scale(effect.element, 100, {
            scaleMode:{ originalHeight:dims.height, originalWidth:dims.width },
            sync:true, scaleFrom:window.opera ? 1 : 0, transition:options.scaleTransition, restoreAfterFinish:true})
        ], Object.extend({
          beforeSetup:function (effect) {
            effect.effects[0].element.setStyle({height:'0px'}).show();
          },
          afterFinishInternal:function (effect) {
            effect.effects[0].element.undoClipping().undoPositioned().setStyle(oldStyle);
          }
        }, options)
      );
    }
  });
};

Effect.Shrink = function (element) {
  element = $(element);
  var options = Object.extend({
    direction:'center',
    moveTransition:Effect.Transitions.sinoidal,
    scaleTransition:Effect.Transitions.sinoidal,
    opacityTransition:Effect.Transitions.none
  }, arguments[1] || { });
  var oldStyle = {
    top:element.style.top,
    left:element.style.left,
    height:element.style.height,
    width:element.style.width,
    opacity:element.getInlineOpacity() };

  var dims = element.getDimensions();
  var moveX, moveY;

  switch (options.direction) {
    case 'top-left':
      moveX = moveY = 0;
      break;
    case 'top-right':
      moveX = dims.width;
      moveY = 0;
      break;
    case 'bottom-left':
      moveX = 0;
      moveY = dims.height;
      break;
    case 'bottom-right':
      moveX = dims.width;
      moveY = dims.height;
      break;
    case 'center':
      moveX = dims.width / 2;
      moveY = dims.height / 2;
      break;
  }

  return new Effect.Parallel(
    [ new Effect.Opacity(element, { sync:true, to:0.0, from:1.0, transition:options.opacityTransition }),
      new Effect.Scale(element, window.opera ? 1 : 0, { sync:true, transition:options.scaleTransition, restoreAfterFinish:true}),
      new Effect.Move(element, { x:moveX, y:moveY, sync:true, transition:options.moveTransition })
    ], Object.extend({
      beforeStartInternal:function (effect) {
        effect.effects[0].element.makePositioned().makeClipping();
      },
      afterFinishInternal:function (effect) {
        effect.effects[0].element.hide().undoClipping().undoPositioned().setStyle(oldStyle);
      }
    }, options)
  );
};

Effect.Pulsate = function (element) {
  element = $(element);
  var options = arguments[1] || { },
    oldOpacity = element.getInlineOpacity(),
    transition = options.transition || Effect.Transitions.linear,
    reverser = function (pos) {
      return 1 - transition((-Math.cos((pos * (options.pulses || 5) * 2) * Math.PI) / 2) + .5);
    };

  return new Effect.Opacity(element,
    Object.extend(Object.extend({  duration:2.0, from:0,
      afterFinishInternal:function (effect) {
        effect.element.setStyle({opacity:oldOpacity});
      }
    }, options), {transition:reverser}));
};

Effect.Fold = function (element) {
  element = $(element);
  var oldStyle = {
    top:element.style.top,
    left:element.style.left,
    width:element.style.width,
    height:element.style.height };
  element.makeClipping();
  return new Effect.Scale(element, 5, Object.extend({
    scaleContent:false,
    scaleX:false,
    afterFinishInternal:function (effect) {
      new Effect.Scale(element, 1, {
        scaleContent:false,
        scaleY:false,
        afterFinishInternal:function (effect) {
          effect.element.hide().undoClipping().setStyle(oldStyle);
        } });
    }}, arguments[1] || { }));
};

Effect.Morph = Class.create(Effect.Base, {
  initialize:function (element) {
    this.element = $(element);
    if (!this.element) throw(Effect._elementDoesNotExistError);
    var options = Object.extend({
      style:{ }
    }, arguments[1] || { });

    if (!Object.isString(options.style)) this.style = $H(options.style);
    else {
      if (options.style.include(':'))
        this.style = options.style.parseStyle();
      else {
        this.element.addClassName(options.style);
        this.style = $H(this.element.getStyles());
        this.element.removeClassName(options.style);
        var css = this.element.getStyles();
        this.style = this.style.reject(function (style) {
          return style.value == css[style.key];
        });
        options.afterFinishInternal = function (effect) {
          effect.element.addClassName(effect.options.style);
          effect.transforms.each(function (transform) {
            effect.element.style[transform.style] = '';
          });
        };
      }
    }
    this.start(options);
  },

  setup:function () {
    function parseColor(color) {
      if (!color || ['rgba(0, 0, 0, 0)', 'transparent'].include(color)) color = '#ffffff';
      color = color.parseColor();
      return $R(0, 2).map(function (i) {
        return parseInt(color.slice(i * 2 + 1, i * 2 + 3), 16);
      });
    }

    this.transforms = this.style.map(function (pair) {
      var property = pair[0], value = pair[1], unit = null;

      if (value.parseColor('#zzzzzz') != '#zzzzzz') {
        value = value.parseColor();
        unit = 'color';
      } else if (property == 'opacity') {
        value = parseFloat(value);
        if (Prototype.Browser.IE && (!this.element.currentStyle.hasLayout))
          this.element.setStyle({zoom:1});
      } else if (Element.CSS_LENGTH.test(value)) {
        var components = value.match(/^([\+\-]?[0-9\.]+)(.*)$/);
        value = parseFloat(components[1]);
        unit = (components.length == 3) ? components[2] : null;
      }

      var originalValue = this.element.getStyle(property);
      return {
        style:property.camelize(),
        originalValue:unit == 'color' ? parseColor(originalValue) : parseFloat(originalValue || 0),
        targetValue:unit == 'color' ? parseColor(value) : value,
        unit:unit
      };
    }.bind(this)).reject(function (transform) {
        return (
          (transform.originalValue == transform.targetValue) ||
            (
              transform.unit != 'color' &&
                (isNaN(transform.originalValue) || isNaN(transform.targetValue))
              )
          );
      });
  },
  update:function (position) {
    var style = { }, transform, i = this.transforms.length;
    while (i--)
      style[(transform = this.transforms[i]).style] =
        transform.unit == 'color' ? '#' +
          (Math.round(transform.originalValue[0] +
            (transform.targetValue[0] - transform.originalValue[0]) * position)).toColorPart() +
          (Math.round(transform.originalValue[1] +
            (transform.targetValue[1] - transform.originalValue[1]) * position)).toColorPart() +
          (Math.round(transform.originalValue[2] +
            (transform.targetValue[2] - transform.originalValue[2]) * position)).toColorPart() :
          (transform.originalValue +
            (transform.targetValue - transform.originalValue) * position).toFixed(3) +
            (transform.unit === null ? '' : transform.unit);
    this.element.setStyle(style, true);
  }
});

Effect.Transform = Class.create({
  initialize:function (tracks) {
    this.tracks = [];
    this.options = arguments[1] || { };
    this.addTracks(tracks);
  },
  addTracks:function (tracks) {
    tracks.each(function (track) {
      track = $H(track);
      var data = track.values().first();
      this.tracks.push($H({
        ids:track.keys().first(),
        effect:Effect.Morph,
        options:{ style:data }
      }));
    }.bind(this));
    return this;
  },
  play:function () {
    return new Effect.Parallel(
      this.tracks.map(
        function (track) {
          var ids = track.get('ids'), effect = track.get('effect'), options = track.get('options');
          var elements = [$(ids) || $$(ids)].flatten();
          return elements.map(function (e) {
            return new effect(e, Object.extend({ sync:true }, options))
          });
        }).flatten(),
      this.options
    );
  }
});

Element.CSS_PROPERTIES = $w(
  'backgroundColor backgroundPosition borderBottomColor borderBottomStyle ' +
    'borderBottomWidth borderLeftColor borderLeftStyle borderLeftWidth ' +
    'borderRightColor borderRightStyle borderRightWidth borderSpacing ' +
    'borderTopColor borderTopStyle borderTopWidth bottom clip color ' +
    'fontSize fontWeight height left letterSpacing lineHeight ' +
    'marginBottom marginLeft marginRight marginTop markerOffset maxHeight ' +
    'maxWidth minHeight minWidth opacity outlineColor outlineOffset ' +
    'outlineWidth paddingBottom paddingLeft paddingRight paddingTop ' +
    'right textIndent top width wordSpacing zIndex');

Element.CSS_LENGTH = /^(([\+\-]?[0-9\.]+)(em|ex|px|in|cm|mm|pt|pc|\%))|0$/;

String.__parseStyleElement = document.createElement('div');
String.prototype.parseStyle = function () {
  var style, styleRules = $H();
  if (Prototype.Browser.WebKit)
    style = new Element('div', {style:this}).style;
  else {
    String.__parseStyleElement.innerHTML = '<div style="' + this + '"></div>';
    style = String.__parseStyleElement.childNodes[0].style;
  }

  Element.CSS_PROPERTIES.each(function (property) {
    if (style[property]) styleRules.set(property, style[property]);
  });

  if (Prototype.Browser.IE && this.include('opacity'))
    styleRules.set('opacity', this.match(/opacity:\s*((?:0|1)?(?:\.\d*)?)/)[1]);

  return styleRules;
};

if (document.defaultView && document.defaultView.getComputedStyle) {
  Element.getStyles = function (element) {
    var css = document.defaultView.getComputedStyle($(element), null);
    return Element.CSS_PROPERTIES.inject({ }, function (styles, property) {
      styles[property] = css[property];
      return styles;
    });
  };
} else {
  Element.getStyles = function (element) {
    element = $(element);
    var css = element.currentStyle, styles;
    styles = Element.CSS_PROPERTIES.inject({ }, function (results, property) {
      results[property] = css[property];
      return results;
    });
    if (!styles.opacity) styles.opacity = element.getOpacity();
    return styles;
  };
}

Effect.Methods = {
  morph:function (element, style) {
    element = $(element);
    new Effect.Morph(element, Object.extend({ style:style }, arguments[2] || { }));
    return element;
  },
  visualEffect:function (element, effect, options) {
    element = $(element);
    var s = effect.dasherize().camelize(), klass = s.charAt(0).toUpperCase() + s.substring(1);
    new Effect[klass](element, options);
    return element;
  },
  highlight:function (element, options) {
    element = $(element);
    new Effect.Highlight(element, options);
    return element;
  }
};

$w('fade appear grow shrink fold blindUp blindDown slideUp slideDown ' +
  'pulsate shake puff squish switchOff dropOut').each(
  function (effect) {
    Effect.Methods[effect] = function (element, options) {
      element = $(element);
      Effect[effect.charAt(0).toUpperCase() + effect.substring(1)](element, options);
      return element;
    };
  }
);

$w('getInlineOpacity forceRerendering setContentZoom collectTextNodes collectTextNodesIgnoreClass getStyles').each(
  function (f) {
    Effect.Methods[f] = Element[f];
  }
);

Element.addMethods(Effect.Methods);

// script.aculo.us controls.js v1.9.0, Thu Dec 23 16:54:48 -0500 2010

// Copyright (c) 2005-2010 Thomas Fuchs (http://script.aculo.us, http://mir.aculo.us)
//           (c) 2005-2010 Ivan Krstic (http://blogs.law.harvard.edu/ivan)
//           (c) 2005-2010 Jon Tirsen (http://www.tirsen.com)
// Contributors:
//  Richard Livsey
//  Rahul Bhargava
//  Rob Wills
//
// script.aculo.us is freely distributable under the terms of an MIT-style license.
// For details, see the script.aculo.us web site: http://script.aculo.us/

// Autocompleter.Base handles all the autocompletion functionality
// that's independent of the data source for autocompletion. This
// includes drawing the autocompletion menu, observing keyboard
// and mouse events, and similar.
//
// Specific autocompleters need to provide, at the very least,
// a getUpdatedChoices function that will be invoked every time
// the text inside the monitored textbox changes. This method
// should get the text for which to provide autocompletion by
// invoking this.getToken(), NOT by directly accessing
// this.element.value. This is to allow incremental tokenized
// autocompletion. Specific auto-completion logic (AJAX, etc)
// belongs in getUpdatedChoices.
//
// Tokenized incremental autocompletion is enabled automatically
// when an autocompleter is instantiated with the 'tokens' option
// in the options parameter, e.g.:
// new Ajax.Autocompleter('id','upd', '/url/', { tokens: ',' });
// will incrementally autocomplete with a comma as the token.
// Additionally, ',' in the above example can be replaced with
// a token array, e.g. { tokens: [',', '\n'] } which
// enables autocompletion on multiple tokens. This is most
// useful when one of the tokens is \n (a newline), as it
// allows smart autocompletion after linebreaks.

if (typeof Effect == 'undefined')
  throw("controls.js requires including script.aculo.us' effects.js library");

var Autocompleter = { };
Autocompleter.Base = Class.create({
  baseInitialize:function (element, update, options) {
    element = $(element);
    this.element = element;
    this.update = $(update);
    this.hasFocus = false;
    this.changed = false;
    this.active = false;
    this.index = 0;
    this.entryCount = 0;
    this.oldElementValue = this.element.value;

    if (this.setOptions)
      this.setOptions(options);
    else
      this.options = options || { };

    this.options.paramName = this.options.paramName || this.element.name;
    this.options.tokens = this.options.tokens || [];
    this.options.frequency = this.options.frequency || 0.4;
    this.options.minChars = this.options.minChars || 1;
    this.options.onShow = this.options.onShow ||
      function (element, update) {
        if (!update.style.position || update.style.position == 'absolute') {
          update.style.position = 'absolute';
          Position.clone(element, update, {
            setHeight:false,
            offsetTop:element.offsetHeight
          });
        }
        Effect.Appear(update, {duration:0.15});
      };
    this.options.onHide = this.options.onHide ||
      function (element, update) {
        new Effect.Fade(update, {duration:0.15})
      };

    if (typeof(this.options.tokens) == 'string')
      this.options.tokens = new Array(this.options.tokens);
    // Force carriage returns as token delimiters anyway
    if (!this.options.tokens.include('\n'))
      this.options.tokens.push('\n');

    this.observer = null;

    this.element.setAttribute('autocomplete', 'off');

    Element.hide(this.update);

    Event.observe(this.element, 'blur', this.onBlur.bindAsEventListener(this));
    Event.observe(this.element, 'keydown', this.onKeyPress.bindAsEventListener(this));
  },

  show:function () {
    if (Element.getStyle(this.update, 'display') == 'none') this.options.onShow(this.element, this.update);
    if (!this.iefix &&
      (Prototype.Browser.IE) &&
      (Element.getStyle(this.update, 'position') == 'absolute')) {
      new Insertion.After(this.update,
        '<iframe id="' + this.update.id + '_iefix" ' +
          'style="display:none;position:absolute;filter:progid:DXImageTransform.Microsoft.Alpha(opacity=0);" ' +
          'src="javascript:false;" frameborder="0" scrolling="no"></iframe>');
      this.iefix = $(this.update.id + '_iefix');
    }
    if (this.iefix) setTimeout(this.fixIEOverlapping.bind(this), 50);
  },

  fixIEOverlapping:function () {
    Position.clone(this.update, this.iefix, {setTop:(!this.update.style.height)});
    this.iefix.style.zIndex = 1;
    this.update.style.zIndex = 2;
    Element.show(this.iefix);
  },

  hide:function () {
    this.stopIndicator();
    if (Element.getStyle(this.update, 'display') != 'none') this.options.onHide(this.element, this.update);
    if (this.iefix) Element.hide(this.iefix);
  },

  startIndicator:function () {
    if (this.options.indicator) Element.show(this.options.indicator);
  },

  stopIndicator:function () {
    if (this.options.indicator) Element.hide(this.options.indicator);
  },

  onKeyPress:function (event) {
    if (this.active)
      switch (event.keyCode) {
        case Event.KEY_TAB:
        case Event.KEY_RETURN:
          this.selectEntry();
          Event.stop(event);
        case Event.KEY_ESC:
          this.hide();
          this.active = false;
          Event.stop(event);
          return;
        case Event.KEY_LEFT:
        case Event.KEY_RIGHT:
          return;
        case Event.KEY_UP:
          this.markPrevious();
          this.render();
          Event.stop(event);
          return;
        case Event.KEY_DOWN:
          this.markNext();
          this.render();
          Event.stop(event);
          return;
      }
    else
    if (event.keyCode == Event.KEY_TAB || event.keyCode == Event.KEY_RETURN ||
      (Prototype.Browser.WebKit > 0 && event.keyCode == 0)) return;

    this.changed = true;
    this.hasFocus = true;

    if (this.observer) clearTimeout(this.observer);
    this.observer =
      setTimeout(this.onObserverEvent.bind(this), this.options.frequency * 1000);
  },

  activate:function () {
    this.changed = false;
    this.hasFocus = true;
    this.getUpdatedChoices();
  },

  onHover:function (event) {
    var element = Event.findElement(event, 'LI');
    if (this.index != element.autocompleteIndex) {
      this.index = element.autocompleteIndex;
      this.render();
    }
    Event.stop(event);
  },

  onClick:function (event) {
    var element = Event.findElement(event, 'LI');
    this.index = element.autocompleteIndex;
    this.selectEntry();
    this.hide();
  },

  onBlur:function (event) {
    // needed to make click events working
    setTimeout(this.hide.bind(this), 250);
    this.hasFocus = false;
    this.active = false;
  },

  render:function () {
    if (this.entryCount > 0) {
      for (var i = 0; i < this.entryCount; i++)
        this.index == i ?
          Element.addClassName(this.getEntry(i), "selected") :
          Element.removeClassName(this.getEntry(i), "selected");
      if (this.hasFocus) {
        this.show();
        this.active = true;
      }
    } else {
      this.active = false;
      this.hide();
    }
  },

  // Patched with http://www.gilluminate.com/2009/01/20/scriptaculous-autocomplete-page-jump-using-arrow-keys/
  markPrevious:function () {
    if (this.index > 0) {
      this.index--;
    }
    else {
      this.index = this.entryCount - 1;
      this.update.scrollTop = this.update.scrollHeight;
    }
    selection = this.getEntry(this.index);
    selection_top = selection.offsetTop;
    if (selection_top < this.update.scrollTop) {
      this.update.scrollTop = this.update.scrollTop - selection.offsetHeight;
    }
  },

  // Patched with http://www.gilluminate.com/2009/01/20/scriptaculous-autocomplete-page-jump-using-arrow-keys/
  markNext:function () {
    if (this.index < this.entryCount - 1) {
      this.index++;
    }
    else {
      this.index = 0;
      this.update.scrollTop = 0;
    }
    selection = this.getEntry(this.index);
    selection_bottom = selection.offsetTop + selection.offsetHeight;
    if (selection_bottom > this.update.scrollTop + this.update.offsetHeight) {
      this.update.scrollTop = this.update.scrollTop + selection.offsetHeight;
    }
  },

  getEntry:function (index) {
    return this.update.firstChild.childNodes[index];
  },

  getCurrentEntry:function () {
    return this.getEntry(this.index);
  },

  selectEntry:function () {
    this.active = false;
    this.updateElement(this.getCurrentEntry());
  },

  updateElement:function (selectedElement) {
    if (this.options.updateElement) {
      this.options.updateElement(selectedElement);
      return;
    }
    var value = '';
    if (this.options.select) {
      var nodes = $(selectedElement).select('.' + this.options.select) || [];
      if (nodes.length > 0) value = Element.collectTextNodes(nodes[0], this.options.select);
    } else
      value = Element.collectTextNodesIgnoreClass(selectedElement, 'informal');

    var bounds = this.getTokenBounds();
    if (bounds[0] != -1) {
      var newValue = this.element.value.substr(0, bounds[0]);
      var whitespace = this.element.value.substr(bounds[0]).match(/^\s+/);
      if (whitespace)
        newValue += whitespace[0];
      this.element.value = newValue + value + this.element.value.substr(bounds[1]);
    } else {
      this.element.value = value;
    }
    this.oldElementValue = this.element.value;

    // SONAR
    // Following line was commented for SONAR-1688 because in our autosuggest text fields, we use
    // the onfocus() method to reinitialize the value of the input field to ''.
    //this.element.focus();
    // /SONAR

    if (this.options.afterUpdateElement)
      this.options.afterUpdateElement(this.element, selectedElement);
  },

  updateChoices:function (choices) {
    if (!this.changed && this.hasFocus) {
      this.update.innerHTML = choices;
      Element.cleanWhitespace(this.update);
      Element.cleanWhitespace(this.update.down());

      if (this.update.firstChild && this.update.down().childNodes) {
        this.entryCount =
          this.update.down().childNodes.length;
        for (var i = 0; i < this.entryCount; i++) {
          var entry = this.getEntry(i);
          entry.autocompleteIndex = i;
          this.addObservers(entry);
        }
      } else {
        this.entryCount = 0;
      }

      this.stopIndicator();
      // Fix bug with arrow keys : http://www.gilluminate.com/2009/01/20/scriptaculous-autocomplete-page-jump-using-arrow-keys/
      this.update.scrollTop = 0;
      this.index = 0;

      if (this.entryCount == 1 && this.options.autoSelect) {
        this.selectEntry();
        this.hide();
      } else {
        this.render();
      }
    }
  },

  addObservers:function (element) {
    Event.observe(element, "mouseover", this.onHover.bindAsEventListener(this));
    Event.observe(element, "click", this.onClick.bindAsEventListener(this));
  },

  onObserverEvent:function () {
    this.changed = false;
    this.tokenBounds = null;
    if (this.getToken().length >= this.options.minChars) {
      this.getUpdatedChoices();
    } else {
      this.active = false;
      this.hide();
    }
    this.oldElementValue = this.element.value;
  },

  getToken:function () {
    var bounds = this.getTokenBounds();
    return this.element.value.substring(bounds[0], bounds[1]).strip();
  },

  getTokenBounds:function () {
    if (null != this.tokenBounds) return this.tokenBounds;
    var value = this.element.value;
    if (value.strip().empty()) return [-1, 0];
    var diff = arguments.callee.getFirstDifferencePos(value, this.oldElementValue);
    var offset = (diff == this.oldElementValue.length ? 1 : 0);
    var prevTokenPos = -1, nextTokenPos = value.length;
    var tp;
    for (var index = 0, l = this.options.tokens.length; index < l; ++index) {
      tp = value.lastIndexOf(this.options.tokens[index], diff + offset - 1);
      if (tp > prevTokenPos) prevTokenPos = tp;
      tp = value.indexOf(this.options.tokens[index], diff + offset);
      if (-1 != tp && tp < nextTokenPos) nextTokenPos = tp;
    }
    return (this.tokenBounds = [prevTokenPos + 1, nextTokenPos]);
  }
});

Autocompleter.Base.prototype.getTokenBounds.getFirstDifferencePos = function (newS, oldS) {
  var boundary = Math.min(newS.length, oldS.length);
  for (var index = 0; index < boundary; ++index)
    if (newS[index] != oldS[index])
      return index;
  return boundary;
};

Ajax.Autocompleter = Class.create(Autocompleter.Base, {
  initialize:function (element, update, url, options) {
    this.baseInitialize(element, update, options);
    this.options.asynchronous = true;
    this.options.onComplete = this.onComplete.bind(this);
    this.options.defaultParams = this.options.parameters || null;
    this.url = url;
  },

  getUpdatedChoices:function () {
    this.startIndicator();

    var entry = encodeURIComponent(this.options.paramName) + '=' +
      encodeURIComponent(this.getToken());

    this.options.parameters = this.options.callback ?
      this.options.callback(this.element, entry) : entry;

    if (this.options.defaultParams)
      this.options.parameters += '&' + this.options.defaultParams;

    new Ajax.Request(this.url, this.options);
  },

  onComplete:function (request) {
    this.updateChoices(request.responseText);
  }
});

// The local array autocompleter. Used when you'd prefer to
// inject an array of autocompletion options into the page, rather
// than sending out Ajax queries, which can be quite slow sometimes.
//
// The constructor takes four parameters. The first two are, as usual,
// the id of the monitored textbox, and id of the autocompletion menu.
// The third is the array you want to autocomplete from, and the fourth
// is the options block.
//
// Extra local autocompletion options:
// - choices - How many autocompletion choices to offer
//
// - partialSearch - If false, the autocompleter will match entered
//                    text only at the beginning of strings in the
//                    autocomplete array. Defaults to true, which will
//                    match text at the beginning of any *word* in the
//                    strings in the autocomplete array. If you want to
//                    search anywhere in the string, additionally set
//                    the option fullSearch to true (default: off).
//
// - fullSsearch - Search anywhere in autocomplete array strings.
//
// - partialChars - How many characters to enter before triggering
//                   a partial match (unlike minChars, which defines
//                   how many characters are required to do any match
//                   at all). Defaults to 2.
//
// - ignoreCase - Whether to ignore case when autocompleting.
//                 Defaults to true.
//
// It's possible to pass in a custom function as the 'selector'
// option, if you prefer to write your own autocompletion logic.
// In that case, the other options above will not apply unless
// you support them.

Autocompleter.Local = Class.create(Autocompleter.Base, {
  initialize:function (element, update, array, options) {
    this.baseInitialize(element, update, options);
    this.options.array = array;
  },

  getUpdatedChoices:function () {
    this.updateChoices(this.options.selector(this));
  },

  setOptions:function (options) {
    this.options = Object.extend({
      choices:10,
      partialSearch:true,
      partialChars:2,
      ignoreCase:true,
      fullSearch:false,
      selector:function (instance) {
        var ret = []; // Beginning matches
        var partial = []; // Inside matches
        var entry = instance.getToken();
        var count = 0;

        for (var i = 0; i < instance.options.array.length &&
          ret.length < instance.options.choices; i++) {

          var elem = instance.options.array[i];
          var foundPos = instance.options.ignoreCase ?
            elem.toLowerCase().indexOf(entry.toLowerCase()) :
            elem.indexOf(entry);

          while (foundPos != -1) {
            if (foundPos == 0 && elem.length != entry.length) {
              ret.push("<li><strong>" + elem.substr(0, entry.length) + "</strong>" +
                elem.substr(entry.length) + "</li>");
              break;
            } else if (entry.length >= instance.options.partialChars &&
              instance.options.partialSearch && foundPos != -1) {
              if (instance.options.fullSearch || /\s/.test(elem.substr(foundPos - 1, 1))) {
                partial.push("<li>" + elem.substr(0, foundPos) + "<strong>" +
                  elem.substr(foundPos, entry.length) + "</strong>" + elem.substr(
                  foundPos + entry.length) + "</li>");
                break;
              }
            }

            foundPos = instance.options.ignoreCase ?
              elem.toLowerCase().indexOf(entry.toLowerCase(), foundPos + 1) :
              elem.indexOf(entry, foundPos + 1);

          }
        }
        if (partial.length)
          ret = ret.concat(partial.slice(0, instance.options.choices - ret.length));
        return "<ul>" + ret.join('') + "</ul>";
      }
    }, options || { });
  }
});

// AJAX in-place editor and collection editor
// Full rewrite by Christophe Porteneuve <tdd@tddsworld.com> (April 2007).

// Use this if you notice weird scrolling problems on some browsers,
// the DOM might be a bit confused when this gets called so do this
// waits 1 ms (with setTimeout) until it does the activation
Field.scrollFreeActivate = function (field) {
  setTimeout(function () {
    Field.activate(field);
  }, 1);
};

Ajax.InPlaceEditor = Class.create({
  initialize:function (element, url, options) {
    this.url = url;
    this.element = element = $(element);
    this.prepareOptions();
    this._controls = { };
    arguments.callee.dealWithDeprecatedOptions(options); // DEPRECATION LAYER!!!
    Object.extend(this.options, options || { });
    if (!this.options.formId && this.element.id) {
      this.options.formId = this.element.id + '-inplaceeditor';
      if ($(this.options.formId))
        this.options.formId = '';
    }
    if (this.options.externalControl)
      this.options.externalControl = $(this.options.externalControl);
    if (!this.options.externalControl)
      this.options.externalControlOnly = false;
    this._originalBackground = this.element.getStyle('background-color') || 'transparent';
    this.element.title = this.options.clickToEditText;
    this._boundCancelHandler = this.handleFormCancellation.bind(this);
    this._boundComplete = (this.options.onComplete || Prototype.emptyFunction).bind(this);
    this._boundFailureHandler = this.handleAJAXFailure.bind(this);
    this._boundSubmitHandler = this.handleFormSubmission.bind(this);
    this._boundWrapperHandler = this.wrapUp.bind(this);
    this.registerListeners();
  },
  checkForEscapeOrReturn:function (e) {
    if (!this._editing || e.ctrlKey || e.altKey || e.shiftKey) return;
    if (Event.KEY_ESC == e.keyCode)
      this.handleFormCancellation(e);
    else if (Event.KEY_RETURN == e.keyCode)
      this.handleFormSubmission(e);
  },
  createControl:function (mode, handler, extraClasses) {
    var control = this.options[mode + 'Control'];
    var text = this.options[mode + 'Text'];
    if ('button' == control) {
      var btn = document.createElement('input');
      btn.type = 'submit';
      btn.value = text;
      btn.className = 'editor_' + mode + '_button';
      if ('cancel' == mode)
        btn.onclick = this._boundCancelHandler;
      this._form.appendChild(btn);
      this._controls[mode] = btn;
    } else if ('link' == control) {
      var link = document.createElement('a');
      link.href = '#';
      link.appendChild(document.createTextNode(text));
      link.onclick = 'cancel' == mode ? this._boundCancelHandler : this._boundSubmitHandler;
      link.className = 'editor_' + mode + '_link';
      if (extraClasses)
        link.className += ' ' + extraClasses;
      this._form.appendChild(link);
      this._controls[mode] = link;
    }
  },
  createEditField:function () {
    var text = (this.options.loadTextURL ? this.options.loadingText : this.getText());
    var fld;
    if (1 >= this.options.rows && !/\r|\n/.test(this.getText())) {
      fld = document.createElement('input');
      fld.type = 'text';
      var size = this.options.size || this.options.cols || 0;
      if (0 < size) fld.size = size;
    } else {
      fld = document.createElement('textarea');
      fld.rows = (1 >= this.options.rows ? this.options.autoRows : this.options.rows);
      fld.cols = this.options.cols || 40;
    }
    fld.name = this.options.paramName;
    fld.value = text; // No HTML breaks conversion anymore
    fld.className = 'editor_field';
    if (this.options.submitOnBlur)
      fld.onblur = this._boundSubmitHandler;
    this._controls.editor = fld;
    if (this.options.loadTextURL)
      this.loadExternalText();
    this._form.appendChild(this._controls.editor);
  },
  createForm:function () {
    var ipe = this;

    function addText(mode, condition) {
      var text = ipe.options['text' + mode + 'Controls'];
      if (!text || condition === false) return;
      ipe._form.appendChild(document.createTextNode(text));
    }

    ;
    this._form = $(document.createElement('form'));
    this._form.id = this.options.formId;
    this._form.addClassName(this.options.formClassName);
    this._form.onsubmit = this._boundSubmitHandler;
    this.createEditField();
    if ('textarea' == this._controls.editor.tagName.toLowerCase())
      this._form.appendChild(document.createElement('br'));
    if (this.options.onFormCustomization)
      this.options.onFormCustomization(this, this._form);
    addText('Before', this.options.okControl || this.options.cancelControl);
    this.createControl('ok', this._boundSubmitHandler);
    addText('Between', this.options.okControl && this.options.cancelControl);
    this.createControl('cancel', this._boundCancelHandler, 'editor_cancel');
    addText('After', this.options.okControl || this.options.cancelControl);
  },
  destroy:function () {
    if (this._oldInnerHTML)
      this.element.innerHTML = this._oldInnerHTML;
    this.leaveEditMode();
    this.unregisterListeners();
  },
  enterEditMode:function (e) {
    if (this._saving || this._editing) return;
    this._editing = true;
    this.triggerCallback('onEnterEditMode');
    if (this.options.externalControl)
      this.options.externalControl.hide();
    this.element.hide();
    this.createForm();
    this.element.parentNode.insertBefore(this._form, this.element);
    if (!this.options.loadTextURL)
      this.postProcessEditField();
    if (e) Event.stop(e);
  },
  enterHover:function (e) {
    if (this.options.hoverClassName)
      this.element.addClassName(this.options.hoverClassName);
    if (this._saving) return;
    this.triggerCallback('onEnterHover');
  },
  getText:function () {
    return this.element.innerHTML.unescapeHTML();
  },
  handleAJAXFailure:function (transport) {
    this.triggerCallback('onFailure', transport);
    if (this._oldInnerHTML) {
      this.element.innerHTML = this._oldInnerHTML;
      this._oldInnerHTML = null;
    }
  },
  handleFormCancellation:function (e) {
    this.wrapUp();
    if (e) Event.stop(e);
  },
  handleFormSubmission:function (e) {
    var form = this._form;
    var value = $F(this._controls.editor);
    this.prepareSubmission();
    var params = this.options.callback(form, value) || '';
    if (Object.isString(params))
      params = params.toQueryParams();
    params.editorId = this.element.id;
    if (this.options.htmlResponse) {
      var options = Object.extend({ evalScripts:true }, this.options.ajaxOptions);
      Object.extend(options, {
        parameters:params,
        onComplete:this._boundWrapperHandler,
        onFailure:this._boundFailureHandler
      });
      new Ajax.Updater({ success:this.element }, this.url, options);
    } else {
      var options = Object.extend({ method:'get' }, this.options.ajaxOptions);
      Object.extend(options, {
        parameters:params,
        onComplete:this._boundWrapperHandler,
        onFailure:this._boundFailureHandler
      });
      new Ajax.Request(this.url, options);
    }
    if (e) Event.stop(e);
  },
  leaveEditMode:function () {
    this.element.removeClassName(this.options.savingClassName);
    this.removeForm();
    this.leaveHover();
    this.element.style.backgroundColor = this._originalBackground;
    this.element.show();
    if (this.options.externalControl)
      this.options.externalControl.show();
    this._saving = false;
    this._editing = false;
    this._oldInnerHTML = null;
    this.triggerCallback('onLeaveEditMode');
  },
  leaveHover:function (e) {
    if (this.options.hoverClassName)
      this.element.removeClassName(this.options.hoverClassName);
    if (this._saving) return;
    this.triggerCallback('onLeaveHover');
  },
  loadExternalText:function () {
    this._form.addClassName(this.options.loadingClassName);
    this._controls.editor.disabled = true;
    var options = Object.extend({ method:'get' }, this.options.ajaxOptions);
    Object.extend(options, {
      parameters:'editorId=' + encodeURIComponent(this.element.id),
      onComplete:Prototype.emptyFunction,
      onSuccess:function (transport) {
        this._form.removeClassName(this.options.loadingClassName);
        var text = transport.responseText;
        if (this.options.stripLoadedTextTags)
          text = text.stripTags();
        this._controls.editor.value = text;
        this._controls.editor.disabled = false;
        this.postProcessEditField();
      }.bind(this),
      onFailure:this._boundFailureHandler
    });
    new Ajax.Request(this.options.loadTextURL, options);
  },
  postProcessEditField:function () {
    var fpc = this.options.fieldPostCreation;
    if (fpc)
      $(this._controls.editor)['focus' == fpc ? 'focus' : 'activate']();
  },
  prepareOptions:function () {
    this.options = Object.clone(Ajax.InPlaceEditor.DefaultOptions);
    Object.extend(this.options, Ajax.InPlaceEditor.DefaultCallbacks);
    [this._extraDefaultOptions].flatten().compact().each(function (defs) {
      Object.extend(this.options, defs);
    }.bind(this));
  },
  prepareSubmission:function () {
    this._saving = true;
    this.removeForm();
    this.leaveHover();
    this.showSaving();
  },
  registerListeners:function () {
    this._listeners = { };
    var listener;
    $H(Ajax.InPlaceEditor.Listeners).each(function (pair) {
      listener = this[pair.value].bind(this);
      this._listeners[pair.key] = listener;
      if (!this.options.externalControlOnly)
        this.element.observe(pair.key, listener);
      if (this.options.externalControl)
        this.options.externalControl.observe(pair.key, listener);
    }.bind(this));
  },
  removeForm:function () {
    if (!this._form) return;
    this._form.remove();
    this._form = null;
    this._controls = { };
  },
  showSaving:function () {
    this._oldInnerHTML = this.element.innerHTML;
    this.element.innerHTML = this.options.savingText;
    this.element.addClassName(this.options.savingClassName);
    this.element.style.backgroundColor = this._originalBackground;
    this.element.show();
  },
  triggerCallback:function (cbName, arg) {
    if ('function' == typeof this.options[cbName]) {
      this.options[cbName](this, arg);
    }
  },
  unregisterListeners:function () {
    $H(this._listeners).each(function (pair) {
      if (!this.options.externalControlOnly)
        this.element.stopObserving(pair.key, pair.value);
      if (this.options.externalControl)
        this.options.externalControl.stopObserving(pair.key, pair.value);
    }.bind(this));
  },
  wrapUp:function (transport) {
    this.leaveEditMode();
    // Can't use triggerCallback due to backward compatibility: requires
    // binding + direct element
    this._boundComplete(transport, this.element);
  }
});

Object.extend(Ajax.InPlaceEditor.prototype, {
  dispose:Ajax.InPlaceEditor.prototype.destroy
});

Ajax.InPlaceCollectionEditor = Class.create(Ajax.InPlaceEditor, {
  initialize:function ($super, element, url, options) {
    this._extraDefaultOptions = Ajax.InPlaceCollectionEditor.DefaultOptions;
    $super(element, url, options);
  },

  createEditField:function () {
    var list = document.createElement('select');
    list.name = this.options.paramName;
    list.size = 1;
    this._controls.editor = list;
    this._collection = this.options.collection || [];
    if (this.options.loadCollectionURL)
      this.loadCollection();
    else
      this.checkForExternalText();
    this._form.appendChild(this._controls.editor);
  },

  loadCollection:function () {
    this._form.addClassName(this.options.loadingClassName);
    this.showLoadingText(this.options.loadingCollectionText);
    var options = Object.extend({ method:'get' }, this.options.ajaxOptions);
    Object.extend(options, {
      parameters:'editorId=' + encodeURIComponent(this.element.id),
      onComplete:Prototype.emptyFunction,
      onSuccess:function (transport) {
        var js = transport.responseText.strip();
        if (!/^\[.*\]$/.test(js)) // TODO: improve sanity check
          throw('Server returned an invalid collection representation.');
        this._collection = eval(js);
        this.checkForExternalText();
      }.bind(this),
      onFailure:this.onFailure
    });
    new Ajax.Request(this.options.loadCollectionURL, options);
  },

  showLoadingText:function (text) {
    this._controls.editor.disabled = true;
    var tempOption = this._controls.editor.firstChild;
    if (!tempOption) {
      tempOption = document.createElement('option');
      tempOption.value = '';
      this._controls.editor.appendChild(tempOption);
      tempOption.selected = true;
    }
    tempOption.update((text || '').stripScripts().stripTags());
  },

  checkForExternalText:function () {
    this._text = this.getText();
    if (this.options.loadTextURL)
      this.loadExternalText();
    else
      this.buildOptionList();
  },

  loadExternalText:function () {
    this.showLoadingText(this.options.loadingText);
    var options = Object.extend({ method:'get' }, this.options.ajaxOptions);
    Object.extend(options, {
      parameters:'editorId=' + encodeURIComponent(this.element.id),
      onComplete:Prototype.emptyFunction,
      onSuccess:function (transport) {
        this._text = transport.responseText.strip();
        this.buildOptionList();
      }.bind(this),
      onFailure:this.onFailure
    });
    new Ajax.Request(this.options.loadTextURL, options);
  },

  buildOptionList:function () {
    this._form.removeClassName(this.options.loadingClassName);
    this._collection = this._collection.map(function (entry) {
      return 2 === entry.length ? entry : [entry, entry].flatten();
    });
    var marker = ('value' in this.options) ? this.options.value : this._text;
    var textFound = this._collection.any(function (entry) {
      return entry[0] == marker;
    }.bind(this));
    this._controls.editor.update('');
    var option;
    this._collection.each(function (entry, index) {
      option = document.createElement('option');
      option.value = entry[0];
      option.selected = textFound ? entry[0] == marker : 0 == index;
      option.appendChild(document.createTextNode(entry[1]));
      this._controls.editor.appendChild(option);
    }.bind(this));
    this._controls.editor.disabled = false;
    Field.scrollFreeActivate(this._controls.editor);
  }
});

//**** DEPRECATION LAYER FOR InPlace[Collection]Editor! ****
//**** This only  exists for a while,  in order to  let ****
//**** users adapt to  the new API.  Read up on the new ****
//**** API and convert your code to it ASAP!            ****

Ajax.InPlaceEditor.prototype.initialize.dealWithDeprecatedOptions = function (options) {
  if (!options) return;
  function fallback(name, expr) {
    if (name in options || expr === undefined) return;
    options[name] = expr;
  }

  ;
  fallback('cancelControl', (options.cancelLink ? 'link' : (options.cancelButton ? 'button' :
    options.cancelLink == options.cancelButton == false ? false : undefined)));
  fallback('okControl', (options.okLink ? 'link' : (options.okButton ? 'button' :
    options.okLink == options.okButton == false ? false : undefined)));
  fallback('highlightColor', options.highlightcolor);
  fallback('highlightEndColor', options.highlightendcolor);
};

Object.extend(Ajax.InPlaceEditor, {
  DefaultOptions:{
    ajaxOptions:{ },
    autoRows:3, // Use when multi-line w/ rows == 1
    cancelControl:'link', // 'link'|'button'|false
    cancelText:'cancel',
    clickToEditText:'Click to edit',
    externalControl:null, // id|elt
    externalControlOnly:false,
    fieldPostCreation:'activate', // 'activate'|'focus'|false
    formClassName:'inplaceeditor-form',
    formId:null, // id|elt
    highlightColor:'#ffff99',
    highlightEndColor:'#ffffff',
    hoverClassName:'',
    htmlResponse:true,
    loadingClassName:'inplaceeditor-loading',
    loadingText:'Loading...',
    okControl:'button', // 'link'|'button'|false
    okText:'ok',
    paramName:'value',
    rows:1, // If 1 and multi-line, uses autoRows
    savingClassName:'inplaceeditor-saving',
    savingText:'Saving...',
    size:0,
    stripLoadedTextTags:false,
    submitOnBlur:false,
    textAfterControls:'',
    textBeforeControls:'',
    textBetweenControls:''
  },
  DefaultCallbacks:{
    callback:function (form) {
      return Form.serialize(form);
    },
    onComplete:function (transport, element) {
      // For backward compatibility, this one is bound to the IPE, and passes
      // the element directly.  It was too often customized, so we don't break it.
      new Effect.Highlight(element, {
        startcolor:this.options.highlightColor, keepBackgroundImage:true });
    },
    onEnterEditMode:null,
    onEnterHover:function (ipe) {
      ipe.element.style.backgroundColor = ipe.options.highlightColor;
      if (ipe._effect)
        ipe._effect.cancel();
    },
    onFailure:function (transport, ipe) {
      alert('Error communication with the server: ' + transport.responseText.stripTags());
    },
    onFormCustomization:null, // Takes the IPE and its generated form, after editor, before controls.
    onLeaveEditMode:null,
    onLeaveHover:function (ipe) {
      ipe._effect = new Effect.Highlight(ipe.element, {
        startcolor:ipe.options.highlightColor, endcolor:ipe.options.highlightEndColor,
        restorecolor:ipe._originalBackground, keepBackgroundImage:true
      });
    }
  },
  Listeners:{
    click:'enterEditMode',
    keydown:'checkForEscapeOrReturn',
    mouseover:'enterHover',
    mouseout:'leaveHover'
  }
});

Ajax.InPlaceCollectionEditor.DefaultOptions = {
  loadingCollectionText:'Loading options...'
};

// Delayed observer, like Form.Element.Observer,
// but waits for delay after last key input
// Ideal for live-search fields

Form.Element.DelayedObserver = Class.create({
  initialize:function (element, delay, callback) {
    this.delay = delay || 0.5;
    this.element = $(element);
    this.callback = callback;
    this.timer = null;
    this.lastValue = $F(this.element);
    Event.observe(this.element, 'keyup', this.delayedListener.bindAsEventListener(this));
  },
  delayedListener:function (event) {
    if (this.lastValue == $F(this.element)) return;
    if (this.timer) clearTimeout(this.timer);
    this.timer = setTimeout(this.onTimerEvent.bind(this), this.delay * 1000);
    this.lastValue = $F(this.element);
  },
  onTimerEvent:function () {
    this.timer = null;
    this.callback(this.element, $F(this.element));
  }
});
// script.aculo.us dragdrop.js v1.9.0, Thu Dec 23 16:54:48 -0500 2010

// Copyright (c) 2005-2010 Thomas Fuchs (http://script.aculo.us, http://mir.aculo.us)
//
// script.aculo.us is freely distributable under the terms of an MIT-style license.
// For details, see the script.aculo.us web site: http://script.aculo.us/

if (Object.isUndefined(Effect))
  throw("dragdrop.js requires including script.aculo.us' effects.js library");

var Droppables = {
  drops:[],

  remove:function (element) {
    this.drops = this.drops.reject(function (d) {
      return d.element == $(element)
    });
  },

  add:function (element) {
    element = $(element);
    var options = Object.extend({
      greedy:true,
      hoverclass:null,
      tree:false
    }, arguments[1] || { });

    // cache containers
    if (options.containment) {
      options._containers = [];
      var containment = options.containment;
      if (Object.isArray(containment)) {
        containment.each(function (c) {
          options._containers.push($(c))
        });
      } else {
        options._containers.push($(containment));
      }
    }

    if (options.accept) options.accept = [options.accept].flatten();

    Element.makePositioned(element); // fix IE
    options.element = element;

    this.drops.push(options);
  },

  findDeepestChild:function (drops) {
    deepest = drops[0];

    for (i = 1; i < drops.length; ++i)
      if (Element.isParent(drops[i].element, deepest.element))
        deepest = drops[i];

    return deepest;
  },

  isContained:function (element, drop) {
    var containmentNode;
    if (drop.tree) {
      containmentNode = element.treeNode;
    } else {
      containmentNode = element.parentNode;
    }
    return drop._containers.detect(function (c) {
      return containmentNode == c
    });
  },

  isAffected:function (point, element, drop) {
    return (
      (drop.element != element) &&
        ((!drop._containers) ||
          this.isContained(element, drop)) &&
        ((!drop.accept) ||
          (Element.classNames(element).detect(
            function (v) {
              return drop.accept.include(v)
            }) )) &&
        Position.within(drop.element, point[0], point[1]) );
  },

  deactivate:function (drop) {
    if (drop.hoverclass)
      Element.removeClassName(drop.element, drop.hoverclass);
    this.last_active = null;
  },

  activate:function (drop) {
    if (drop.hoverclass)
      Element.addClassName(drop.element, drop.hoverclass);
    this.last_active = drop;
  },

  show:function (point, element) {
    if (!this.drops.length) return;
    var drop, affected = [];

    this.drops.each(function (drop) {
      if (Droppables.isAffected(point, element, drop))
        affected.push(drop);
    });

    if (affected.length > 0)
      drop = Droppables.findDeepestChild(affected);

    if (this.last_active && this.last_active != drop) this.deactivate(this.last_active);
    if (drop) {
      Position.within(drop.element, point[0], point[1]);
      if (drop.onHover)
        drop.onHover(element, drop.element, Position.overlap(drop.overlap, drop.element));

      if (drop != this.last_active) Droppables.activate(drop);
    }
  },

  fire:function (event, element) {
    if (!this.last_active) return;
    Position.prepare();

    if (this.isAffected([Event.pointerX(event), Event.pointerY(event)], element, this.last_active))
      if (this.last_active.onDrop) {
        this.last_active.onDrop(element, this.last_active.element, event);
        return true;
      }
  },

  reset:function () {
    if (this.last_active)
      this.deactivate(this.last_active);
  }
};

var Draggables = {
  drags:[],
  observers:[],

  register:function (draggable) {
    if (this.drags.length == 0) {
      this.eventMouseUp = this.endDrag.bindAsEventListener(this);
      this.eventMouseMove = this.updateDrag.bindAsEventListener(this);
      this.eventKeypress = this.keyPress.bindAsEventListener(this);

      Event.observe(document, "mouseup", this.eventMouseUp);
      Event.observe(document, "mousemove", this.eventMouseMove);
      Event.observe(document, "keypress", this.eventKeypress);
    }
    this.drags.push(draggable);
  },

  unregister:function (draggable) {
    this.drags = this.drags.reject(function (d) {
      return d == draggable
    });
    if (this.drags.length == 0) {
      Event.stopObserving(document, "mouseup", this.eventMouseUp);
      Event.stopObserving(document, "mousemove", this.eventMouseMove);
      Event.stopObserving(document, "keypress", this.eventKeypress);
    }
  },

  activate:function (draggable) {
    if (draggable.options.delay) {
      this._timeout = setTimeout(function () {
        Draggables._timeout = null;
        window.focus();
        Draggables.activeDraggable = draggable;
      }.bind(this), draggable.options.delay);
    } else {
      window.focus(); // allows keypress events if window isn't currently focused, fails for Safari
      this.activeDraggable = draggable;
    }
  },

  deactivate:function () {
    this.activeDraggable = null;
  },

  updateDrag:function (event) {
    if (!this.activeDraggable) return;
    var pointer = [Event.pointerX(event), Event.pointerY(event)];
    // Mozilla-based browsers fire successive mousemove events with
    // the same coordinates, prevent needless redrawing (moz bug?)
    if (this._lastPointer && (this._lastPointer.inspect() == pointer.inspect())) return;
    this._lastPointer = pointer;

    this.activeDraggable.updateDrag(event, pointer);
  },

  endDrag:function (event) {
    if (this._timeout) {
      clearTimeout(this._timeout);
      this._timeout = null;
    }
    if (!this.activeDraggable) return;
    this._lastPointer = null;
    this.activeDraggable.endDrag(event);
    this.activeDraggable = null;
  },

  keyPress:function (event) {
    if (this.activeDraggable)
      this.activeDraggable.keyPress(event);
  },

  addObserver:function (observer) {
    this.observers.push(observer);
    this._cacheObserverCallbacks();
  },

  removeObserver:function (element) {  // element instead of observer fixes mem leaks
    this.observers = this.observers.reject(function (o) {
      return o.element == element
    });
    this._cacheObserverCallbacks();
  },

  notify:function (eventName, draggable, event) {  // 'onStart', 'onEnd', 'onDrag'
    if (this[eventName + 'Count'] > 0)
      this.observers.each(function (o) {
        if (o[eventName]) o[eventName](eventName, draggable, event);
      });
    if (draggable.options[eventName]) draggable.options[eventName](draggable, event);
  },

  _cacheObserverCallbacks:function () {
    ['onStart', 'onEnd', 'onDrag'].each(function (eventName) {
      Draggables[eventName + 'Count'] = Draggables.observers.select(
        function (o) {
          return o[eventName];
        }
      ).length;
    });
  }
};

/*--------------------------------------------------------------------------*/

var Draggable = Class.create({
  initialize:function (element) {
    var defaults = {
      handle:false,
      reverteffect:function (element, top_offset, left_offset) {
        var dur = Math.sqrt(Math.abs(top_offset ^ 2) + Math.abs(left_offset ^ 2)) * 0.02;
        new Effect.Move(element, { x:-left_offset, y:-top_offset, duration:dur,
          queue:{scope:'_draggable', position:'end'}
        });
      },
      endeffect:function (element) {
        var toOpacity = Object.isNumber(element._opacity) ? element._opacity : 1.0;
        new Effect.Opacity(element, {duration:0.2, from:0.7, to:toOpacity,
          queue:{scope:'_draggable', position:'end'},
          afterFinish:function () {
            Draggable._dragging[element] = false
          }
        });
      },
      zindex:1000,
      revert:false,
      quiet:false,
      scroll:false,
      scrollSensitivity:20,
      scrollSpeed:15,
      snap:false, // false, or xy or [x,y] or function(x,y){ return [x,y] }
      delay:0
    };

    if (!arguments[1] || Object.isUndefined(arguments[1].endeffect))
      Object.extend(defaults, {
        starteffect:function (element) {
          element._opacity = Element.getOpacity(element);
          Draggable._dragging[element] = true;
          new Effect.Opacity(element, {duration:0.2, from:element._opacity, to:0.7});
        }
      });

    var options = Object.extend(defaults, arguments[1] || { });

    this.element = $(element);

    if (options.handle && Object.isString(options.handle))
      this.handle = this.element.down('.' + options.handle, 0);

    if (!this.handle) this.handle = $(options.handle);
    if (!this.handle) this.handle = this.element;

    if (options.scroll && !options.scroll.scrollTo && !options.scroll.outerHTML) {
      options.scroll = $(options.scroll);
      this._isScrollChild = Element.childOf(this.element, options.scroll);
    }

    Element.makePositioned(this.element); // fix IE

    this.options = options;
    this.dragging = false;

    this.eventMouseDown = this.initDrag.bindAsEventListener(this);
    Event.observe(this.handle, "mousedown", this.eventMouseDown);

    Draggables.register(this);
  },

  destroy:function () {
    Event.stopObserving(this.handle, "mousedown", this.eventMouseDown);
    Draggables.unregister(this);
  },

  currentDelta:function () {
    return([
      parseInt(Element.getStyle(this.element, 'left') || '0'),
      parseInt(Element.getStyle(this.element, 'top') || '0')]);
  },

  initDrag:function (event) {
    if (!Object.isUndefined(Draggable._dragging[this.element]) &&
      Draggable._dragging[this.element]) return;
    if (Event.isLeftClick(event)) {
      // abort on form elements, fixes a Firefox issue
      var src = Event.element(event);
      if ((tag_name = src.tagName.toUpperCase()) && (
        tag_name == 'INPUT' ||
          tag_name == 'SELECT' ||
          tag_name == 'OPTION' ||
          tag_name == 'BUTTON' ||
          tag_name == 'TEXTAREA')) return;

      var pointer = [Event.pointerX(event), Event.pointerY(event)];
      var pos = this.element.cumulativeOffset();
      this.offset = [0, 1].map(function (i) {
        return (pointer[i] - pos[i])
      });

      Draggables.activate(this);
      Event.stop(event);
    }
  },

  startDrag:function (event) {
    this.dragging = true;
    if (!this.delta)
      this.delta = this.currentDelta();

    if (this.options.zindex) {
      this.originalZ = parseInt(Element.getStyle(this.element, 'z-index') || 0);
      this.element.style.zIndex = this.options.zindex;
    }

    if (this.options.ghosting) {
      this._clone = this.element.cloneNode(true);
      this._originallyAbsolute = (this.element.getStyle('position') == 'absolute');
      if (!this._originallyAbsolute)
        Position.absolutize(this.element);
      this.element.parentNode.insertBefore(this._clone, this.element);
    }

    if (this.options.scroll) {
      if (this.options.scroll == window) {
        var where = this._getWindowScroll(this.options.scroll);
        this.originalScrollLeft = where.left;
        this.originalScrollTop = where.top;
      } else {
        this.originalScrollLeft = this.options.scroll.scrollLeft;
        this.originalScrollTop = this.options.scroll.scrollTop;
      }
    }

    Draggables.notify('onStart', this, event);

    if (this.options.starteffect) this.options.starteffect(this.element);
  },

  updateDrag:function (event, pointer) {
    if (!this.dragging) this.startDrag(event);

    if (!this.options.quiet) {
      Position.prepare();
      Droppables.show(pointer, this.element);
    }

    Draggables.notify('onDrag', this, event);

    this.draw(pointer);
    if (this.options.change) this.options.change(this);

    if (this.options.scroll) {
      this.stopScrolling();

      var p;
      if (this.options.scroll == window) {
        with (this._getWindowScroll(this.options.scroll)) {
          p = [ left, top, left + width, top + height ];
        }
      } else {
        p = Position.page(this.options.scroll).toArray();
        p[0] += this.options.scroll.scrollLeft + Position.deltaX;
        p[1] += this.options.scroll.scrollTop + Position.deltaY;
        p.push(p[0] + this.options.scroll.offsetWidth);
        p.push(p[1] + this.options.scroll.offsetHeight);
      }
      var speed = [0, 0];
      if (pointer[0] < (p[0] + this.options.scrollSensitivity)) speed[0] = pointer[0] - (p[0] + this.options.scrollSensitivity);
      if (pointer[1] < (p[1] + this.options.scrollSensitivity)) speed[1] = pointer[1] - (p[1] + this.options.scrollSensitivity);
      if (pointer[0] > (p[2] - this.options.scrollSensitivity)) speed[0] = pointer[0] - (p[2] - this.options.scrollSensitivity);
      if (pointer[1] > (p[3] - this.options.scrollSensitivity)) speed[1] = pointer[1] - (p[3] - this.options.scrollSensitivity);
      this.startScrolling(speed);
    }

    // fix AppleWebKit rendering
    if (Prototype.Browser.WebKit) window.scrollBy(0, 0);

    Event.stop(event);
  },

  finishDrag:function (event, success) {
    this.dragging = false;

    if (this.options.quiet) {
      Position.prepare();
      var pointer = [Event.pointerX(event), Event.pointerY(event)];
      Droppables.show(pointer, this.element);
    }

    if (this.options.ghosting) {
      if (!this._originallyAbsolute)
        Position.relativize(this.element);
      delete this._originallyAbsolute;
      Element.remove(this._clone);
      this._clone = null;
    }

    var dropped = false;
    if (success) {
      dropped = Droppables.fire(event, this.element);
      if (!dropped) dropped = false;
    }
    if (dropped && this.options.onDropped) this.options.onDropped(this.element);
    Draggables.notify('onEnd', this, event);

    var revert = this.options.revert;
    if (revert && Object.isFunction(revert)) revert = revert(this.element);

    var d = this.currentDelta();
    if (revert && this.options.reverteffect) {
      if (dropped == 0 || revert != 'failure')
        this.options.reverteffect(this.element,
          d[1] - this.delta[1], d[0] - this.delta[0]);
    } else {
      this.delta = d;
    }

    if (this.options.zindex)
      this.element.style.zIndex = this.originalZ;

    if (this.options.endeffect)
      this.options.endeffect(this.element);

    Draggables.deactivate(this);
    Droppables.reset();
  },

  keyPress:function (event) {
    if (event.keyCode != Event.KEY_ESC) return;
    this.finishDrag(event, false);
    Event.stop(event);
  },

  endDrag:function (event) {
    if (!this.dragging) return;
    this.stopScrolling();
    this.finishDrag(event, true);
    Event.stop(event);
  },

  draw:function (point) {
    var pos = this.element.cumulativeOffset();
    if (this.options.ghosting) {
      var r = Position.realOffset(this.element);
      pos[0] += r[0] - Position.deltaX;
      pos[1] += r[1] - Position.deltaY;
    }

    var d = this.currentDelta();
    pos[0] -= d[0];
    pos[1] -= d[1];

    if (this.options.scroll && (this.options.scroll != window && this._isScrollChild)) {
      pos[0] -= this.options.scroll.scrollLeft - this.originalScrollLeft;
      pos[1] -= this.options.scroll.scrollTop - this.originalScrollTop;
    }

    var p = [0, 1].map(function (i) {
      return (point[i] - pos[i] - this.offset[i])
    }.bind(this));

    if (this.options.snap) {
      if (Object.isFunction(this.options.snap)) {
        p = this.options.snap(p[0], p[1], this);
      } else {
        if (Object.isArray(this.options.snap)) {
          p = p.map(function (v, i) {
            return (v / this.options.snap[i]).round() * this.options.snap[i]
          }.bind(this));
        } else {
          p = p.map(function (v) {
            return (v / this.options.snap).round() * this.options.snap
          }.bind(this));
        }
      }
    }

    var style = this.element.style;
    if ((!this.options.constraint) || (this.options.constraint == 'horizontal'))
      style.left = p[0] + "px";
    if ((!this.options.constraint) || (this.options.constraint == 'vertical'))
      style.top = p[1] + "px";

    if (style.visibility == "hidden") style.visibility = ""; // fix gecko rendering
  },

  stopScrolling:function () {
    if (this.scrollInterval) {
      clearInterval(this.scrollInterval);
      this.scrollInterval = null;
      Draggables._lastScrollPointer = null;
    }
  },

  startScrolling:function (speed) {
    if (!(speed[0] || speed[1])) return;
    this.scrollSpeed = [speed[0] * this.options.scrollSpeed, speed[1] * this.options.scrollSpeed];
    this.lastScrolled = new Date();
    this.scrollInterval = setInterval(this.scroll.bind(this), 10);
  },

  scroll:function () {
    var current = new Date();
    var delta = current - this.lastScrolled;
    this.lastScrolled = current;
    if (this.options.scroll == window) {
      with (this._getWindowScroll(this.options.scroll)) {
        if (this.scrollSpeed[0] || this.scrollSpeed[1]) {
          var d = delta / 1000;
          this.options.scroll.scrollTo(left + d * this.scrollSpeed[0], top + d * this.scrollSpeed[1]);
        }
      }
    } else {
      this.options.scroll.scrollLeft += this.scrollSpeed[0] * delta / 1000;
      this.options.scroll.scrollTop += this.scrollSpeed[1] * delta / 1000;
    }

    Position.prepare();
    Droppables.show(Draggables._lastPointer, this.element);
    Draggables.notify('onDrag', this);
    if (this._isScrollChild) {
      Draggables._lastScrollPointer = Draggables._lastScrollPointer || $A(Draggables._lastPointer);
      Draggables._lastScrollPointer[0] += this.scrollSpeed[0] * delta / 1000;
      Draggables._lastScrollPointer[1] += this.scrollSpeed[1] * delta / 1000;
      if (Draggables._lastScrollPointer[0] < 0)
        Draggables._lastScrollPointer[0] = 0;
      if (Draggables._lastScrollPointer[1] < 0)
        Draggables._lastScrollPointer[1] = 0;
      this.draw(Draggables._lastScrollPointer);
    }

    if (this.options.change) this.options.change(this);
  },

  _getWindowScroll:function (w) {
    var T, L, W, H;
    with (w.document) {
      if (w.document.documentElement && documentElement.scrollTop) {
        T = documentElement.scrollTop;
        L = documentElement.scrollLeft;
      } else if (w.document.body) {
        T = body.scrollTop;
        L = body.scrollLeft;
      }
      if (w.innerWidth) {
        W = w.innerWidth;
        H = w.innerHeight;
      } else if (w.document.documentElement && documentElement.clientWidth) {
        W = documentElement.clientWidth;
        H = documentElement.clientHeight;
      } else {
        W = body.offsetWidth;
        H = body.offsetHeight;
      }
    }
    return { top:T, left:L, width:W, height:H };
  }
});

Draggable._dragging = { };

/*--------------------------------------------------------------------------*/

var SortableObserver = Class.create({
  initialize:function (element, observer) {
    this.element = $(element);
    this.observer = observer;
    this.lastValue = Sortable.serialize(this.element);
  },

  onStart:function () {
    this.lastValue = Sortable.serialize(this.element);
  },

  onEnd:function () {
    Sortable.unmark();
    if (this.lastValue != Sortable.serialize(this.element))
      this.observer(this.element)
  }
});

var Sortable = {
  SERIALIZE_RULE:/^[^_\-](?:[A-Za-z0-9\-\_]*)[_](.*)$/,

  sortables:{ },

  _findRootElement:function (element) {
    while (element.tagName.toUpperCase() != "BODY") {
      if (element.id && Sortable.sortables[element.id]) return element;
      element = element.parentNode;
    }
  },

  options:function (element) {
    element = Sortable._findRootElement($(element));
    if (!element) return;
    return Sortable.sortables[element.id];
  },

  destroy:function (element) {
    element = $(element);
    var s = Sortable.sortables[element.id];

    if (s) {
      Draggables.removeObserver(s.element);
      s.droppables.each(function (d) {
        Droppables.remove(d)
      });
      s.draggables.invoke('destroy');

      delete Sortable.sortables[s.element.id];
    }
  },

  create:function (element) {
    element = $(element);
    var options = Object.extend({
      element:element,
      tag:'li', // assumes li children, override with tag: 'tagname'
      dropOnEmpty:false,
      tree:false,
      treeTag:'ul',
      overlap:'vertical', // one of 'vertical', 'horizontal'
      constraint:'vertical', // one of 'vertical', 'horizontal', false
      containment:element, // also takes array of elements (or id's); or false
      handle:false, // or a CSS class
      only:false,
      delay:0,
      hoverclass:null,
      ghosting:false,
      quiet:false,
      scroll:false,
      scrollSensitivity:20,
      scrollSpeed:15,
      format:this.SERIALIZE_RULE,

      // these take arrays of elements or ids and can be
      // used for better initialization performance
      elements:false,
      handles:false,

      onChange:Prototype.emptyFunction,
      onUpdate:Prototype.emptyFunction
    }, arguments[1] || { });

    // clear any old sortable with same element
    this.destroy(element);

    // build options for the draggables
    var options_for_draggable = {
      revert:true,
      quiet:options.quiet,
      scroll:options.scroll,
      scrollSpeed:options.scrollSpeed,
      scrollSensitivity:options.scrollSensitivity,
      delay:options.delay,
      ghosting:options.ghosting,
      constraint:options.constraint,
      handle:options.handle };

    if (options.starteffect)
      options_for_draggable.starteffect = options.starteffect;

    if (options.reverteffect)
      options_for_draggable.reverteffect = options.reverteffect;
    else
    if (options.ghosting) options_for_draggable.reverteffect = function (element) {
      element.style.top = 0;
      element.style.left = 0;
    };

    if (options.endeffect)
      options_for_draggable.endeffect = options.endeffect;

    if (options.zindex)
      options_for_draggable.zindex = options.zindex;

    // build options for the droppables
    var options_for_droppable = {
      overlap:options.overlap,
      containment:options.containment,
      tree:options.tree,
      hoverclass:options.hoverclass,
      onHover:Sortable.onHover
    };

    var options_for_tree = {
      onHover:Sortable.onEmptyHover,
      overlap:options.overlap,
      containment:options.containment,
      hoverclass:options.hoverclass
    };

    // fix for gecko engine
    Element.cleanWhitespace(element);

    options.draggables = [];
    options.droppables = [];

    // drop on empty handling
    if (options.dropOnEmpty || options.tree) {
      Droppables.add(element, options_for_tree);
      options.droppables.push(element);
    }

    (options.elements || this.findElements(element, options) || []).each(function (e, i) {
      var handle = options.handles ? $(options.handles[i]) :
        (options.handle ? $(e).select('.' + options.handle)[0] : e);
      options.draggables.push(
        new Draggable(e, Object.extend(options_for_draggable, { handle:handle })));
      Droppables.add(e, options_for_droppable);
      if (options.tree) e.treeNode = element;
      options.droppables.push(e);
    });

    if (options.tree) {
      (Sortable.findTreeElements(element, options) || []).each(function (e) {
        Droppables.add(e, options_for_tree);
        e.treeNode = element;
        options.droppables.push(e);
      });
    }

    // keep reference
    this.sortables[element.identify()] = options;

    // for onupdate
    Draggables.addObserver(new SortableObserver(element, options.onUpdate));

  },

  // return all suitable-for-sortable elements in a guaranteed order
  findElements:function (element, options) {
    return Element.findChildren(
      element, options.only, options.tree ? true : false, options.tag);
  },

  findTreeElements:function (element, options) {
    return Element.findChildren(
      element, options.only, options.tree ? true : false, options.treeTag);
  },

  onHover:function (element, dropon, overlap) {
    if (Element.isParent(dropon, element)) return;

    if (overlap > .33 && overlap < .66 && Sortable.options(dropon).tree) {
      return;
    } else if (overlap > 0.5) {
      Sortable.mark(dropon, 'before');
      if (dropon.previousSibling != element) {
        var oldParentNode = element.parentNode;
        element.style.visibility = "hidden"; // fix gecko rendering
        dropon.parentNode.insertBefore(element, dropon);
        if (dropon.parentNode != oldParentNode)
          Sortable.options(oldParentNode).onChange(element);
        Sortable.options(dropon.parentNode).onChange(element);
      }
    } else {
      Sortable.mark(dropon, 'after');
      var nextElement = dropon.nextSibling || null;
      if (nextElement != element) {
        var oldParentNode = element.parentNode;
        element.style.visibility = "hidden"; // fix gecko rendering
        dropon.parentNode.insertBefore(element, nextElement);
        if (dropon.parentNode != oldParentNode)
          Sortable.options(oldParentNode).onChange(element);
        Sortable.options(dropon.parentNode).onChange(element);
      }
    }
  },

  onEmptyHover:function (element, dropon, overlap) {
    var oldParentNode = element.parentNode;
    var droponOptions = Sortable.options(dropon);

    if (!Element.isParent(dropon, element)) {
      var index;

      var children = Sortable.findElements(dropon, {tag:droponOptions.tag, only:droponOptions.only});
      var child = null;

      if (children) {
        var offset = Element.offsetSize(dropon, droponOptions.overlap) * (1.0 - overlap);

        for (index = 0; index < children.length; index += 1) {
          if (offset - Element.offsetSize(children[index], droponOptions.overlap) >= 0) {
            offset -= Element.offsetSize(children[index], droponOptions.overlap);
          } else if (offset - (Element.offsetSize(children[index], droponOptions.overlap) / 2) >= 0) {
            child = index + 1 < children.length ? children[index + 1] : null;
            break;
          } else {
            child = children[index];
            break;
          }
        }
      }

      dropon.insertBefore(element, child);

      Sortable.options(oldParentNode).onChange(element);
      droponOptions.onChange(element);
    }
  },

  unmark:function () {
    if (Sortable._marker) Sortable._marker.hide();
  },

  mark:function (dropon, position) {
    // mark on ghosting only
    var sortable = Sortable.options(dropon.parentNode);
    if (sortable && !sortable.ghosting) return;

    if (!Sortable._marker) {
      Sortable._marker =
        ($('dropmarker') || Element.extend(document.createElement('DIV'))).
          hide().addClassName('dropmarker').setStyle({position:'absolute'});
      document.getElementsByTagName("body").item(0).appendChild(Sortable._marker);
    }
    var offsets = dropon.cumulativeOffset();
    Sortable._marker.setStyle({left:offsets[0] + 'px', top:offsets[1] + 'px'});

    if (position == 'after')
      if (sortable.overlap == 'horizontal')
        Sortable._marker.setStyle({left:(offsets[0] + dropon.clientWidth) + 'px'});
      else
        Sortable._marker.setStyle({top:(offsets[1] + dropon.clientHeight) + 'px'});

    Sortable._marker.show();
  },

  _tree:function (element, options, parent) {
    var children = Sortable.findElements(element, options) || [];

    for (var i = 0; i < children.length; ++i) {
      var match = children[i].id.match(options.format);

      if (!match) continue;

      var child = {
        id:encodeURIComponent(match ? match[1] : null),
        element:element,
        parent:parent,
        children:[],
        position:parent.children.length,
        container:$(children[i]).down(options.treeTag)
      };

      /* Get the element containing the children and recurse over it */
      if (child.container)
        this._tree(child.container, options, child);

      parent.children.push(child);
    }

    return parent;
  },

  tree:function (element) {
    element = $(element);
    var sortableOptions = this.options(element);
    var options = Object.extend({
      tag:sortableOptions.tag,
      treeTag:sortableOptions.treeTag,
      only:sortableOptions.only,
      name:element.id,
      format:sortableOptions.format
    }, arguments[1] || { });

    var root = {
      id:null,
      parent:null,
      children:[],
      container:element,
      position:0
    };

    return Sortable._tree(element, options, root);
  },

  /* Construct a [i] index for a particular node */
  _constructIndex:function (node) {
    var index = '';
    do {
      if (node.id) index = '[' + node.position + ']' + index;
    } while ((node = node.parent) != null);
    return index;
  },

  sequence:function (element) {
    element = $(element);
    var options = Object.extend(this.options(element), arguments[1] || { });

    return $(this.findElements(element, options) || []).map(function (item) {
      return item.id.match(options.format) ? item.id.match(options.format)[1] : '';
    });
  },

  setSequence:function (element, new_sequence) {
    element = $(element);
    var options = Object.extend(this.options(element), arguments[2] || { });

    var nodeMap = { };
    this.findElements(element, options).each(function (n) {
      if (n.id.match(options.format))
        nodeMap[n.id.match(options.format)[1]] = [n, n.parentNode];
      n.parentNode.removeChild(n);
    });

    new_sequence.each(function (ident) {
      var n = nodeMap[ident];
      if (n) {
        n[1].appendChild(n[0]);
        delete nodeMap[ident];
      }
    });
  },

  serialize:function (element) {
    element = $(element);
    var options = Object.extend(Sortable.options(element), arguments[1] || { });
    var name = encodeURIComponent(
      (arguments[1] && arguments[1].name) ? arguments[1].name : element.id);

    if (options.tree) {
      return Sortable.tree(element, arguments[1]).children.map(
        function (item) {
          return [name + Sortable._constructIndex(item) + "[id]=" +
            encodeURIComponent(item.id)].concat(item.children.map(arguments.callee));
        }).flatten().join('&');
    } else {
      return Sortable.sequence(element, arguments[1]).map(
        function (item) {
          return name + "[]=" + encodeURIComponent(item);
        }).join('&');
    }
  }
};

// Returns true if child is contained within element
Element.isParent = function (child, element) {
  if (!child.parentNode || child == element) return false;
  if (child.parentNode == element) return true;
  return Element.isParent(child.parentNode, element);
};

Element.findChildren = function (element, only, recursive, tagName) {
  if (!element.hasChildNodes()) return null;
  tagName = tagName.toUpperCase();
  if (only) only = [only].flatten();
  var elements = [];
  $A(element.childNodes).each(function (e) {
    if (e.tagName && e.tagName.toUpperCase() == tagName &&
      (!only || (Element.classNames(e).detect(function (v) {
        return only.include(v)
      }))))
      elements.push(e);
    if (recursive) {
      var grandchildren = Element.findChildren(e, only, recursive, tagName);
      if (grandchildren) elements.push(grandchildren);
    }
  });

  return (elements.length > 0 ? elements.flatten() : []);
};

Element.offsetSize = function (element, type) {
  return element['offset' + ((type == 'vertical' || type == 'height') ? 'Height' : 'Width')];
};