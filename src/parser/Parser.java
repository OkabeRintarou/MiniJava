package parser;

import ast.Ast;
import lexer.Lexer;
import lexer.Token;
import lexer.Token.Kind;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class Parser {
  Lexer lexer;
  Token current;
  Map<String, Ast.Type.T> id2type;

  public Parser(String fname, java.io.InputStream fstream) {
    lexer = new Lexer(fname, fstream);
    current = lexer.nextToken();
  }

  // /////////////////////////////////////////////
  // utility methods to connect the lexer
  // and the parser.

  private void advance() {
    current = lexer.nextToken();
  }

  private void eatToken(Kind kind) {
    if (kind == current.kind)
      advance();
    else {
      System.out.println("Expects: " + kind.toString());
      System.out.println("But got: " + current.kind.toString());
      System.exit(1);
    }
  }

  private void error() {
    System.out.println("Syntax error: compilation aborting...\n");
    System.exit(1);
  }

  // ////////////////////////////////////////////////////////////
  // below are method for parsing.

  // A bunch of parsing methods to parse expressions. The messy
  // parts are to deal with precedence and associativity.

  // ExpList -> Exp ExpRest*
  // ->
  // ExpRest -> , Exp
  private LinkedList<Ast.Exp.T> parseExpList() {

    LinkedList<Ast.Exp.T> expList = new LinkedList<>();

    if (current.kind == Kind.TOKEN_RPAREN)
      return expList;
    expList.add(parseExp());
    while (current.kind == Kind.TOKEN_COMMER) {
      advance();
      expList.add(parseExp());
    }
    return expList;
  }

  // AtomExp -> (exp)
  // -> INTEGER_LITERAL
  // -> true
  // -> false
  // -> this
  // -> id
  // -> new int [exp]
  // -> new id ()
  private Ast.Exp.T parseAtomExp() {

    Ast.Exp.T exp;
    String id;

    switch (current.kind) {
      case TOKEN_LPAREN:
        advance();
        exp = parseExp();
        eatToken(Kind.TOKEN_RPAREN);
        return exp;
      case TOKEN_NUM:
        exp = new Ast.Exp.Num(Integer.parseInt(current.lexeme));
        advance();
        return exp;
      case TOKEN_TRUE:
        exp = new Ast.Exp.True();
        advance();
        return exp;
      case TOKEN_FALSE:
        exp = new Ast.Exp.False();
        advance();
        return exp;
      case TOKEN_THIS:
        exp = new Ast.Exp.This();
        advance();
        return exp;
      case TOKEN_ID:
        exp = new Ast.Exp.Id(current.lexeme);
        Ast.Type.T type = id2type.get(current.lexeme);
        if (type != null) {
          ((Ast.Exp.Id) exp).type = type;
        }

        advance();
        return exp;
      case TOKEN_NEW: {
        advance();
        switch (current.kind) {
          case TOKEN_INT:
            advance();
            eatToken(Kind.TOKEN_LBRACK);
            exp = parseExp();
            eatToken(Kind.TOKEN_RBRACK);
            return new Ast.Exp.NewIntArray(exp);
          case TOKEN_ID:
            id = current.lexeme;
            advance();
            eatToken(Kind.TOKEN_LPAREN);
            eatToken(Kind.TOKEN_RPAREN);
            return new Ast.Exp.NewObject(id);
          default:
            error();
            return null;
        }
      }
      default:
        error();
        return null;
    }
  }

  // NotExp -> AtomExp
  // -> AtomExp .id (expList)
  // -> AtomExp [exp]
  // -> AtomExp .length
  private Ast.Exp.T parseNotExp() {

    Ast.Exp.T exp = parseAtomExp();

    while (current.kind == Kind.TOKEN_DOT || current.kind == Kind.TOKEN_LBRACK) {

      if (current.kind == Kind.TOKEN_DOT) {
        advance();
        if (current.kind == Kind.TOKEN_LENGTH) {
          advance();
          exp = new Ast.Exp.Length(exp);
          break;
        }
        String id = current.lexeme;
        eatToken(Kind.TOKEN_ID);
        eatToken(Kind.TOKEN_LPAREN);
        LinkedList<Ast.Exp.T> args = parseExpList();
        exp = new Ast.Exp.Call(exp, id, args);
        eatToken(Kind.TOKEN_RPAREN);
      } else {
        // NotExp -> AtomExp [exp]
        advance();
        Ast.Exp.T index = parseExp();
        exp = new Ast.Exp.ArraySelect(exp, index);
        eatToken(Kind.TOKEN_RBRACK);
      }
    }


    return exp;
  }

  // TimesExp -> ! TimesExp
  // -> NotExp
  private Ast.Exp.T parseTimesExp() {
    int c = 0;
    while (current.kind == Kind.TOKEN_NOT) {
      c++;
      advance();
    }

    Ast.Exp.T exp = parseNotExp();
    if ((c & 1) == 0x01) {
      exp = new Ast.Exp.Not(exp);
    }
    return exp;
  }

  // AddSubExp -> TimesExp * TimesExp
  // -> TimesExp
  private Ast.Exp.T parseAddSubExp() {

    Ast.Exp.T right;
    Ast.Exp.T exp = parseTimesExp();

    while (current.kind == Kind.TOKEN_TIMES) {
      advance();
      right = parseTimesExp();
      exp = new Ast.Exp.Times(exp, right);
    }
    return exp;
  }

  // LtExp -> AddSubExp + AddSubExp
  // -> AddSubExp - AddSubExp
  // -> AddSubExp
  private Ast.Exp.T parseLtExp() {
    Ast.Exp.T exp, right;

    exp = parseAddSubExp();

    while (current.kind == Kind.TOKEN_ADD || current.kind == Kind.TOKEN_SUB) {
      Kind kind = current.kind;
      advance();
      right = parseAddSubExp();

      if (kind == Kind.TOKEN_ADD) {
        exp = new Ast.Exp.Add(exp, right);
      } else {
        exp = new Ast.Exp.Sub(exp, right);
      }

    }
    return exp;
  }

  // AndExp -> LtExp < LtExp
  // -> LtExp
  private Ast.Exp.T parseAndExp() {
    Ast.Exp.T exp, right;
    exp = parseLtExp();

    while (current.kind == Kind.TOKEN_LT) {
      advance();
      right = parseLtExp();
      exp = new Ast.Exp.Lt(exp, right);
    }
    return exp;
  }

  // Exp -> AndExp && AndExp
  // -> AndExp
  private Ast.Exp.T parseExp() {
    Ast.Exp.T exp, right;

    exp = parseAndExp();
    while (current.kind == Kind.TOKEN_AND) {
      advance();
      right = parseAndExp();
      exp = new Ast.Exp.And(exp, right);
    }
    return exp;
  }

  // Statement -> { Statement* }
  // -> if ( Exp ) Statement else Statement
  // -> while ( Exp ) Statement
  // -> System.out.println ( Exp ) ;
  // -> id = Exp ;
  // -> id [ Exp ]= Exp ;
  private Ast.Stm.T parseStatement() {
    // Lab1. Exercise 4: Fill in the missing code
    // to parse a statement.
    Ast.Stm.T stmt = null;
    Ast.Exp.T exp;
    Ast.Stm.T thenn, elsee, body;
    String id;

    switch (current.kind) {

      case TOKEN_LBRACE:
        advance();
        stmt = new Ast.Stm.Block(parseStatements());
        eatToken(Kind.TOKEN_RBRACE);
        break;
      case TOKEN_IF:
        advance();
        eatToken(Kind.TOKEN_LPAREN);
        exp = parseExp();
        eatToken(Kind.TOKEN_RPAREN);
        thenn = parseStatement();
        eatToken(Kind.TOKEN_ELSE);
        elsee = parseStatement();
        stmt = new Ast.Stm.If(exp, thenn, elsee);
        break;
      case TOKEN_WHILE:
        advance();
        eatToken(Kind.TOKEN_LPAREN);
        exp = parseExp();
        eatToken(Kind.TOKEN_RPAREN);
        body = parseStatement();
        stmt = new Ast.Stm.While(exp, body);
        break;
      case TOKEN_SYSTEM:
        advance();
        eatToken(Kind.TOKEN_DOT);
        eatToken(Kind.TOKEN_OUT);
        eatToken(Kind.TOKEN_DOT);
        eatToken(Kind.TOKEN_PRINTLN);
        eatToken(Kind.TOKEN_LPAREN);
        exp = parseExp();
        eatToken(Kind.TOKEN_RPAREN);
        eatToken(Kind.TOKEN_SEMI);
        stmt = new Ast.Stm.Print(exp);
        break;
      case TOKEN_ID:
        id = current.lexeme;
        advance();
        if (current.kind == Kind.TOKEN_ASSIGN) {
          advance();
          exp = parseExp();
          eatToken(Kind.TOKEN_SEMI);
          stmt = new Ast.Stm.Assign(id, exp);
        } else if (current.kind == Kind.TOKEN_LBRACK) {
          advance();
          Ast.Exp.T index = parseExp();
          eatToken(Kind.TOKEN_RBRACK);
          eatToken(Kind.TOKEN_ASSIGN);
          exp = parseExp();
          eatToken(Kind.TOKEN_SEMI);
          stmt = new Ast.Stm.AssignArray(id, index, exp);
        } else {
          error();
        }
        break;
    }

    return stmt;
  }

  // Statements -> Statement Statements
  // ->
  private LinkedList<Ast.Stm.T> parseStatements() {

    LinkedList<Ast.Stm.T> stmts = new LinkedList<>();

    while (current.kind == Kind.TOKEN_LBRACE || current.kind == Kind.TOKEN_IF
        || current.kind == Kind.TOKEN_WHILE
        || current.kind == Kind.TOKEN_SYSTEM || current.kind == Kind.TOKEN_ID) {
      stmts.add(parseStatement());
    }

    return stmts;
  }

  // Type -> int []
  // -> boolean
  // -> int
  // -> id
  private Ast.Type.T parseType() {
    // Lab1. Exercise 4: Fill in the missing code
    // to parse a type.

    Ast.Type.T type = null;

    switch (current.kind) {
      case TOKEN_INT:
        advance();
        type = new Ast.Type.Int();
        if (current.kind == Kind.TOKEN_LBRACK) {
          advance();
          eatToken(Kind.TOKEN_RBRACK);
          type = new Ast.Type.IntArray();
        }
        break;
      case TOKEN_BOOLEAN:
        advance();
        type = new Ast.Type.Boolean();
        break;
      case TOKEN_ID:
        type = new Ast.Type.ClassType(current.lexeme);
        advance();
        break;
      default:
        error();
    }
    return type;
  }

  // VarDecl -> Type id ;
  private Ast.Dec.T parseVarDecl() {
    // to parse the "Type" nonterminal in this method, instead of writing
    // a fresh one.
    Ast.Type.T type = parseType();
    Ast.Dec.T dec = new Ast.Dec.DecSingle(type, current.lexeme);
    eatToken(Kind.TOKEN_ID);
    eatToken(Kind.TOKEN_SEMI);
    return dec;
  }

  // VarDecls -> VarDecl VarDecls
  // ->
  private LinkedList<Ast.Dec.T> parseVarDecls() {

    LinkedList<Ast.Dec.T> decs = new LinkedList<>();

    while (current.kind == Kind.TOKEN_INT || current.kind == Kind.TOKEN_BOOLEAN
        || current.kind == Kind.TOKEN_ID) {


      Token next = lexer.peek();
      if (next.kind == Kind.TOKEN_ID || next.kind == Kind.TOKEN_LBRACK) {
        decs.add(parseVarDecl());
      } else {
        break;
      }
    }
    return decs;
  }

  // FormalList -> Type id FormalRest*
  // ->
  // FormalRest -> , Type id
  private LinkedList<Ast.Dec.T> parseFormalList() {

    LinkedList<Ast.Dec.T> decs = new LinkedList<>();
    Ast.Type.T type;
    String id;

    if (current.kind == Kind.TOKEN_INT || current.kind == Kind.TOKEN_BOOLEAN
        || current.kind == Kind.TOKEN_ID) {
      type = parseType();
      id = current.lexeme;
      eatToken(Kind.TOKEN_ID);

      decs.add(new Ast.Dec.DecSingle(type, id));

      while (current.kind == Kind.TOKEN_COMMER) {
        advance();
        type = parseType();
        id = current.lexeme;
        eatToken(Kind.TOKEN_ID);
        decs.add(new Ast.Dec.DecSingle(type, id));
      }
    }
    return decs;
  }

  // Method -> public Type id ( FormalList )
  // { VarDecl* Statement* return Exp ;}
  private Ast.Method.T parseMethod() {
    // Lab1. Exercise 4: Fill in the missing code
    // to parse a method.

    id2type = new HashMap<>();

    advance();
    Ast.Type.T retType = parseType();
    String id = current.lexeme;
    eatToken(Kind.TOKEN_ID);
    eatToken(Kind.TOKEN_LPAREN);
    LinkedList<Ast.Dec.T> formals = parseFormalList();
    eatToken(Kind.TOKEN_RPAREN);
    eatToken(Kind.TOKEN_LBRACE);

    for (Ast.Dec.T d : formals) {
      Ast.Dec.DecSingle dec = (Ast.Dec.DecSingle)d;
      id2type.put(dec.id, dec.type);
    }


    LinkedList<Ast.Dec.T> locals = parseVarDecls();

    for (Ast.Dec.T d : locals) {
      Ast.Dec.DecSingle dec = (Ast.Dec.DecSingle)d;
      id2type.put(dec.id, dec.type);
    }


    LinkedList<Ast.Stm.T> stmts = parseStatements();

    eatToken(Kind.TOKEN_RETURN);
    Ast.Exp.T retExp = parseExp();
    eatToken(Kind.TOKEN_SEMI);
    eatToken(Kind.TOKEN_RBRACE);
    return new Ast.Method.MethodSingle(retType, id, formals, locals, stmts, retExp);
  }

  // MethodDecls -> MethodDecl MethodDecls
  // ->
  private LinkedList<Ast.Method.T> parseMethodDecls() {

    LinkedList<Ast.Method.T> methods = new LinkedList<>();

    while (current.kind == Kind.TOKEN_PUBLIC) {
      methods.add(parseMethod());
    }
    return methods;
  }

  // ClassDecl -> class id { VarDecl* MethodDecl* }
  // -> class id extends id { VarDecl* MethodDecl* }
  private Ast.Class.T parseClassDecl() {

    String id, extends_ = null;

    eatToken(Kind.TOKEN_CLASS);
    id = current.lexeme;

    eatToken(Kind.TOKEN_ID);
    if (current.kind == Kind.TOKEN_EXTENDS) {
      eatToken(Kind.TOKEN_EXTENDS);
      extends_ = current.lexeme;
      eatToken(Kind.TOKEN_ID);
    }
    eatToken(Kind.TOKEN_LBRACE);
    LinkedList<Ast.Dec.T> decs = parseVarDecls();
    LinkedList<Ast.Method.T> methods = parseMethodDecls();
    eatToken(Kind.TOKEN_RBRACE);
    return new Ast.Class.ClassSingle(id, extends_, decs, methods);
  }

  // ClassDecls -> ClassDecl ClassDecls
  // ->
  private LinkedList<Ast.Class.T> parseClassDecls() {

    LinkedList<Ast.Class.T> classes = new LinkedList<>();

    while (current.kind == Kind.TOKEN_CLASS) {
      classes.add(parseClassDecl());
    }
    return classes;
  }

  // MainClass -> class id
  // {
  // public static void main ( String [] id )
  // {
  // Statement
  // }
  // }
  private Ast.MainClass.T parseMainClass() {
    // Lab1. Exercise 4: Fill in the missing code
    // to parse a main class as described by the
    // grammar above.
    String id, arg;
    Ast.MainClass.T mainClass;
    advance();
    id = current.lexeme;
    eatToken(Kind.TOKEN_ID);
    eatToken(Kind.TOKEN_LBRACE);
    eatToken(Kind.TOKEN_PUBLIC);
    eatToken(Kind.TOKEN_STATIC);
    eatToken(Kind.TOKEN_VOID);
    eatToken(Kind.TOKEN_MAIN);
    eatToken(Kind.TOKEN_LPAREN);
    eatToken(Kind.TOKEN_STRING);
    eatToken(Kind.TOKEN_LBRACK);
    eatToken(Kind.TOKEN_RBRACK);
    arg = current.lexeme;
    eatToken(Kind.TOKEN_ID);
    eatToken(Kind.TOKEN_RPAREN);
    eatToken(Kind.TOKEN_LBRACE);


    mainClass = new Ast.MainClass.MainClassSingle(
        id, arg, parseStatement()
    );
    eatToken(Kind.TOKEN_RBRACE);
    eatToken(Kind.TOKEN_RBRACE);

    return mainClass;
  }

  // Program -> MainClass ClassDecl*
  private ast.Ast.Program.T parseProgram() {
    Ast.MainClass.T mainClass = parseMainClass();
    LinkedList<Ast.Class.T> classes = parseClassDecls();
    Ast.Program.T program = new Ast.Program.ProgramSingle(mainClass, classes);
    eatToken(Kind.TOKEN_EOF);
    return program;
  }

  public ast.Ast.Program.T parse() {
    return parseProgram();
  }
}
