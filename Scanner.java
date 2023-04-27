package CalcPVM;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.BitSet;

class Token {
	public int kind; // token kind
	public int pos; // token position in the source text (starting at 0)
	public int col; // token column (starting at 0)
	public int line; // token line (starting at 1)
	public String val; // token value
	public Token next; // AW 2003-03-07 Tokens are kept in linked list
}

class Buffer {
	public static final char EOF = (char) 256;
	static byte[] buf;
	static int bufLen;
	static int pos;

	public static void Fill(FileInputStream s) {
		try {
			bufLen = s.available();
			buf = new byte[bufLen];
			s.read(buf, 0, bufLen);
			pos = 0;
		} catch (IOException e) {
			System.out.println("--- error on filling the buffer ");
			System.exit(1);
		}
	}

	public static int Read() {
		if (pos < bufLen)
			return buf[pos++] & 0xff; // mask out sign bits
		else
			return EOF; /* pdt */
	}

	public static int Peek() {
		if (pos < bufLen)
			return buf[pos] & 0xff; // mask out sign bits
		else
			return EOF; /* pdt */
	}

	/* AW 2003-03-10 moved this from ParserGen.cs */
	public static String GetString(int beg, int end) {
		StringBuffer s = new StringBuffer(64);
		int oldPos = Buffer.getPos();
		Buffer.setPos(beg);
		while (beg < end) {
			s.append((char) Buffer.Read());
			beg++;
		}
		Buffer.setPos(oldPos);
		return s.toString();
	}

	public static int getPos() {
		return pos;
	}

	public static void setPos(int value) {
		if (value < 0)
			pos = 0;
		else if (value >= bufLen)
			pos = bufLen;
		else
			pos = value;
	}

} // end Buffer

public class Scanner {
	static final char EOL = '\n';
	static final int eofSym = 0;
	static final int charSetSize = 256;
	static final int maxT = 25;
	static final int noSym = 25;
	// terminals
	static final int EOF_SYM = 0;
	static final int varIdent_Sym = 1;
	static final int number_Sym = 2;
	static final int stringLit_Sym = 3;
	static final int semicolon_Sym = 4;
	static final int equal_Sym = 5;
	static final int output_Sym = 6;
	static final int lparen_Sym = 7;
	static final int rparen_Sym = 8;
	static final int comma_Sym = 9;
	static final int plus_Sym = 10;
	static final int minus_Sym = 11;
	static final int true_Sym = 12;
	static final int false_Sym = 13;
	static final int barbar_Sym = 14;
	static final int star_Sym = 15;
	static final int slash_Sym = 16;
	static final int percent_Sym = 17;
	static final int andand_Sym = 18;
	static final int equalequal_Sym = 19;
	static final int bangequal_Sym = 20;
	static final int less_Sym = 21;
	static final int lessequal_Sym = 22;
	static final int greater_Sym = 23;
	static final int greaterequal_Sym = 24;
	static final int NOT_SYM = 25;
	// pragmas
	static final int CodeOn_Sym = 26;
	static final int CodeOff_Sym = 27;

	static short[] start = {
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			0, 36, 3, 0, 8, 32, 33, 0, 16, 17, 30, 19, 18, 20, 0, 31,
			2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 0, 10, 44, 40, 45, 0,
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			0, 1, 1, 1, 1, 1, 43, 1, 1, 1, 1, 1, 1, 1, 1, 41,
			1, 1, 1, 1, 42, 1, 1, 1, 1, 1, 1, 0, 28, 0, 0, 0,
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			-1 };

	static Token t; // current token
	static char ch; // current input character
	static int pos; // column number of current character
	static int line; // line number of current character
	static int lineStart; // start position of current line
	static int oldEols; // EOLs that appeared in a comment;
	static BitSet ignore; // set of characters to be ignored by the scanner

	static Token tokens; // the complete input token stream
	static Token pt; // current peek token

	public static void Init(String fileName) {
		FileInputStream s = null;
		try {
			s = new FileInputStream(fileName);
			Init(s);
		} catch (IOException e) {
			System.out.println("--- Cannot open file " + fileName);
			System.exit(1);
		} finally {
			if (s != null) {
				try {
					s.close();
				} catch (IOException e) {
					System.out.println("--- Cannot close file " + fileName);
					System.exit(1);
				}
			}
		}
	}

	public static void Init(FileInputStream s) {
		Buffer.Fill(s);
		pos = -1;
		line = 1;
		lineStart = 0;
		oldEols = 0;
		NextCh();
		ignore = new BitSet(charSetSize + 1);
		ignore.set(' '); // blanks are always white space
		ignore.set(9);
		ignore.set(10);
		ignore.set(11);
		ignore.set(12);
		ignore.set(13);
		// --- AW: fill token list
		tokens = new Token(); // first token is a dummy
		Token node = tokens;
		do {
			node.next = NextToken();
			node = node.next;
		} while (node.kind != eofSym);
		node.next = node;
		node.val = "EOF";
		t = pt = tokens;
	}

