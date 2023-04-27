package CalcPVM;

import java.util.*;

class Label {
  private int memAdr; // address if this.defined, else last forward reference
  private boolean defined; // true once this.memAdr is known

  public Label(boolean known) {
    // Constructor for label, possibly at already known location
    if (known)
      this.memAdr = CodeGen.getCodeLength();
    else
      this.memAdr = CodeGen.undefined; // mark end of forward reference chain
    this.defined = known;
  }

  public int address() {
    // Returns memAdr if known, otherwise effectively adds to a forward reference
    // chain that will be resolved if and when here() is called and returns the
    // address of the most recent forward reference
    int adr = memAdr;
    if (!defined)
      memAdr = CodeGen.getCodeLength();
    return adr;
  }

  public void here() {
    // Defines memAdr of this label to be at current location counter after fixing
    // any outstanding forward references
    if (defined)
      Parser.SemError("Compiler error - bad label");
    else
      CodeGen.backPatch(memAdr);
    memAdr = CodeGen.getCodeLength();
    defined = true;
  }

  public boolean isDefined() {
    // Returns true if the location of this label has been established
    return defined;
  }

  public String toString() {
    return Integer.toString(memAdr);
  }

} // end Label

class CodeGen {
  static boolean generatingCode = true;
  static int codeTop = 0, stkTop = PVM.memSize;

  public static final int undefined = -1,
      headerSize = PVM.headerSize,

      nop = 1,
      add = 2,
      sub = 3,
      mul = 4,
      div = 5,
      rem = 6,
      and = 7,
      or = 8,
      ceq = 9,
      cne = 10,
      clt = 11,
      cge = 12,
      cgt = 13,
      cle = 14;

  private static void emit(int word) {
    // Code generator for single word
    if (!generatingCode)
      return;
    if (codeTop >= stkTop) {
      Parser.SemError("program too long");
      generatingCode = false;
    } else {
      PVM.mem[codeTop] = word;
      codeTop++;
    }
  }

  public static void negateInteger() {
    // Generates code to negate integer value on top of evaluation stack
    emit(PVM.neg);
  }

  public static void negateBoolean() {
    // Generates code to negate boolean value on top of evaluation stack
    emit(PVM.not);
  }

  public static void binaryOp(int op) {
    // Generates code to pop two values A,B from evaluation stack
    // and push value A op B
    switch (op) {
      case CodeGen.mul:
        emit(PVM.mul);
        break;
      case CodeGen.div:
        emit(PVM.div);
        break;
      case CodeGen.rem:
        emit(PVM.rem);
        break;
      case CodeGen.and:
        emit(PVM.and);
        break;
      case CodeGen.add:
        emit(PVM.add);
        break;
      case CodeGen.sub:
        emit(PVM.sub);
        break;
      case CodeGen.or:
        emit(PVM.or);
        break;
    }
  }

  public static void comparison(int op) {
    // Generates code to pop two values A,B from evaluation stack
    // and push Boolean value A op B
    switch (op) {
      case CodeGen.ceq:
        emit(PVM.ceq);
        break;
      case CodeGen.cne:
        emit(PVM.cne);
        break;
      case CodeGen.clt:
        emit(PVM.clt);
        break;
      case CodeGen.cle:
        emit(PVM.cle);
        break;
      case CodeGen.cgt:
        emit(PVM.cgt);
        break;
      case CodeGen.cge:
        emit(PVM.cge);
        break;
      case CodeGen.nop:
        break;
    }
  }

  public static void read(int type) {
    // Generates code to read a value of specified type
    // and store it at the address found on top of stack
    switch (type) {
      case Types.intType:
        emit(PVM.inpi);
        break;
      case Types.boolType:
        emit(PVM.inpb);
        break;
    }
  }

  public static void write(int type) {
    // Generates code to output value of specified type, popped from top of stack
    switch (type) {
      case Types.intType:
        emit(PVM.prni);
        break;
      case Types.boolType:
        emit(PVM.prnb);
        break;
    }
  }

