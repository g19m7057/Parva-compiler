package CalcPVM;

import library.*;

import java.io.*;

public class Parser {
	public static final int _EOF = 0;
	public static final int _varIdent = 1;
	public static final int _number = 2;
	public static final int _stringLit = 3;
	// terminals
	public static final int EOF_SYM = 0;
	public static final int varIdent_Sym = 1;
	public static final int number_Sym = 2;
	public static final int stringLit_Sym = 3;
	public static final int semicolon_Sym = 4;
	public static final int equal_Sym = 5;
	public static final int output_Sym = 6;
	public static final int lparen_Sym = 7;
	public static final int rparen_Sym = 8;
	public static final int comma_Sym = 9;
	public static final int plus_Sym = 10;
	public static final int minus_Sym = 11;
	public static final int true_Sym = 12;
	public static final int false_Sym = 13;
	public static final int barbar_Sym = 14;
	public static final int star_Sym = 15;
	public static final int slash_Sym = 16;
	public static final int percent_Sym = 17;
	public static final int andand_Sym = 18;
	public static final int equalequal_Sym = 19;
	public static final int bangequal_Sym = 20;
	public static final int less_Sym = 21;
	public static final int lessequal_Sym = 22;
	public static final int greater_Sym = 23;
	public static final int greaterequal_Sym = 24;
	public static final int NOT_SYM = 25;
	// pragmas
	public static final int CodeOn_Sym = 26;
	public static final int CodeOff_Sym = 27;

	public static final int maxT = 25;
	public static final int _CodeOn = 26;
	public static final int _CodeOff = 27;

	static final boolean T = true;
	static final boolean x = false;
	static final int minErrDist = 2;

	public static Token token; // last recognized token /* pdt */
	public static Token la; // lookahead token
	static int errDist = minErrDist;

	public static boolean debug = false,
			warnings = true,
			listCode = false;

	static VarTable theTable;

	static String unescape(String s) {
		/* Replaces escape sequences in s by their Unicode values */
		StringBuilder buf = new StringBuilder();
		int i = 0;
		while (i < s.length()) {
			if (s.charAt(i) == '\\') {
				switch (s.charAt(i + 1)) {
					case '\\':
						buf.append('\\');
						break;
					case '\'':
						buf.append('\'');
						break;
					case '\"':
						buf.append('\"');
						break;
					case 'r':
						buf.append('\r');
						break;
					case 'n':
						buf.append('\n');
						break;
					case 't':
						buf.append('\t');
						break;
					case 'b':
						buf.append('\b');
						break;
					case 'f':
						buf.append('\f');
						break;
					default:
						buf.append(s.charAt(i + 1));
						break;
				}
				i += 2;
			} else {
				buf.append(s.charAt(i));
				i++;
			}
		}
		return buf.toString();
	} // unescape

	static boolean isArith(int type) {
		return type == Types.intType || type == Types.noType;
	}

	static boolean isBool(int type) {
		return type == Types.boolType || type == Types.noType;
	}

	static boolean compatible(int typeOne, int typeTwo) {
		// Returns true if typeOne is compatible with typeTwo
		return typeOne == typeTwo
				|| typeOne == Types.noType
				|| typeTwo == Types.noType;
	}

	/* ---------------------------------------------------------------------- */

	static void SynErr(int n) {
		if (errDist >= minErrDist)
			Errors.SynErr(la.line, la.col, n);
		errDist = 0;
	}

	public static void SemErr(String msg) {
		if (errDist >= minErrDist)
			Errors.Error(token.line, token.col, msg); /* pdt */
		errDist = 0;
	}

	public static void SemError(String msg) {
		if (errDist >= minErrDist)
			Errors.Error(token.line, token.col, msg); /* pdt */
		errDist = 0;
	}

	public static void Warning(String msg) { /* pdt */
		if (errDist >= minErrDist)
			Errors.Warn(token.line, token.col, msg);
		errDist = 2; // ++ 2009/11/04
	}

	public static boolean Successful() { /* pdt */
		return Errors.count == 0;
	}

	public static String LexString() { /* pdt */
		return token.val;
	}