	static void NextCh() {
		if (oldEols > 0) {
			ch = EOL;
			oldEols--;
		} else {
			ch = (char) Buffer.Read();
			pos++;
			// replace isolated '\r' by '\n' in order to make
			// eol handling uniform across Windows, Unix and Mac
			if (ch == '\r' && Buffer.Peek() != '\n')
				ch = EOL;
			if (ch == EOL) {
				line++;
				lineStart = pos + 1;
			}
		}

	}

	static boolean Comment0() {
		int level = 1, line0 = line, lineStart0 = lineStart;
		NextCh();
		if (ch == '*') {
			NextCh();
			for (;;) {
				if (ch == '*') {
					NextCh();
					if (ch == '/') {
						level--;
						if (level == 0) {
							oldEols = line - line0;
							NextCh();
							return true;
						}
						NextCh();
					}
				} else if (ch == Buffer.EOF)
					return false;
				else
					NextCh();
			}
		} else {
			if (ch == EOL) {
				line--;
				lineStart = lineStart0;
			}
			pos = pos - 2;
			Buffer.setPos(pos + 1);
			NextCh();
		}
		return false;
	}

	static boolean Comment1() {
		int level = 1, line0 = line, lineStart0 = lineStart;
		NextCh();
		if (ch == '/') {
			NextCh();
			for (;;) {
				if (ch == 10) {
					level--;
					if (level == 0) {
						oldEols = line - line0;
						NextCh();
						return true;
					}
					NextCh();
				} else if (ch == Buffer.EOF)
					return false;
				else
					NextCh();
			}
		} else {
			if (ch == EOL) {
				line--;
				lineStart = lineStart0;
			}
			pos = pos - 2;
			Buffer.setPos(pos + 1);
			NextCh();
		}
		return false;
	}

	static void CheckLiteral() {
		String lit = t.val;

	}

