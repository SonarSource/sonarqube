#--
# Colour management with Ruby.
#
# Copyright 2005 Austin Ziegler
#   http://rubyforge.org/ruby-pdf/
#
#   Licensed under a MIT-style licence.
#
# $Id: rgb-colors.rb,v 1.1 2005/08/05 23:07:20 austin Exp $
#++

class Color::RGB
  AliceBlue             = Color::RGB.new(0xf0, 0xf8, 0xff).freeze
  AntiqueWhite          = Color::RGB.new(0xfa, 0xeb, 0xd7).freeze
  Aqua                  = Color::RGB.new(0x00, 0xff, 0xff).freeze
  Aquamarine            = Color::RGB.new(0x7f, 0xff, 0xd4).freeze
  Azure                 = Color::RGB.new(0xf0, 0xff, 0xff).freeze
  Beige                 = Color::RGB.new(0xf5, 0xf5, 0xdc).freeze
  Bisque                = Color::RGB.new(0xff, 0xe4, 0xc4).freeze
  Black                 = Color::RGB.new(0, 0, 0).freeze
  BlanchedAlmond        = Color::RGB.new(0xff, 0xeb, 0xcd).freeze
  Blue                  = Color::RGB.new(0x00, 0x00, 0xff).freeze
  BlueViolet            = Color::RGB.new(0x8a, 0x2b, 0xe2).freeze
  Brown                 = Color::RGB.new(0xa5, 0x2a, 0x2a).freeze
  Burlywood             = Color::RGB.new(0xde, 0xb8, 0x87).freeze
  BurlyWood             = Burlywood
  CadetBlue             = Color::RGB.new(0x5f, 0x9e, 0xa0).freeze
  Chartreuse            = Color::RGB.new(0x7f, 0xff, 0x00).freeze
  Chocolate             = Color::RGB.new(0xd2, 0x69, 0x1e).freeze
  Coral                 = Color::RGB.new(0xff, 0x7f, 0x50).freeze
  CornflowerBlue        = Color::RGB.new(0x64, 0x95, 0xed).freeze
  Cornsilk              = Color::RGB.new(0xff, 0xf8, 0xdc).freeze
  Crimson               = Color::RGB.new(0xdc, 0x14, 0x3c).freeze
  Cyan                  = Color::RGB.new(0x00, 0xff, 0xff).freeze
  DarkBlue              = Color::RGB.new(0x00, 0x00, 0x8b).freeze
  DarkCyan              = Color::RGB.new(0x00, 0x8b, 0x8b).freeze
  DarkGoldenrod         = Color::RGB.new(0xb8, 0x86, 0x0b).freeze
  DarkGoldenRod         = DarkGoldenrod
  DarkGray              = Color::RGB.new(0xa9, 0xa9, 0xa9).freeze
  DarkGreen             = Color::RGB.new(0x00, 0x64, 0x00).freeze
  DarkGrey              = DarkGray
  DarkKhaki             = Color::RGB.new(0xbd, 0xb7, 0x6b).freeze
  DarkMagenta           = Color::RGB.new(0x8b, 0x00, 0x8b).freeze
  DarkoliveGreen        = Color::RGB.new(0x55, 0x6b, 0x2f).freeze
  DarkOliveGreen        = DarkoliveGreen
  Darkorange            = Color::RGB.new(0xff, 0x8c, 0x00).freeze
  DarkOrange            = Darkorange
  DarkOrchid            = Color::RGB.new(0x99, 0x32, 0xcc).freeze
  DarkRed               = Color::RGB.new(0x8b, 0x00, 0x00).freeze
  Darksalmon            = Color::RGB.new(0xe9, 0x96, 0x7a).freeze
  DarkSalmon            = Darksalmon
  DarkSeaGreen          = Color::RGB.new(0x8f, 0xbc, 0x8f).freeze
  DarkSlateBlue         = Color::RGB.new(0x48, 0x3d, 0x8b).freeze
  DarkSlateGray         = Color::RGB.new(0x2f, 0x4f, 0x4f).freeze
  DarkSlateGrey         = DarkSlateGray
  DarkTurquoise         = Color::RGB.new(0x00, 0xce, 0xd1).freeze
  DarkViolet            = Color::RGB.new(0x94, 0x00, 0xd3).freeze
  DeepPink              = Color::RGB.new(0xff, 0x14, 0x93).freeze
  DeepSkyBlue           = Color::RGB.new(0x00, 0xbf, 0xbf).freeze
  DimGray               = Color::RGB.new(0x69, 0x69, 0x69).freeze
  DimGrey               = DimGray
  DodgerBlue            = Color::RGB.new(0x1e, 0x90, 0xff).freeze
  Feldspar              = Color::RGB.new(0xd1, 0x92, 0x75).freeze
  Firebrick             = Color::RGB.new(0xb2, 0x22, 0x22).freeze
  FireBrick             = Firebrick
  FloralWhite           = Color::RGB.new(0xff, 0xfa, 0xf0).freeze
  ForestGreen           = Color::RGB.new(0x22, 0x8b, 0x22).freeze
  Fuchsia               = Color::RGB.new(0xff, 0x00, 0xff).freeze
  Gainsboro             = Color::RGB.new(0xdc, 0xdc, 0xdc).freeze
  GhostWhite            = Color::RGB.new(0xf8, 0xf8, 0xff).freeze
  Gold                  = Color::RGB.new(0xff, 0xd7, 0x00).freeze
  Goldenrod             = Color::RGB.new(0xda, 0xa5, 0x20).freeze
  GoldenRod             = Goldenrod
  Gray                  = Color::RGB.new(0x80, 0x80, 0x80).freeze
  Green                 = Color::RGB.new(0x00, 0x80, 0x00).freeze
  GreenYellow           = Color::RGB.new(0xad, 0xff, 0x2f).freeze
  Grey                  = Gray
  Honeydew              = Color::RGB.new(0xf0, 0xff, 0xf0).freeze
  HoneyDew              = Honeydew
  HotPink               = Color::RGB.new(0xff, 0x69, 0xb4).freeze
  IndianRed             = Color::RGB.new(0xcd, 0x5c, 0x5c).freeze
  Indigo                = Color::RGB.new(0x4b, 0x00, 0x82).freeze
  Ivory                 = Color::RGB.new(0xff, 0xff, 0xf0).freeze
  Khaki                 = Color::RGB.new(0xf0, 0xe6, 0x8c).freeze
  Lavender              = Color::RGB.new(0xe6, 0xe6, 0xfa).freeze
  LavenderBlush         = Color::RGB.new(0xff, 0xf0, 0xf5).freeze
  LawnGreen             = Color::RGB.new(0x7c, 0xfc, 0x00).freeze
  LemonChiffon          = Color::RGB.new(0xff, 0xfa, 0xcd).freeze
  LightBlue             = Color::RGB.new(0xad, 0xd8, 0xe6).freeze
  LightCoral            = Color::RGB.new(0xf0, 0x80, 0x80).freeze
  LightCyan             = Color::RGB.new(0xe0, 0xff, 0xff).freeze
  LightGoldenrodYellow  = Color::RGB.new(0xfa, 0xfa, 0xd2).freeze
  LightGoldenRodYellow  = LightGoldenrodYellow
  LightGray             = Color::RGB.new(0xd3, 0xd3, 0xd3).freeze
  LightGreen            = Color::RGB.new(0x90, 0xee, 0x90).freeze
  LightGrey             = LightGray
  LightPink             = Color::RGB.new(0xff, 0xb6, 0xc1).freeze
  Lightsalmon           = Color::RGB.new(0xff, 0xa0, 0x7a).freeze
  LightSalmon           = Lightsalmon
  LightSeaGreen         = Color::RGB.new(0x20, 0xb2, 0xaa).freeze
  LightSkyBlue          = Color::RGB.new(0x87, 0xce, 0xfa).freeze
  LightSlateBlue        = Color::RGB.new(0x84, 0x70, 0xff).freeze
  LightSlateGray        = Color::RGB.new(0x77, 0x88, 0x99).freeze
  LightSlateGrey        = LightSlateGray
  LightsteelBlue        = Color::RGB.new(0xb0, 0xc4, 0xde).freeze
  LightSteelBlue        = LightsteelBlue
  LightYellow           = Color::RGB.new(0xff, 0xff, 0xe0).freeze
  Lime                  = Color::RGB.new(0x00, 0xff, 0x00).freeze
  LimeGreen             = Color::RGB.new(0x32, 0xcd, 0x32).freeze
  Linen                 = Color::RGB.new(0xfa, 0xf0, 0xe6).freeze
  Magenta               = Color::RGB.new(0xff, 0x00, 0xff).freeze
  Maroon                = Color::RGB.new(0x80, 0x00, 0x00).freeze
  MediumAquamarine      = Color::RGB.new(0x66, 0xcd, 0xaa).freeze
  MediumAquaMarine      = MediumAquamarine
  MediumBlue            = Color::RGB.new(0x00, 0x00, 0xcd).freeze
  MediumOrchid          = Color::RGB.new(0xba, 0x55, 0xd3).freeze
  MediumPurple          = Color::RGB.new(0x93, 0x70, 0xdb).freeze
  MediumSeaGreen        = Color::RGB.new(0x3c, 0xb3, 0x71).freeze
  MediumSlateBlue       = Color::RGB.new(0x7b, 0x68, 0xee).freeze
  MediumSpringGreen     = Color::RGB.new(0x00, 0xfa, 0x9a).freeze
  MediumTurquoise       = Color::RGB.new(0x48, 0xd1, 0xcc).freeze
  MediumVioletRed       = Color::RGB.new(0xc7, 0x15, 0x85).freeze
  MidnightBlue          = Color::RGB.new(0x19, 0x19, 0x70).freeze
  MintCream             = Color::RGB.new(0xf5, 0xff, 0xfa).freeze
  MistyRose             = Color::RGB.new(0xff, 0xe4, 0xe1).freeze
  Moccasin              = Color::RGB.new(0xff, 0xe4, 0xb5).freeze
  NavajoWhite           = Color::RGB.new(0xff, 0xde, 0xad).freeze
  Navy                  = Color::RGB.new(0x00, 0x00, 0x80).freeze
  OldLace               = Color::RGB.new(0xfd, 0xf5, 0xe6).freeze
  Olive                 = Color::RGB.new(0x80, 0x80, 0x00).freeze
  Olivedrab             = Color::RGB.new(0x6b, 0x8e, 0x23).freeze
  OliveDrab             = Olivedrab
  Orange                = Color::RGB.new(0xff, 0xa5, 0x00).freeze
  OrangeRed             = Color::RGB.new(0xff, 0x45, 0x00).freeze
  Orchid                = Color::RGB.new(0xda, 0x70, 0xd6).freeze
  PaleGoldenrod         = Color::RGB.new(0xee, 0xe8, 0xaa).freeze
  PaleGoldenRod         = PaleGoldenrod
  PaleGreen             = Color::RGB.new(0x98, 0xfb, 0x98).freeze
  PaleTurquoise         = Color::RGB.new(0xaf, 0xee, 0xee).freeze
  PaleVioletRed         = Color::RGB.new(0xdb, 0x70, 0x93).freeze
  PapayaWhip            = Color::RGB.new(0xff, 0xef, 0xd5).freeze
  Peachpuff             = Color::RGB.new(0xff, 0xda, 0xb9).freeze
  PeachPuff             = Peachpuff
  Peru                  = Color::RGB.new(0xcd, 0x85, 0x3f).freeze
  Pink                  = Color::RGB.new(0xff, 0xc0, 0xcb).freeze
  Plum                  = Color::RGB.new(0xdd, 0xa0, 0xdd).freeze
  PowderBlue            = Color::RGB.new(0xb0, 0xe0, 0xe6).freeze
  Purple                = Color::RGB.new(0x80, 0x00, 0x80).freeze
  Red                   = Color::RGB.new(0xff, 0x00, 0x00).freeze
  RosyBrown             = Color::RGB.new(0xbc, 0x8f, 0x8f).freeze
  RoyalBlue             = Color::RGB.new(0x41, 0x69, 0xe1).freeze
  SaddleBrown           = Color::RGB.new(0x8b, 0x45, 0x13).freeze
  Salmon                = Color::RGB.new(0xfa, 0x80, 0x72).freeze
  SandyBrown            = Color::RGB.new(0xf4, 0xa4, 0x60).freeze
  SeaGreen              = Color::RGB.new(0x2e, 0x8b, 0x57).freeze
  Seashell              = Color::RGB.new(0xff, 0xf5, 0xee).freeze
  SeaShell              = Seashell
  Sienna                = Color::RGB.new(0xa0, 0x52, 0x2d).freeze
  Silver                = Color::RGB.new(0xc0, 0xc0, 0xc0).freeze
  SkyBlue               = Color::RGB.new(0x87, 0xce, 0xeb).freeze
  SlateBlue             = Color::RGB.new(0x6a, 0x5a, 0xcd).freeze
  SlateGray             = Color::RGB.new(0x70, 0x80, 0x90).freeze
  SlateGrey             = SlateGray
  Snow                  = Color::RGB.new(0xff, 0xfa, 0xfa).freeze
  SpringGreen           = Color::RGB.new(0x00, 0xff, 0x7f).freeze
  SteelBlue             = Color::RGB.new(0x46, 0x82, 0xb4).freeze
  Tan                   = Color::RGB.new(0xd2, 0xb4, 0x8c).freeze
  Teal                  = Color::RGB.new(0x00, 0x80, 0x80).freeze
  Thistle               = Color::RGB.new(0xd8, 0xbf, 0xd8).freeze
  Tomato                = Color::RGB.new(0xff, 0x63, 0x47).freeze
  Turquoise             = Color::RGB.new(0x40, 0xe0, 0xd0).freeze
  Violet                = Color::RGB.new(0xee, 0x82, 0xee).freeze
  VioletRed             = Color::RGB.new(0xd0, 0x20, 0x90).freeze
  Wheat                 = Color::RGB.new(0xf5, 0xde, 0xb3).freeze
  White                 = Color::RGB.new(0xff, 0xff, 0xff).freeze
  WhiteSmoke            = Color::RGB.new(0xf5, 0xf5, 0xf5).freeze
  Yellow                = Color::RGB.new(0xff, 0xff, 0x00).freeze
  YellowGreen           = Color::RGB.new(0x9a, 0xcd, 0x32).freeze

  Gray10 = Grey10       = Color::RGB.from_percentage(10, 10, 10).freeze
  Gray20 = Grey20       = Color::RGB.from_percentage(20, 20, 20).freeze
  Gray30 = Grey30       = Color::RGB.from_percentage(30, 30, 30).freeze
  Gray40 = Grey40       = Color::RGB.from_percentage(40, 40, 40).freeze
  Gray50 = Grey50       = Color::RGB.from_percentage(50, 50, 50).freeze
  Gray60 = Grey60       = Color::RGB.from_percentage(60, 60, 60).freeze
  Gray70 = Grey70       = Color::RGB.from_percentage(70, 70, 70).freeze
  Gray80 = Grey80       = Color::RGB.from_percentage(80, 80, 80).freeze
  Gray90 = Grey90       = Color::RGB.from_percentage(90, 90, 90).freeze
end
