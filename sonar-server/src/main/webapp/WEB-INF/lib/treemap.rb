#
# treemap.rb - RubyTreemap
#
# Copyright (c) 2006 by Andrew Bruno <aeb@qnot.org>
#
# This program is free software; you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation; either version 2 of the License, or
# (at your option) any later version.
#
#

require File.dirname(__FILE__) + '/treemap/node'
require File.dirname(__FILE__) + '/treemap/layout_base'
require File.dirname(__FILE__) + '/treemap/output_base'
require File.dirname(__FILE__) + '/treemap/color_base'
require File.dirname(__FILE__) + '/treemap/slice_layout'
require File.dirname(__FILE__) + '/treemap/squarified_layout'
require File.dirname(__FILE__) + '/treemap/html_output'
require File.dirname(__FILE__) + '/treemap/rectangle'
require File.dirname(__FILE__) + '/treemap/gradient_color'

# XXX these are still expirmental. Requires RMagick
# require File.dirname(__FILE__) + '/treemap/image_output'
# require File.dirname(__FILE__) + '/treemap/svg_output'

module Treemap
    VERSION = "0.0.1"

    def Treemap::dump_tree(node)
        puts "#{node.label}: #{node.bounds.to_s}"
        node.children.each do |c|
            dump_tree(c)
        end
    end

    def Treemap::tree_from_xml(file)
        doc = REXML::Document.new(file)
        node_from_xml(doc.root)
    end

    def Treemap::node_from_xml(xmlnode)
        node = Treemap::Node.new

        node.label = xmlnode.attributes["label"]
        id = xmlnode.attributes["id"]
        if(!id.nil?)
            node.id = id.to_s
        end

        node.size = xmlnode.attributes["size"]
        node.size = node.size.to_f unless node.size.nil?

        node.color = xmlnode.attributes["change"]
        node.color = node.color.to_f unless node.color.nil?


        xmlnode.elements.each do |c|
            child = node_from_xml(c)
            node.add_child(child) if !child.nil?
        end

        return nil if node.size < 5
        node
    end
end
