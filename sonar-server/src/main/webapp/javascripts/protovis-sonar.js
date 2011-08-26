window.SonarWidgets = {}

SonarWidgets.Timeline = function (divId) {
	this.wDivId = divId;
	this.wHeight;
	this.wData;
	this.wSnapshots;
	this.wMetrics;
	this.wTranslations;
	this.wEvents;
	this.height = function(height) {
		this.wHeight = height;
		return this;
	}
	this.data = function(data) {
		this.wData = data;
		return this;
	}
	this.snapshots = function(snapshots) {
		this.wSnapshots = snapshots;
		return this;
	}
	this.metrics = function(metrics) {
		this.wMetrics = metrics;
		return this;
	}
	this.translations = function(translations) {
		this.wTranslations = translations;
		return this;
	}
	this.events = function(events) {
		this.wEvents = events;
		return this;
	}
}

SonarWidgets.Timeline.prototype.render = function() {
	
	var trendData = this.wData;
	var metrics = this.wMetrics;
	var snapshots = this.wSnapshots;
	var translations = this.wTranslations;
	var events = this.wEvents;
	var widgetDiv = document.getElementById(this.wDivId);
	
	var footerFont = "12px Arial,Helvetica,sans-serif";
	
	/* Sizing and scales. */
	var leftMargin = 20;
	var show_y_axis = (data.length==1)
	if (show_y_axis) {
		// We must evaluate how wide the left margin must be, depending on the values that we get (so that they can be displayed correctly)
		var maxNumberOnY = 0;
		for each (var dataArray in trendData) {
			for each (var d in dataArray) {
				if (d.y > maxNumberOnY) maxNumberOnY = d.y;
			}
		}
		minMargin = (maxNumberOnY + "").length * 7
		if (minMargin > leftMargin) leftMargin = minMargin
	}	
	var footerHeight = 30 + (events ? 3 : this.wMetrics.size()) * 12;
	var w = widgetDiv.parentNode.clientWidth - leftMargin - 30, 
		h = (this.wHeight == null ? 80 : this.wHeight) + footerHeight - 5,
		S=2;

	var x = pv.Scale.linear(pv.blend(pv.map(data, function(d) {return d;})), function(d) {return d.x}).range(0, w);
	var y = new Array(data.length);
	for(var i = 0; i < data.length; i++){
		y[i]=pv.Scale.linear(data[i], function(d) {return d.y;}).range(20, h-10)
	}
	var interpolate = "linear"; /* cardinal or linear */
	var idx = this.wData[0].size() - 1;

	/* The root panel. */
	var vis = new pv.Panel()
		.canvas(widgetDiv)
		.width(w)
		.height(h)
		.left(leftMargin)
		.right(20)
		.bottom(footerHeight)
		.top(5)
		.strokeStyle("#CCC");

	/* X-axis */
	vis.add(pv.Rule)
		.data(x.ticks())
		.left(x)
		.bottom(-5)
		.height(5)
		.anchor("bottom")
		.add(pv.Label)
		.text(x.tickFormat);

	/* Y-axis and ticks. */
	if (show_y_axis) { 
		vis.add(pv.Rule)
		.data(y[0].ticks(5))
		.bottom(y[0])
		.strokeStyle(function(d) {return d ? "#eee" : "#000";})
		.anchor("left")
		.add(pv.Label)
		.text(y[0].tickFormat); 
	}

	/* A panel for each data series. */
	var panel = vis.add(pv.Panel)
		.data(trendData);

	/* The line. */
	var line = panel.add(pv.Line)
		.data(function(array) {return array;})
		.left(function(d) {return x(d.x);})
		.bottom(function(d) {return y[this.parent.index](d.y);})
		.interpolate(function() {return interpolate;})
		.lineWidth(2);

	/* The mouseover dots and label in footer. */
	line.add(pv.Dot)
		.data(function(d) {return [d[idx]];})
		.fillStyle(function() {return line.strokeStyle();})
		.strokeStyle("#000")
		.size(20) 
		.lineWidth(1)
		.add(pv.Dot)
		.left(0)
		.bottom(function() {return 0 - 30 - this.parent.index * 14;})
		.anchor("right").add(pv.Label)
		.font(footerFont)
		.text(function(d) {return metrics[this.parent.index] + ": " + d.y.toFixed(2);});
	
	/* The date of the selected dot in footer. */
	vis.add(pv.Label)
		.left(w/2)
		.bottom(-36)
		.font(footerFont)
		.text(function() {return translations.date + ": " + snapshots[idx].d;});
	
	/* The event labels */
	if (events) {
	  eventColor = "rgba(75,159,213,1)";
	  eventHoverColor = "rgba(202,227,242,1)";
	  vis.add(pv.Line)
		.strokeStyle("rgba(0,0,0,.001)")
		.data(events)
		.left(function(e) {return x(e.d);})
		.bottom(0)
		.anchor("top")
		.add(pv.Dot)
		.bottom(6)
		.shape("triangle")
		.strokeStyle("grey")
		.fillStyle(function(e) {return e.l[0].ld == snapshots[idx].d ? eventHoverColor : eventColor})
		.add(pv.Dot)
		.visible(function(e) { return e.l[0].ld == snapshots[idx].d;})
		.left(w/2+8)
		.bottom(-45)
		.shape("triangle")
		.fillStyle(function(e) {return e.l[0].ld == snapshots[idx].d ? eventHoverColor : eventColor})
		.strokeStyle("grey")
		.anchor("right")
		.add(pv.Label)
		.font(footerFont)
		.text(function(e) {return e.l[0].n + ( e.l[1] ? " (... +" + (e.l.size()-1) + ")" : "");});
	}
	
	/* An invisible bar to capture events (without flickering). */
	vis.add(pv.Bar)
		.fillStyle("rgba(0,0,0,.001)")
		.width(w+10)
		.event("mouseout", function() {
			i = -1;
			return vis;
		})
		.event("mousemove", function() {
			var mx = x.invert(vis.mouse().x);
			idx = pv.search(data[0].map(function(d) {return d.x;}), mx);
			idx = idx < 0 ? (-idx - 2) : idx;
			idx = idx < 0 ? 0 : idx;
			return vis;
		});
	
	vis.render();
	
}