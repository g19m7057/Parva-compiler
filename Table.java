package CalcPVM;

import java.util.*;
import library.*;

class Types {
  // Identifier (and expression) types.
  public static final int noType = 0,
      nullType = 2,
      intType = 4,
      boolType = 6,
      voidType = 8;
} // end Types

class Kinds {
  // Identifier kinds
  public static final int Con = 0,
      Var = 1;
} // end Kinds

class ConstRec {
  // Objects of this type are associated with literal constants
  public int value; // value of a constant literal
  public int type; // constant type (determined from the literal)
} // end ConstRec

class VarEntry {
  // Symbol table entries for parameters and variables
  public char name;
  public int offset;

  public VarEntry(char name, int offset) {
    this.name = name;
    this.offset = offset;
  } // constructor

  public String toString() {
    return name + "  " + offset;
  } // toString

} // VarEntry

class VarTable {
  // Symbol tables for single letter variables and parameters

  static ArrayList<VarEntry> varList = new ArrayList<VarEntry>();

  public static int findOffset(char name) {
    // Searches table for an entry matching name.
    // If found then return the corresponding offset
    for (int look = 0; look < varList.size(); look++)
      if (varList.get(look).name == name) {
        return varList.get(look).offset;
      }
    Parser.SemError("undeclared");
    return 0;
  } // findOffset

  public static void addVar(char name) {
    // Adds an entry, computing what its offset would be
    varList.add(new VarEntry(name, varList.size()));
  } // addVar

  public static void printTable(OutFile lst) {
    // Prints symbol table for diagnostic purposes
    lst.writeLine();
    lst.writeLine("Variable table");
    lst.write("Name", 6);
    lst.write("Offset", 10);
    for (int i = 0; i < varList.size(); i++) {
      lst.write(varList.get(i).name, 4);
      lst.write(varList.get(i).offset, 8);
    }
  } // printTable

} // end VarTable
