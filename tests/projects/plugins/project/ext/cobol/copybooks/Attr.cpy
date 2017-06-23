       01  ATTRIBUTE-DEFINITIONS.
      *
           05  ATTR-UNPROT                 PIC X   VALUE ' '.
           05  ATTR-UNPROT-MDT             PIC X   VALUE X'C1'.
           05  ATTR-UNPROT-BRT             PIC X   VALUE X'C8'.
           05  ATTR-UNPROT-BRT-MDT         PIC X   VALUE X'C9'.
           05  ATTR-UNPROT-DARK            PIC X   VALUE X'4C'.
           05  ATTR-UNPROT-DARK-MDT        PIC X   VALUE X'4D'.
           05  ATTR-UNPROT-NUM             PIC X   VALUE X'50'.
           05  ATTR-UNPROT-NUM-MDT         PIC X   VALUE X'D1'.
           05  ATTR-UNPROT-NUM-BRT         PIC X   VALUE X'D8'.
           05  ATTR-UNPROT-NUM-BRT-MDT     PIC X   VALUE X'D9'.
           05  ATTR-UNPROT-NUM-DARK        PIC X   VALUE X'5C'.
           05  ATTR-UNPROT-NUM-DARK-MDT    PIC X   VALUE X'5D'.
           05  ATTR-PROT                   PIC X   VALUE X'60'.
           05  ATTR-PROT-MDT               PIC X   VALUE X'61'.
           05  ATTR-PROT-BRT               PIC X   VALUE X'E8'.
           05  ATTR-PROT-BRT-MDT           PIC X   VALUE X'E9'.
           05  ATTR-PROT-DARK              PIC X   VALUE '%'.
           05  ATTR-PROT-DARK-MDT          PIC X   VALUE X'6D'.
           05  ATTR-PROT-SKIP              PIC X   VALUE X'F0'.
           05  ATTR-PROT-SKIP-MDT          PIC X   VALUE X'F1'.
           05  ATTR-PROT-SKIP-BRT          PIC X   VALUE X'F8'.
           05  ATTR-PROT-SKIP-BRT-MDT      PIC X   VALUE X'F9'.
           05  ATTR-PROT-SKIP-DARK         PIC X   VALUE X'7C'.
           05  ATTR-PROT-SKIP-DARK-MDT     PIC X   VALUE X'7D'.
      *
           05  ATTR-NO-HIGHLIGHT           PIC X   VALUE X'00'.
           05  ATTR-BLINK                  PIC X   VALUE '1'.
           05  ATTR-REVERSE                PIC X   VALUE '2'.
           05  ATTR-UNDERSCORE             PIC X   VALUE '4'.
      *
           05  ATTR-DEFAULT-COLOR          PIC X   VALUE X'00'.
           05  ATTR-BLUE                   PIC X   VALUE '1'.
           05  ATTR-RED                    PIC X   VALUE '2'.
           05  ATTR-PINK                   PIC X   VALUE '3'.
           05  ATTR-GREEN                  PIC X   VALUE '4'.
           05  ATTR-TURQUOISE              PIC X   VALUE '5'.
           05  ATTR-YELLOW                 PIC X   VALUE '6'.
           05  ATTR-NEUTRAL                PIC X   VALUE '7'.