	public static String LookAheadString() { /* pdt */
		return la.val;
	}

	static void Get() {
		for (;;) {
			token = la; /* pdt */
			la = Scanner.Scan();
			if (la.kind <= maxT) {
				++errDist;
				break;
			}
			if (la.kind == CodeOn_Sym) {
				listCode = true;
			}
			if (la.kind == CodeOff_Sym) {
				listCode = false;
			}

			la = token; /* pdt */
		}
	}

	static void Expect(int n) {
		if (la.kind == n)
			Get();
		else {
			SynErr(n);
		}
	}

	static boolean StartOf(int s) {
		return set[s][la.kind];
	}

	static void ExpectWeak(int n, int follow) {
		if (la.kind == n)
			Get();
		else {
			SynErr(n);
			while (!StartOf(follow))
				Get();
		}
	}

	static boolean WeakSeparator(int n, int syFol, int repFol) {
		boolean[] s = new boolean[maxT + 1];
		if (la.kind == n) {
			Get();
			return true;
		} else if (StartOf(repFol))
			return false;
		else {
			for (int i = 0; i <= maxT; i++) {
				s[i] = set[syFol][i] || set[repFol][i] || set[0][i];
			}
			SynErr(n);
			while (!s[la.kind])
				Get();
			return StartOf(syFol);
		}
	}

	static void CalcPVM() {
		theTable = new VarTable();
		for (char ch = 'a'; ch <= 'z'; ch++)
			theTable.addVar(ch);
		CodeGen.openStackFrame(26);
		while (la.kind == varIdent_Sym || la.kind == semicolon_Sym || la.kind == output_Sym) {
			while (!(StartOf(1))) {
				SynErr(26);
				Get();
			}
			Statement();
		}
		CodeGen.leaveProgram();
		Expect(EOF_SYM);
	}

	static void Statement() {
		if (la.kind == varIdent_Sym) {
			Assignment();
		} else if (la.kind == output_Sym) {
			while (!(la.kind == EOF_SYM || la.kind == output_Sym)) {
				SynErr(27);
				Get();
			}
			OutputStatement();
		} else if (la.kind == semicolon_Sym) {
			ExpectWeak(semicolon_Sym, 2);
		} else
			SynErr(28);
	}

	static void Assignment() {
		char name;
		int offset;
		int expType;
		name = Variable();
		offset = theTable.findOffset(name);
		Expect(equal_Sym);
		Expression();
		CodeGen.storeValue(offset);
		ExpectWeak(semicolon_Sym, 2);
	}

	static void OutputStatement() {
		while (!(la.kind == EOF_SYM || la.kind == output_Sym)) {
			SynErr(29);
			Get();
		}
		Expect(output_Sym);
		ExpectWeak(lparen_Sym, 2);
		WriteList();
		ExpectWeak(rparen_Sym, 2);
		CodeGen.writeLine();
		Expect(semicolon_Sym);
	}

	static char Variable() {
		char name;
		Expect(varIdent_Sym);
		name = token.val.charAt(0);
		return name;
	}

	static void Expression() {
		int op;
		AddExp();
		if (StartOf(3)) {
			op = RelOp();
			AddExp();
			CodeGen.binaryOp(op);
		}
	}

	static void WriteList() {
		while (!(StartOf(4))) {
			SynErr(30);
			Get();
		}
		WriteElement();
		while (WeakSeparator(comma_Sym, 5, 6)) {
			WriteElement();
		}
	}

	static void WriteElement() {
		String str;
		int expType;
		if (la.kind == stringLit_Sym) {
			str = StringConst();
			CodeGen.writeString(str);
		} else if (StartOf(7)) {
			Expression();
			CodeGen.write(Types.intType);
		} else
			SynErr(31);
	}

	static String StringConst() {
		String str;
		Expect(stringLit_Sym);
		str = token.val;
		str = unescape(str.substring(1, str.length() - 1));
		return str;
	}

