# Parva-compiler

A compiler that translates and compiles the Cocol programming language.


<img width="411" alt="Coco:R Compiler generator" src="https://user-images.githubusercontent.com/47664755/234779842-c9a54e07-9587-4a3a-a4a0-48bde3bc1017.png">

### Scanner

It consists of a scanner, which is specified by a list of token (made up from the ASCII set) declarations used in the production of the grammar for the language.
The scanner is a deterministic finite automation so tokens are described using EBNF rules where symbols are assigned to non-terminals.
It can handle pragmas in the single stream of input into it, pragmas are tokens not part of the syntax but appear int he input stream.

### Parser

The parser is specified by EBNF productions with attributes and semantic actions, that allow for alternatives, repetions and optional parts. Coco/R 
translates the productions into a recursive descent parser.


### Input language

The input language of the compiler is Cocol/R which is used as the input language for COc/R
The languages basic elements can be defined as:
```
ident = letter { letter | digit }.
number = digit { digit }.
string = '"' {anyButQuote|escape} '"' | "'" {anyButApostrophe|escape} "'" .
char = '"' (anyButQuote|escape) '"' | "'" (anyButApostrophe|escape) "'" .
```
The following beign reserved keywords:
```
ANY COMPILER IF NESTED SYNC
CHARACTERS CONTEXT IGNORE out TO
CHR END IGNORECASE PRAGMAS TOKENS
COMMENTS FROM NAMES PRODUCTIONS WEAK
```
The compiler description has the following structure:
```
Cocol =
[ Imports ]
"COMPILER" ident
[ GlobalFieldsAndMethods ]
ScannerSpecification
ParserSpecification
"END" ident '.'
.
```

### Scanner specifications
The scanner reads input text extracting useful information from it to pass to the parser, The scanner has six optional parts (denoted by curly brackets).
```
ScannerSpecification =
[ "IGNORECASE"]
[ "CHARACTERS" { SetDecl } ]
[ "TOKENS" { TokenDecl } ]
[ "NAMES" { NameDecl } ]
[ "PRAGMAS" { PragmaDecl } ]
{ CommentDecl }
{ WhiteSpaceDecl }.
```
The character set is defined as:
```
CHARACTERS
  digit = "0123456789". /* the set of all digits */
  hexDigit = digit + "ABCDEF". /* the set of all hexadecimal digits */
  letter = 'A' .. 'Z'. /* the set of all upper case letters */
  eol = '\r'. /* the end-of-line character */
  noDigit = ANY - digit. /* any character that is not a digit */
  control = CHR(0) .. CHR(31). /* ASCII control characters */
```
The token set is defined as:
```
TOKENS
  ident = letter { letter | digit | '_' }.
  number = digit { digit }
  | "0x" hexDigit hexDigit hexDigit hexDigit.
  float = digit {digit} '.' {digit} [ 'E' ['+'|'-'] digit {digit} ].
```
Pragmas (not forwarded to the parser) are defines as:
```
PRAGMAS
  option = '$' { letter }. (. foreach (char ch in la.val)
  if (ch == 'A') ...
  else if (ch == 'B') ...
  ... .)
```

Comments can be defined as:
```
COMMENTS FROM "/*" TO "*/" NESTED
COMMENTS FROM "//" TO eol
```
or they can be defined in pragmas since they will not be passed to the parser:
```
CHARACTERS
  other = ANY - '/' - '*'.
PRAGMAS
  comment = "/*" {'/' | other | '*' {'*'} other} '*' {'*'} '/'.
```

blanks, tabs, and End of line separators are considered whitespace and can be ignored:
```
WhiteSpaceDecl = "IGNORE" Set.
IGNORE '\t' + '\r' + '\n'
```

###Parser specifications

This is the main part of the compilers description with attributed grammar descriptions that specify the syntax of the language.

i.e:
```
ParserSpecification = "PRODUCTIONS" { Production }.
Production = ident [ FormalAttributes ] [ LocalDecl ] '=' Expression '.'.
Expression = Term {'|' Term}.
Term = [ [ Resolver ] Factor { Factor } ].
Factor = [ "WEAK" ] Symbol [ ActualAttributes ]

  | '(' Expression ')'
  | '[' Expression ']'
  | '{' Expression '}'
  | "ANY"
  | "SYNC"
  | SemAction.

Symbol = ident | string | char.
SemAction = "(." ArbitraryStatements ".)".
LocalDecl = SemAction.
FormalAttributes = '<' ArbitraryText '>' | '<.' ArbitraryText '.>'
ActualAttributes = '<' ArbitraryText '>' | '<.' ArbitraryText '.>'.
Resolver = "IF" '(' { ANY } ')'.
```

##### Productions
The productions show the syntactical structure of a nonterminal symbol, with a right hand side and left hand side separated by an equals sign.
LHS specifies the name and attributes of the nonterminal.
RHS has the EBNF that specifies the structure of the nonterminal.

##### Sementic actions
Sementic actions are written in the target code of the Coco/R compiler, in this case Java. It is exectued by the generated parser.
They are written on the RHS inside brackets with dots (. SemAction .)

```
IdentList =
  ident               (. int n = 1; .)
  { ',' ident         (. n++; .)
  }                   (. System.out.print("n = " + n); .)
.
```


##### Attributes
Productions are considered parsing methods for nonterminals, these nonterminals can have attributes which correspond to the parameters of the nonterminals production.
These attribbtes are used to pass values to the production of a nonterminal and output attributes, to return values from the production of the nonterminal to the caller.

This example shows a nonterminal ```T``` with a output attribute ```x``` and two input attributes ```y``` and ```z```.
```T<out int x, char y, int z> = ... ```


This translates to:
```
static int T(char y, int z) {
int x;
...
return x;
}
```


##### ANY

```ANY``` denotes a token that is not an alternative to that ANY symbol in the context where it appears

```
Attributes < out int len> =
  '<'             (. int beg = token.pos + 1; .)
  { ANY }
  '>'             (. len = token.pos - beg; .) .
```
In the above example the token ```>``` is an implicity alternative to ```ANY```

#To run

You need a version of Coco/R from http://www.ssw.uni-linz.ac.at/Research/Projects/Coco/ for both the Java and C# version of Coco/R.

In java Coco can be invoked with the following command:
```
  java -jar Coco.jar fileName [ Options ]
```
filename is the file containing the Coco/R description is explained above, with the extension .ATG (attributed grammar)

Options are:
```
{ "-namespace" namespaceName /* in Java: "-package" packageName */
| "-frames" framesDirectory
| ( "-trace" | "-options" ) optionString
}.
```

This will output the Scanner.java, Parser.java, listings.txt, trace.txt. 
I have edited this files to create my own more robust compiler to handle more tokens and productions.
