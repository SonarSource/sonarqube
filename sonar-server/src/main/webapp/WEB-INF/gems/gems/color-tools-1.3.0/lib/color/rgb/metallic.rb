#--
# Colour management with Ruby.
#
# Copyright 2005 Austin Ziegler
#   http://rubyforge.org/ruby-pdf/
#
#   Licensed under a MIT-style licence.
#
# $Id: metallic.rb,v 1.1 2005/08/05 23:07:20 austin Exp $
#++

  # This namespace contains some RGB metallic colours suggested by Jim Freeze.
module Color::RGB::Metallic
  Aluminum    = Color::RGB.new(0x99, 0x99, 0x99)
  CoolCopper  = Color::RGB.new(0xd9, 0x87, 0x19)
  Copper      = Color::RGB.new(0xb8, 0x73, 0x33)
  Iron        = Color::RGB.new(0x4c, 0x4c, 0x4c)
  Lead        = Color::RGB.new(0x19, 0x19, 0x19)
  Magnesium   = Color::RGB.new(0xb3, 0xb3, 0xb3)
  Mercury     = Color::RGB.new(0xe6, 0xe6, 0xe6)
  Nickel      = Color::RGB.new(0x80, 0x80, 0x80)
  PolySilicon = Color::RGB.new(0x60, 0x00, 0x00)
  Poly        = PolySilicon
  Silver      = Color::RGB.new(0xcc, 0xcc, 0xcc)
  Steel       = Color::RGB.new(0x66, 0x66, 0x66)
  Tin         = Color::RGB.new(0x7f, 0x7f, 0x7f)
  Tungsten    = Color::RGB.new(0x33, 0x33, 0x33)
end