  public static void writeLine() {
    // Generates code to output line mark
    emit(PVM.prnl);
  }

  public static void writeString(String str) {
    // Generates code to output string stored at known location
    int l = str.length(), first = stkTop - 1;
    if (stkTop <= codeTop + l + 1) {
      Parser.SemError("program too long");
      generatingCode = false;
      return;
    }
    for (int i = 0; i < l; i++) {
      stkTop--;
      PVM.mem[stkTop] = str.charAt(i);
    }
    stkTop--;
    PVM.mem[stkTop] = 0;
    emit(PVM.prns);
    emit(first);
  }

  public static void loadConstant(int number) {
    // Generates code to push number onto evaluation stack
    emit(PVM.ldc);
    emit(number);
  }

  public static void loadAddress(int offset) {
    // Generates code to push address of variable with known local offset onto
    // evaluation stack
    emit(PVM.lda);
    emit(offset);
  }

  public static void loadValue(int offset) {
    // Generates code to push value of variable with known local offset onto
    // evaluation stack
    emit(PVM.ldl);
    emit(offset);
  }

  public static void index() {
    // Generates code to index an array on the heap
    emit(PVM.ldxa);
  }

  public static void allocate() {
    // Generates code to allocate an array on the heap
    emit(PVM.anew);
  }

  public static void dereference() {
    // Generates code to replace top of evaluation stack by the value found at the
    // address currently stored on top of the stack
    emit(PVM.ldv);
  }

  public static void assign(int type) {
    // Generates code to store value currently on top-of-stack on the address
    // given by next-to-top, popping these two elements
    emit(PVM.sto);
  }

  public static void storeValue(int offset) {
    // Generates code to pop top of stack and store at known local offset
    emit(PVM.stl);
    emit(offset);
  }

  public static void openStackFrame(int size) {
    // Generates (possibly incomplete) code to reserve space for variables
    emit(PVM.dsp);
    emit(size);
  }

  public static void fixDSP(int location, int size) {
    // Fixes up DSP instruction at location to reserve size space for variables
    PVM.mem[location + 1] = size;
  }

  public static void leaveProgram() {
    // Generates code needed to leave a program (halt)
    emit(PVM.halt);
  }

  public static void pop(int n) {
    // Generates code to pop and discard top n elements from the evaluation stack
    emit(PVM.dsp);
    emit(-n);
  }

  public static void branch(Label destination) {
    // Generates unconditional branch to destination
    emit(PVM.brn);
    emit(destination.address());
  }

  public static void branchFalse(Label destination) {
    // Generates branch to destination, conditional on the Boolean
    // value currently on top of the evaluation stack, popping this value
    emit(PVM.bze);
    emit(destination.address());
  }

  public static void backPatch(int adr) {
    // Stores the current location counter as the address field of the branch or
    // call
    // instruction currently holding a forward reference to adr and repeatedly
    // works through a linked list of such instructions
    while (adr != undefined) {
      int nextAdr = PVM.mem[adr];
      PVM.mem[adr] = codeTop;
      adr = nextAdr;
    }
  }

  public static void dump() {
    // Generates code to dump the current state of the evaluation stack (debugging
    // aid)
    emit(PVM.stk);
  }

  public static int getCodeLength() {
    // Returns codeTop = length of the generated code
    return codeTop;
  }

  public static int getInitSP() {
    // Returns stkTop = position for initial stack pointer
    return stkTop;
  }

  public static void oneWord(String mnemonic) {
    // Inline assembly of a single word instruction (with no operand)
    emit(PVM.opCode(mnemonic));
  }

  public static void twoWord(String mnemonic, int adr) {
    // Inline assembly of a two word instruction (with integer operand)
    emit(PVM.opCode(mnemonic));
    emit(adr);
  }

  public static void branch(String mnemonic, Label adr) {
    // Inline assembly of a two word branch style instruction (with Label operand)
    emit(PVM.opCode(mnemonic));
    emit(adr.address());
  }

} // end CodeGen
