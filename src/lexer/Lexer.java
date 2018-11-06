package lexer;

import lexer.Token.Kind;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static control.Control.ConLexer.dump;

public class Lexer {
  String fname; // the input file name to be compiled
  InputStream fstream; // input stream for the above file
  Map<String, Kind> keywords;
  int lineNumber;
  Token peekToken;

  public Lexer(String fname, InputStream fstream) {
    this.fname = fname;
    this.fstream = fstream;
    this.lineNumber = 1;
    this.peekToken = null;

    keywords = new HashMap<>();
    keywords.put("boolean", Kind.TOKEN_BOOLEAN);
    keywords.put("class", Kind.TOKEN_CLASS);
    keywords.put("else", Kind.TOKEN_ELSE);
    keywords.put("extends", Kind.TOKEN_EXTENDS);
    keywords.put("false", Kind.TOKEN_FALSE);
    keywords.put("if", Kind.TOKEN_IF);
    keywords.put("int", Kind.TOKEN_INT);
    keywords.put("length", Kind.TOKEN_LENGTH);
    keywords.put("main", Kind.TOKEN_MAIN);
    keywords.put("new", Kind.TOKEN_NEW);
    keywords.put("out", Kind.TOKEN_OUT);
    keywords.put("println", Kind.TOKEN_PRINTLN);
    keywords.put("public", Kind.TOKEN_PUBLIC);
    keywords.put("return", Kind.TOKEN_RETURN);
    keywords.put("static", Kind.TOKEN_STATIC);
    keywords.put("String", Kind.TOKEN_STRING);
    keywords.put("System", Kind.TOKEN_SYSTEM);
    keywords.put("this", Kind.TOKEN_THIS);
    keywords.put("true", Kind.TOKEN_TRUE);
    keywords.put("void", Kind.TOKEN_VOID);
    keywords.put("while", Kind.TOKEN_WHILE);
  }

  private static boolean isLetter(int c) {
    return (c >= 'a' && c <= 'z') ||
        (c >= 'A' && c <= 'Z');
  }

  private static boolean isDigit(int c) {
    return c >= '0' && c <= '9';
  }

  private Token identifier(int c) throws Exception {
    StringBuilder sb = new StringBuilder();
    sb.append((char) c);

    fstream.mark(1);
    c = fstream.read();

    while (c == '_' || isLetter(c) || isDigit(c)) {
      sb.append((char) c);
      fstream.mark(1);
      c = fstream.read();
    }
    fstream.reset();

    String lexeme = sb.toString();
    Kind kind;
    if ((kind = keywords.get(lexeme)) != null) {
      return new Token(kind, lineNumber);
    } else {
      return new Token(Kind.TOKEN_ID, lineNumber, lexeme);
    }
  }

  private Token integerLiteral(int c) throws Exception {
    StringBuilder sb = new StringBuilder();
    sb.append((char) c);

    fstream.mark(1);
    c = fstream.read();

    while (isDigit(c)) {
      sb.append((char) c);
      fstream.mark(1);
      c = fstream.read();
    }
    fstream.reset();

    String lexeme = sb.toString();
    return new Token(Kind.TOKEN_NUM, lineNumber, lexeme);
  }

  // When called, return the next token (refer to the code "Token.java")
  // from the input stream.
  // Return TOKEN_EOF when reaching the end of the input stream.
  private Token nextTokenInternal() throws Exception {
    int c = this.fstream.read(), c2;

    if (-1 == c)
      // The value for "lineNum" is now "null",
      // you should modify this to an appropriate
      // line number for the "EOF" token.
      return new Token(Kind.TOKEN_EOF, null);

    // skip all kinds of "blanks" or comments
    while (' ' == c || '\t' == c || '\n' == c || '/' == c) {
      if (c == '\n') {
        ++this.lineNumber;
      } else if (c == '/') {
        c = fstream.read();
        if (c == '/') {
          c = fstream.read();
          while (c != -1) {
            if (c == '\n') {
              ++lineNumber;
              break;
            }
            c = fstream.read();
          }

        }
      }
      c = this.fstream.read();
    }


    if (-1 == c)
      return new Token(Kind.TOKEN_EOF, lineNumber);


    switch (c) {
      case '+':
        return new Token(Kind.TOKEN_ADD, lineNumber);
      case '&':
        c2 = fstream.read();
        if (c2 == '&') {
          return new Token(Kind.TOKEN_AND, lineNumber);
        }
      case '=':
        return new Token(Kind.TOKEN_ASSIGN, lineNumber);
      case ',':
        return new Token(Kind.TOKEN_COMMER, lineNumber);
      case '.':
        return new Token(Kind.TOKEN_DOT, lineNumber);
      case '{':
        return new Token(Kind.TOKEN_LBRACE, lineNumber);
      case '[':
        return new Token(Kind.TOKEN_LBRACK, lineNumber);
      case '(':
        return new Token(Kind.TOKEN_LPAREN, lineNumber);
      case '<':
        return new Token(Kind.TOKEN_LT, lineNumber);
      case '!':
        return new Token(Kind.TOKEN_NOT, lineNumber);
      case '}':
        return new Token(Kind.TOKEN_RBRACE, lineNumber);
      case ']':
        return new Token(Kind.TOKEN_RBRACK, lineNumber);
      case ')':
        return new Token(Kind.TOKEN_RPAREN, lineNumber);
      case ';':
        return new Token(Kind.TOKEN_SEMI, lineNumber);
      case '-':
        return new Token(Kind.TOKEN_SUB, lineNumber);
      case '*':
        return new Token(Kind.TOKEN_TIMES, lineNumber);


      default:
        // Lab 1, exercise 2: supply missing code to
        // lex other kinds of tokens.
        // Hint: think carefully about the basic
        // data structure and algorithms. The code
        // is not that much and may be less than 50 lines. If you
        // find you are writing a lot of code, you
        // are on the wrong way.
        if (isLetter(c)) {
          return identifier(c);
        } else if (isDigit(c)) {
          return integerLiteral(c);
        }
        return null;
    }
  }

  public Token nextToken() {
    Token t = null;

    if (peekToken != null) {
      t = peekToken;
      peekToken = null;
      return t;
    }
    try {
      t = this.nextTokenInternal();
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }
    if (dump)
      System.out.println(t.toString());
    return t;
  }

  public Token peek() {
    if (peekToken != null) {
      System.err.println("at most one token can be looked ahead");
      System.exit(1);
    }
    this.peekToken = nextToken();
    return this.peekToken;
  }
}
