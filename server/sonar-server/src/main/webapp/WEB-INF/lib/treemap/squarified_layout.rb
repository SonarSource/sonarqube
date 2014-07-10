#
# squarified_layout.rb - RubyTreemap
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

class Treemap::SquarifiedLayout < Treemap::SliceLayout

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
            squarify_children(node, bounds, flip(axis))
        end
    end

    def squarify_children(node, bounds, axis)

        parent_bounds = bounds.clone
        bounds = bounds.clone

        node.children.sort! {|a,b| b.size <=> a.size}

        if(node.children.size < 2)
            process_children(node.children, bounds, flip(axis))
        end

        parent_size = node.size
        first_child = node.children.first

        row_size = first_child.size
        row_max = row_size.to_f / parent_size.to_f
        total = row_max 

        prev_aspect = aspect_ratio(bounds, first_child.size.to_f / row_size.to_f, total, axis)
        row = [first_child]

        node.children[1 .. node.children.size-1].each do |c|
            child_prop = c.size.to_f / parent_size.to_f
            aspect = aspect_ratio(bounds, c.size.to_f / row_size.to_f, total + child_prop, axis)

            if(aspect > prev_aspect)
                newb = bounds.clone
                if(axis == Vertical)
                    newb.x2 = bounds.x1 + ((bounds.width * total)).round
                else 
                    newb.y2 = bounds.y1 + ((bounds.height * total)).round
                end

                process_children(row, newb, flip(axis))

                if(axis == Vertical)
                    bounds.x1 = newb.x2
                else 
                    bounds.y1 = newb.y2
                end

                axis = flip(axis)
                parent_size -= row_size
                row_size = c.size
                total = row_max = row_size.to_f / parent_size.to_f
                prev_aspect = aspect_ratio(bounds, c.size.to_f / row_size.to_f, total, axis)
                row = [c]
            else
                row_size += c.size
                total += child_prop
                prev_aspect = aspect
                row.push(c)
            end
        end

        process_children(row, bounds, flip(axis))
    end

    def aspect_ratio(bounds, node_prop, row_prop, axis)
        height = bounds.height * row_prop
        width = bounds.width * node_prop
        if(axis == Vertical)
            width = bounds.width * row_prop
            height = bounds.height * node_prop
        end 

        return 0 if width == 0 and height == 0

        a = 0;
        b = 0;
        if(width > 0)
            a = height.to_f / width.to_f
        end
        if(height > 0)
            b = width.to_f / height.to_f
        end

        ratio = [a, b].max

        ratio
    end

    def axis(bounds)
        # XXX experiment with switching
        # axis = super(bounds)
        # flip(axis)
    end

    # XXX another way of computing the aspect ratio
    def aspect_ratio_method2(bounds, max, proportion, axis)

        large = bounds.height
        small = bounds.width
        if(axis == Vertical)
            large = bounds.width
            small = bounds.height
        end 

        ratio = (large * proportion).to_f / ((small * max).to_f / proportion.to_f).to_f

        if(ratio < 1)
            ratio = 1.to_f / ratio.to_f
        end

        ratio
    end
end