	/* AW Scan() renamed to NextToken() */
	static Token NextToken() {
		while (ignore.get(ch))
			NextCh();
		if (ch == '/' && Comment0() || ch == '/' && Comment1())
			return NextToken();
		t = new Token();
		t.pos = pos;
		t.col = pos - lineStart + 1;
		t.line = line;
		int state = start[ch];
		StringBuffer buf = new StringBuffer(16);
		buf.append(ch);
		NextCh();
		boolean done = false;
		while (!done) {
			switch (state) {
				case -1: {
					t.kind = eofSym;
					done = true;
					break;
				} // NextCh already done /* pdt */
				case 0: {
					t.kind = noSym;
					done = true;
					break;
				} // NextCh already done
				case 1: {
					t.kind = varIdent_Sym;
					done = true;
					break;
				}
				case 2:
					if ((ch >= '0' && ch <= '9')) {
						buf.append(ch);
						NextCh();
						state = 2;
						break;
					} else {
						t.kind = number_Sym;
						done = true;
						break;
					}
				case 3:
					if ((ch >= ' ' && ch <= '!'
							|| ch >= '#' && ch <= '['
							|| ch >= ']' && ch <= 255)) {
						buf.append(ch);
						NextCh();
						state = 3;
						break;
					} else if ((ch == 92)) {
						buf.append(ch);
						NextCh();
						state = 4;
						break;
					} else if (ch == '"') {
						buf.append(ch);
						NextCh();
						state = 5;
						break;
					} else {
						t.kind = noSym;
						done = true;
						break;
					}
				case 4:
					if ((ch >= ' ' && ch <= 255)) {
						buf.append(ch);
						NextCh();
						state = 3;
						break;
					} else {
						t.kind = noSym;
						done = true;
						break;
					}
				case 5: {
					t.kind = stringLit_Sym;
					done = true;
					break;
				}
				case 6: {
					t.kind = CodeOn_Sym;
					done = true;
					break;
				}
				case 7: {
					t.kind = CodeOff_Sym;
					done = true;
					break;
				}
				case 8:
					if (ch == 'C') {
						buf.append(ch);
						NextCh();
						state = 9;
						break;
					} else {
						t.kind = noSym;
						done = true;
						break;
					}
				case 9:
					if (ch == '+') {
						buf.append(ch);
						NextCh();
						state = 6;
						break;
					} else if (ch == '-') {
						buf.append(ch);
						NextCh();
						state = 7;
						break;
					} else {
						t.kind = noSym;
						done = true;
						break;
					}
				case 10: {
					t.kind = semicolon_Sym;
					done = true;
					break;
				}
				case 11:
					if (ch == 't') {
						buf.append(ch);
						NextCh();
						state = 12;
						break;
					} else {
						t.kind = noSym;
						done = true;
						break;
					}
				case 12:
					if (ch == 'p') {
						buf.append(ch);
						NextCh();
						state = 13;
						break;
					} else {
						t.kind = noSym;
						done = true;
						break;
					}
				case 13:
					if (ch == 'u') {
						buf.append(ch);
						NextCh();
						state = 14;
						break;
					} else {
						t.kind = noSym;
						done = true;
						break;
					}
				case 14:
					if (ch == 't') {
						buf.append(ch);
						NextCh();
						state = 15;
						break;
					} else {
						t.kind = noSym;
						done = true;
						break;
					}
				case 15: {
					t.kind = output_Sym;
					done = true;
					break;
				}
				case 16: {
					t.kind = lparen_Sym;
					done = true;
					break;
				}
				case 17: {
					t.kind = rparen_Sym;
					done = true;
					break;
				}
				case 18: {
					t.kind = comma_Sym;
					done = true;
					break;
				}
				case 19: {
					t.kind = plus_Sym;
					done = true;
					break;
				}
				case 20: {
					t.kind = minus_Sym;
					done = true;
					break;
				}
				case 21:
					if (ch == 'u') {
						buf.append(ch);
						NextCh();
						state = 22;
						break;
					} else {
						t.kind = noSym;
						done = true;
						break;
					}
				case 22:
					if (ch == 'e') {
						buf.append(ch);
						NextCh();
						state = 23;
						break;
					} else {
						t.kind = noSym;
						done = true;
						break;
					}
				case 23: {
					t.kind = true_Sym;
					done = true;
					break;
				}
				case 24:
					if (ch == 'l') {
						buf.append(ch);
						NextCh();
						state = 25;
						break;
					} else {
						t.kind = noSym;
						done = true;
						break;
					}
				case 25:
					if (ch == 's') {
						buf.append(ch);
						NextCh();
						state = 26;
						break;
					} else {
						t.kind = noSym;
						done = true;
						break;
					}
				case 26:
					if (ch == 'e') {
						buf.append(ch);
						NextCh();
						state = 27;
						break;
					} else {
						t.kind = noSym;
						done = true;
						break;
					}
				case 27: {
					t.kind = false_Sym;
					done = true;
					break;
				}
				case 28:
					if (ch == '|') {
						buf.append(ch);
						NextCh();
						state = 29;
						break;
					} else {
						t.kind = noSym;
						done = true;
						break;
					}
				case 29: {
					t.kind = barbar_Sym;
					done = true;
					break;
				}
				case 30: {
					t.kind = star_Sym;
					done = true;
					break;
				}
				case 31: {
					t.kind = slash_Sym;
					done = true;
					break;
				}
				case 32: {
					t.kind = percent_Sym;
					done = true;
					break;
				}
				case 33:
					if (ch == '&') {
						buf.append(ch);
						NextCh();
						state = 34;
						break;
					} else {
						t.kind = noSym;
						done = true;
						break;
					}
				case 34: {
					t.kind = andand_Sym;
					done = true;
					break;
				}
				case 35: {
					t.kind = equalequal_Sym;
					done = true;
					break;
				}
				case 36:
					if (ch == '=') {
						buf.append(ch);
						NextCh();
						state = 37;
						break;
					} else {
						t.kind = noSym;
						done = true;
						break;
					}
				case 37: {
					t.kind = bangequal_Sym;
					done = true;
					break;
				}
				case 38: {
					t.kind = lessequal_Sym;
					done = true;
					break;
				}
				case 39: {
					t.kind = greaterequal_Sym;
					done = true;
					break;
				}
				case 40:
					if (ch == '=') {
						buf.append(ch);
						NextCh();
						state = 35;
						break;
					} else {
						t.kind = equal_Sym;
						done = true;
						break;
					}
				case 41:
					if (ch == 'u') {
						buf.append(ch);
						NextCh();
						state = 11;
						break;
					} else {
						t.kind = varIdent_Sym;
						done = true;
						break;
					}
				case 42:
					if (ch == 'r') {
						buf.append(ch);
						NextCh();
						state = 21;
						break;
					} else {
						t.kind = varIdent_Sym;
						done = true;
						break;
					}
				case 43:
					if (ch == 'a') {
						buf.append(ch);
						NextCh();
						state = 24;
						break;
					} else {
						t.kind = varIdent_Sym;
						done = true;
						break;
					}
				case 44:
					if (ch == '=') {
						buf.append(ch);
						NextCh();
						state = 38;
						break;
					} else {
						t.kind = less_Sym;
						done = true;
						break;
					}
				case 45:
					if (ch == '=') {
						buf.append(ch);
						NextCh();
						state = 39;
						break;
					} else {
						t.kind = greater_Sym;
						done = true;
						break;
					}

			}
		}
		t.val = buf.toString();
		return t;
	}

	/*
	 * AW 2003-03-07 get the next token, move on and synch peek token with current
	 */
	public static Token Scan() {
		t = pt = t.next;
		return t;
	}

	/* AW 2003-03-07 get the next token, ignore pragmas */
	public static Token Peek() {
		do { // skip pragmas while peeking
			pt = pt.next;
		} while (pt.kind > maxT);
		return pt;
	}

	/* AW 2003-03-11 to make sure peek start at current scan position */
	public static void ResetPeek() {
		pt = t;
	}

} // end Scanner
