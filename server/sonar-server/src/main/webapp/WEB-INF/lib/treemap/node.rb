#
# node.rb - RubyTreemap
#
# Copyright (c) 2006 by Andrew Bruno <aeb@qnot.org>
#
# This program is free software; you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation; either version 2 of the License, or
# (at your option) any later version.
#
#

require "md5"

module Treemap
    #
    # A generic tree node class which is used to represent the data to be
    # treemap'ed. The Layout and Output classes expect an object of this
    # type to perform the treemap calculations on.
    #
    # Create a simple tree:
    #    root = Treemap::Node.new
    #    root.new_child(:size => 6)
    #    root.new_child(:size => 6)
    #    root.new_child(:size => 4)
    #    root.new_child(:size => 3)
    #    root.new_child(:size => 2)
    #    root.new_child(:size => 2)
    #    root.new_child(:size => 1)
    #
    # Initialize values:
    #    root = Treemap::Node.new(:label => "All", :size => 100, :color => "#FFCCFF")
    #    child1 = Treemap::Node.new(:label => "Child 1", :size => 50)
    #    child2 = Treemap::Node.new(:label => "Child 2", :size => 50)
    #    root.add_child(child1)
    #    root.add_child(child2)
    #
    #
    class Treemap::Node
        attr_accessor :id, :label, :color, :size, :bounds, :parent, :tooltip, :url, :title, :rid, :leaf
        attr_reader :children

        #
        # Create a new Node. You can initialize the node by passing in
        # a hash with any of the following keys:
        #
        # * :size - The size that this node represents. For non-leaf nodes the 
        #   size must be equal to the sum of the sizes of it's children. If size
        #   is nil then the value will be calculated by recursing the children.
        # * :label - The label for this node. Used when displaying. Defaults to "node"
        # * :color - The background fill color in hex to render when drawing the 
        #   square. If the value is a number a color will be calculated. An example
        #   string color would be: ##FFFFFF (white)
        # * :id - a unique id to assign to this node. Default id will be generated if
        #   one is not provided.
        #
        #
        def initialize(opts = {})
            @size = opts[:size]
            @label = opts[:label]
            @title = opts[:title]
            @color = opts[:color]
            @id = opts[:id]
            @url = opts[:url]
            @tooltip = opts[:tooltip]
            @children = []
            @rid = opts[:rid]
            @leaf = opts[:leaf]
            if(@id.nil?)
                make_id
            end
        end

        #
        # Returns the depth of the node. 0 for root.
        #
        def depth
            return 0 if parent.nil?
            1 + self.parent.depth
        end

        def add_child(node)
            # XXX check to see that a node with the same label doesn't already exist.
            #     having 2 nodes with the same label at the same depth 
            #     doesn't seem to make sense
            node.parent = self
            @children.push(node)
        end

        #
        # Creates a new node and adds it as a child. See new method.
        #
        def new_child(*args)
            node = Treemap::Node.new(*args)
            self.add_child(node)
        end

        def find
            @children.find { |c| yield(c) }
        end

        def to_s
            str = "[:id => " + id + " :label => " + label
            str += " :size => " + size.to_s + " :color => " + color.to_s
            if(not(bounds.nil?))
                str += " :bounds => " + bounds.to_s
            end
            str += "]"
            str
        end

        def size
            return @size if !@size.nil?
            sum = 0
            @children.each do |c|
                sum += c.size
            end

            sum
        end

        def label
            return @label if !@label.nil?
            "node - " + size.to_s
        end

        def leaf?
            return true if @children.nil?
            !(@children.size > 0)
        end

    private
        def make_id
            #XXX prob should change this. Create a better way to generate unique id's
            @id = MD5.new([self.label, rand(100000000)].join("-")).hexdigest
        end
    end
end
