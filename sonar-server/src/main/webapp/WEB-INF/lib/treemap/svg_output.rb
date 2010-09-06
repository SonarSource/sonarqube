#
# svg_output.rb - RubyTreemap
#
# Copyright (c) 2006 by Andrew Bruno <aeb@qnot.org>
#
# This program is free software; you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation; either version 2 of the License, or
# (at your option) any later version.
#
#

require 'cgi'
require 'RMagick'

require File.dirname(__FILE__) + "/output_base"

class Treemap::SvgOutput < Treemap::OutputBase

    def initialize
        super

        yield self if block_given?
    end

    def node_label(node)
        CGI.escapeHTML(node.label)
    end

    def node_color(node)
        color = "#CCCCCC"

        if(!node.color.nil?)
            if(not Numeric === node.color)
                color = node.color
            else
                color = "#" + @color.get_hex_color(node.color)
            end
        end

        color
    end

    def to_png(node, filename="treemap.png")
        svg = to_svg(node)
        img = Magick::Image.from_blob(svg) { self.format = "SVG" }
        img[0].write(filename)
    end

    def to_svg(node)
        bounds = self.bounds

        @layout.process(node, bounds)

        draw_map(node)
    end

    def draw_map(node)
        svg = "<svg"
        svg += " xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\""
        svg += " xmlns:ev=\"http://www.w3.org/2001/xml-events\"" 
        svg += " width=\"" + (node.bounds.width).to_s + "\" height=\"" + (node.bounds.height).to_s + "\">"

        svg += draw_node(node)
        svg += "</svg>"
        svg
    end

    def draw_node(node)
        return "" if node.bounds.nil?

        svg = ""
        svg += "<rect"
        svg += " id=\"rect-" + node.id.to_s + "\""
        svg += " x=\"" + node.bounds.x1.to_s + "\""
        svg += " y=\"" + node.bounds.y1.to_s + "\""
        svg += " width=\"" + node.bounds.width.to_s + "\""
        svg += " height=\"" + node.bounds.height.to_s + "\""
        #svg += " style=\""
        #svg += " fill: " + node_color(node) + ";"
        #svg += " stroke: #000000;"
        #svg += " stroke-width: 1px;"
        svg += " fill=\"" + node_color(node) + "\""
        svg += " stroke=\"#000000\""
        svg += " stroke-width=\"1px\""
        svg += " />"

        if(!node.children.nil? and node.children.size > 0)
            node.children.each do |c|
                svg += draw_node(c)
            end
        end

        svg
    end
end