	static void AddExp() {
		int op;
		if (la.kind == plus_Sym || la.kind == minus_Sym) {
			if (la.kind == plus_Sym) {
				Get();
			} else {
				Get();
			}
		}
		MulExp();
		while (la.kind == plus_Sym || la.kind == minus_Sym || la.kind == barbar_Sym) {
			op = AddOp();
			MulExp();
			CodeGen.binaryOp(op);
		}
	}

	static int RelOp() {
		int op;
		op = CodeGen.nop;
		switch (la.kind) {
			case equalequal_Sym: {
				Get();
				op = CodeGen.ceq;
				break;
			}
			case bangequal_Sym: {
				Get();
				op = CodeGen.cne;
				break;
			}
			case less_Sym: {
				Get();
				op = CodeGen.clt;
				break;
			}
			case lessequal_Sym: {
				Get();
				op = CodeGen.cle;
				break;
			}
			case greater_Sym: {
				Get();
				op = CodeGen.cgt;
				break;
			}
			case greaterequal_Sym: {
				Get();
				op = CodeGen.cge;
				break;
			}
			default:
				SynErr(32);
				break;
		}
		return op;
	}

	static void MulExp() {
		int op;
		Factor();
		while (StartOf(8)) {
			op = MulOp();
			Factor();
			CodeGen.binaryOp(op);
		}
	}

	static int AddOp() {
		int op;
		op = CodeGen.nop;
		if (la.kind == plus_Sym) {
			Get();
			op = CodeGen.add;
		} else if (la.kind == minus_Sym) {
			Get();
			op = CodeGen.sub;
		} else if (la.kind == barbar_Sym) {
			Get();
			op = CodeGen.or;
		} else
			SynErr(33);
		return op;
	}

	static void Factor() {
		ConstRec con;
		char name;
		if (la.kind == varIdent_Sym) {
			name = Variable();
		} else if (la.kind == number_Sym || la.kind == true_Sym || la.kind == false_Sym) {
			con = Constant();
			CodeGen.loadConstant(con.value);
		} else if (la.kind == lparen_Sym) {
			Get();
			Expression();
			Expect(rparen_Sym);
		} else
			SynErr(34);
	}

	static int MulOp() {
		int op;
		op = CodeGen.nop;
		if (la.kind == star_Sym) {
			Get();
			op = CodeGen.mul;
		} else if (la.kind == slash_Sym) {
			Get();
			op = CodeGen.div;
		} else if (la.kind == percent_Sym) {
			Get();
			op = CodeGen.rem;
		} else if (la.kind == andand_Sym) {
			Get();
			op = CodeGen.and;
		} else
			SynErr(35);
		return op;
	}

	static ConstRec Constant() {
		ConstRec con;
		con = new ConstRec();
		if (la.kind == number_Sym) {
			con.value = IntConst();
			con.type = Types.intType;
		} else if (la.kind == true_Sym) {
			Get();
			con.type = Types.boolType;
			con.value = 1;
		} else if (la.kind == false_Sym) {
			Get();
			con.type = Types.boolType;
			con.value = 0;
		} else
			SynErr(36);
		return con;
	}

	static int IntConst() {
		int value;
		Expect(number_Sym);
		try {
			value = Integer.parseInt(token.val);
		} catch (NumberFormatException e) {
			value = 0;
			SemError("number too large");
		}
		return value;
	}

	public static void Parse() {
		la = new Token();
		la.val = "";
		Get();
		CalcPVM();
		Expect(EOF_SYM);

	}

	private static boolean[][] set = {
			{ T, T, T, T, T, x, T, T, x, x, T, T, T, T, x, x, x, x, x, x, x, x, x, x, x, x, x },
			{ T, T, x, x, T, x, T, x, x, x, x, x, x, x, x, x, x, x, x, x, x, x, x, x, x, x, x },
			{ T, T, T, T, T, x, T, T, x, x, T, T, T, T, x, x, x, x, x, x, x, x, x, x, x, x, x },
			{ x, x, x, x, x, x, x, x, x, x, x, x, x, x, x, x, x, x, x, T, T, T, T, T, T, x, x },
			{ T, T, T, T, x, x, x, T, x, x, T, T, T, T, x, x, x, x, x, x, x, x, x, x, x, x, x },
			{ x, T, T, T, x, x, x, T, x, x, T, T, T, T, x, x, x, x, x, x, x, x, x, x, x, x, x },
			{ x, x, x, x, x, x, x, x, T, x, x, x, x, x, x, x, x, x, x, x, x, x, x, x, x, x, x },
			{ x, T, T, x, x, x, x, T, x, x, T, T, T, T, x, x, x, x, x, x, x, x, x, x, x, x, x },
			{ x, x, x, x, x, x, x, x, x, x, x, x, x, x, x, T, T, T, T, x, x, x, x, x, x, x, x }

	};

} // end Parser

