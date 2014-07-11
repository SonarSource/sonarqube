#
# slice_layout.rb - RubyTreemap
#
# Copyright (c) 2006 by Andrew Bruno <aeb@qnot.org>
#
# This program is free software; you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation; either version 2 of the License, or
# (at your option) any later version.
#
#

require File.dirname(__FILE__) + "/layout_base"

class Treemap::SliceLayout < Treemap::LayoutBase
    Vertical = 1
    Horizontal = 2

    def process(node, bounds, axis=nil)
        bounds = bounds.clone

        node.bounds = bounds.clone

        if(@position == :absolute) 
            bounds.x2 = bounds.width
            bounds.y2 = bounds.height
            bounds.x1 = 0
            bounds.y1 = 0
        end

        if(!node.leaf?)
            process_children(node.children, bounds, axis)
        end
    end

    def sum(children)
        sum = 0
        children.each do |c|
            sum += c.size
        end
        sum
    end

    def process_children(children, bounds, axis=nil)
        parent_bounds = bounds.clone
        bounds = bounds.clone

        axis = axis(bounds) if(axis.nil?)

        width = axis == Vertical ? bounds.width : bounds.height

        sum = sum(children)

        return if sum<=0

        # XXX should we sort? seems to produce better map but not tested on 
        # larger data sets
        # children.sort {|c1,c2| c2.size <=> c1.size}.each do |c|
        children.each do |c|
            size = ((c.size.to_f / sum.to_f)*width).round

            if(axis == Vertical) 
                bounds.x2 = bounds.x1 + size
            else
                bounds.y2 = bounds.y1 + size
            end

            process(c, bounds, flip(axis))

            axis == Vertical ? bounds.x1 = bounds.x2 : bounds.y1 = bounds.y2
        end

        last = children.last
        if(axis == Vertical)
            last.bounds.x2 = parent_bounds.x2
        else
           last.bounds.y2 = parent_bounds.y2
        end

    end

    def flip(axis)
        return Horizontal if axis == Vertical
        Vertical
    end
    
    def vertical?(bounds)
        bounds.width > bounds.height
    end

    def horizontal?(bounds)
        bounds.width < bounds.height
    end

    def axis(bounds)
        return Horizontal if horizontal?(bounds)
        Vertical
    end
end