/* pdt - considerable extension from here on */

class ErrorRec {
	public int line, col, num;
	public String str;
	public ErrorRec next;

	public ErrorRec(int l, int c, String s) {
		line = l;
		col = c;
		str = s;
		next = null;
	}

} // end ErrorRec

class Errors {

	public static int count = 0; // number of errors detected
	public static int warns = 0; // number of warnings detected
	public static String errMsgFormat = "file {0} : ({1}, {2}) {3}"; // 0=file 1=line, 2=column, 3=text
	static String fileName = "";
	static String listName = "";
	static boolean mergeErrors = false;
	static PrintWriter mergedList;

	static ErrorRec first = null, last;
	static boolean eof = false;

	static String getLine() {
		char ch, CR = '\r', LF = '\n';
		int l = 0;
		StringBuffer s = new StringBuffer();
		ch = (char) Buffer.Read();
		while (ch != Buffer.EOF && ch != CR && ch != LF) {
			s.append(ch);
			l++;
			ch = (char) Buffer.Read();
		}
		eof = (l == 0 && ch == Buffer.EOF);
		if (ch == CR) { // check for MS-DOS
			ch = (char) Buffer.Read();
			if (ch != LF && ch != Buffer.EOF)
				Buffer.pos--;
		}
		return s.toString();
	}

	static private String Int(int n, int len) {
		String s = String.valueOf(n);
		int i = s.length();
		if (len < i)
			len = i;
		int j = 0, d = len - s.length();
		char[] a = new char[len];
		for (i = 0; i < d; i++)
			a[i] = ' ';
		for (j = 0; i < len; i++) {
			a[i] = s.charAt(j);
			j++;
		}
		return new String(a, 0, len);
	}

	static void display(String s, ErrorRec e) {
		mergedList.print("**** ");
		for (int c = 1; c < e.col; c++)
			if (s.charAt(c - 1) == '\t')
				mergedList.print("\t");
			else
				mergedList.print(" ");
		mergedList.println("^ " + e.str);
	}

	public static void Init(String fn, String dir, boolean merge) {
		fileName = fn;
		listName = dir + "listing.txt";
		mergeErrors = merge;
		if (mergeErrors)
			try {
				mergedList = new PrintWriter(new BufferedWriter(new FileWriter(listName, false)));
			} catch (IOException e) {
				Errors.Exception("-- could not open " + listName);
			}
	}

	public static void Summarize() {
		if (mergeErrors) {
			mergedList.println();
			ErrorRec cur = first;
			Buffer.setPos(0);
			int lnr = 1;
			String s = getLine();
			while (!eof) {
				mergedList.println(Int(lnr, 4) + " " + s);
				while (cur != null && cur.line == lnr) {
					display(s, cur);
					cur = cur.next;
				}
				lnr++;
				s = getLine();
			}
			if (cur != null) {
				mergedList.println(Int(lnr, 4));
				while (cur != null) {
					display(s, cur);
					cur = cur.next;
				}
			}
			mergedList.println();
			mergedList.println(count + " errors detected");
			if (warns > 0)
				mergedList.println(warns + " warnings detected");
			mergedList.close();
		}
		switch (count) {
			case 0:
				System.out.println("Parsed correctly");
				break;
			case 1:
				System.out.println("1 error detected");
				break;
			default:
				System.out.println(count + " errors detected");
				break;
		}
		if (warns > 0)
			System.out.println(warns + " warnings detected");
		if ((count > 0 || warns > 0) && mergeErrors)
			System.out.println("see " + listName);
	}

	public static void storeError(int line, int col, String s) {
		if (mergeErrors) {
			ErrorRec latest = new ErrorRec(line, col, s);
			if (first == null)
				first = latest;
			else
				last.next = latest;
			last = latest;
		} else
			printMsg(fileName, line, col, s);
	}

	public static void SynErr(int line, int col, int n) {
		String s;
		switch (n) {
			case 0:
				s = "EOF expected";
				break;
			case 1:
				s = "varIdent expected";
				break;
			case 2:
				s = "number expected";
				break;
			case 3:
				s = "stringLit expected";
				break;
			case 4:
				s = "\";\" expected";
				break;
			case 5:
				s = "\"=\" expected";
				break;
			case 6:
				s = "\"output\" expected";
				break;
			case 7:
				s = "\"(\" expected";
				break;
			case 8:
				s = "\")\" expected";
				break;
			case 9:
				s = "\",\" expected";
				break;
			case 10:
				s = "\"+\" expected";
				break;
			case 11:
				s = "\"-\" expected";
				break;
			case 12:
				s = "\"true\" expected";
				break;
			case 13:
				s = "\"false\" expected";
				break;
			case 14:
				s = "\"||\" expected";
				break;
			case 15:
				s = "\"*\" expected";
				break;
			case 16:
				s = "\"/\" expected";
				break;
			case 17:
				s = "\"%\" expected";
				break;
			case 18:
				s = "\"&&\" expected";
				break;
			case 19:
				s = "\"==\" expected";
				break;
			case 20:
				s = "\"!=\" expected";
				break;
			case 21:
				s = "\"<\" expected";
				break;
			case 22:
				s = "\"<=\" expected";
				break;
			case 23:
				s = "\">\" expected";
				break;
			case 24:
				s = "\">=\" expected";
				break;
			case 25:
				s = "??? expected";
				break;
			case 26:
				s = "this symbol not expected in CalcPVM";
				break;
			case 27:
				s = "this symbol not expected in Statement";
				break;
			case 28:
				s = "invalid Statement";
				break;
			case 29:
				s = "this symbol not expected in OutputStatement";
				break;
			case 30:
				s = "this symbol not expected in WriteList";
				break;
			case 31:
				s = "invalid WriteElement";
				break;
			case 32:
				s = "invalid RelOp";
				break;
			case 33:
				s = "invalid AddOp";
				break;
			case 34:
				s = "invalid Factor";
				break;
			case 35:
				s = "invalid MulOp";
				break;
			case 36:
				s = "invalid Constant";
				break;
			default:
				s = "error " + n;
				break;
		}
		storeError(line, col, s);
		count++;
	}

	public static void SemErr(int line, int col, int n) {
		storeError(line, col, ("error " + n));
		count++;
	}

	public static void Error(int line, int col, String s) {
		storeError(line, col, s);
		count++;
	}

	public static void Error(String s) {
		if (mergeErrors)
			mergedList.println(s);
		else
			System.out.println(s);
		count++;
	}

	public static void Warn(int line, int col, String s) {
		storeError(line, col, s);
		warns++;
	}

	public static void Warn(String s) {
		if (mergeErrors)
			mergedList.println(s);
		else
			System.out.println(s);
		warns++;
	}

	public static void Exception(String s) {
		System.out.println(s);
		System.exit(1);
	}

	private static void printMsg(String fileName, int line, int column, String msg) {
		StringBuffer b = new StringBuffer(errMsgFormat);
		int pos = b.indexOf("{0}");
		if (pos >= 0) {
			b.replace(pos, pos + 3, fileName);
		}
		pos = b.indexOf("{1}");
		if (pos >= 0) {
			b.delete(pos, pos + 3);
			b.insert(pos, line);
		}
		pos = b.indexOf("{2}");
		if (pos >= 0) {
			b.delete(pos, pos + 3);
			b.insert(pos, column);
		}
		pos = b.indexOf("{3}");
		if (pos >= 0)
			b.replace(pos, pos + 3, msg);
		System.out.println(b.toString());
	}

} // end Errors
