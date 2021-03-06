package ast;

import ast.Ast.*;
import ast.Ast.Exp.*;
import ast.Ast.MainClass.MainClassSingle;
import ast.Ast.Program.ProgramSingle;
import ast.Ast.Stm.Assign;
import ast.Ast.Stm.If;
import ast.Ast.Stm.Print;

public class Fac {
  // Lab2, exercise 2: read the following code and make
  // sure you understand how the sample program "test/Fac.java" is represented.

  // /////////////////////////////////////////////////////
  // To represent the "Fac.java" program in memory manually
  // this is for demonstration purpose only, and
  // no one would want to do this in reality (boring and error-prone).
  /*
   * class Factorial { public static void main(String[] a) {
   * System.out.println(new Fac().ComputeFac(10)); } } class Fac { public int
   * ComputeFac(int num) { int num_aux; if (num < 1) num_aux = 1; else num_aux =
   * num * (this.ComputeFac(num-1)); return num_aux; } }
   */

  // // main class: "Factorial"
  static MainClass.T factorial = new MainClassSingle(
      "Factorial", "a", new Print(new Call(
      new NewObject("Fac"), "ComputeFac",
      new util.Flist<Exp.T>().list(new Num(10)))));

  // // class "Fac"
  static Ast.Class.T fac = new Ast.Class.ClassSingle("Fac", null,
      new util.Flist<Dec.T>().list(),
      new util.Flist<Method.T>().list(new Method.MethodSingle(
          new Type.Int(), "ComputeFac", new util.Flist<Dec.T>()
          .list(new Dec.DecSingle(new Type.Int(), "num")),
          new util.Flist<Dec.T>().list(new Dec.DecSingle(
              new Type.Int(), "num_aux")), new util.Flist<Stm.T>()
          .list(new If(new Lt(new Id("num"),
              new Num(1)), new Assign("num_aux",
              new Num(1)), new Assign("num_aux",
              new Times(new Id("num"), new Call(
                  new This(), "ComputeFac",
                  new util.Flist<Exp.T>().list(new Sub(
                      new Id("num"), new Num(1)))))))),
          new Id("num_aux"))));

  // program
  public static Program.T prog = new ProgramSingle(factorial,
      new util.Flist<Ast.Class.T>().list(fac));

  // Lab2, exercise 2: you should write some code to
  // represent the program "test/Sum.java".
  // Your code here:

  // main class: Sum
  static MainClass.T sum = new MainClassSingle(
      "Sum", "a",
      new Print(
          new Call(
              new NewObject("Doit"), "doit",
              new util.Flist<Exp.T>().list(new Num(101))
          )
      )
  );

  static Ast.Class.T doit = new Ast.Class.ClassSingle("Doit", null,
      new util.Flist<Dec.T>().list(),
      new util.Flist<Method.T>().list(
          new Method.MethodSingle(
              new Type.Int(),
              "doit",
              new util.Flist<Dec.T>().list(
                  new Dec.DecSingle(new Type.Int(), "n")
              ),
              new util.Flist<Dec.T>().list(
                  new Dec.DecSingle(new Type.Int(), "sum"),
                  new Dec.DecSingle(new Type.Int(), "i")
              ),
              new util.Flist<Stm.T>().list(
                  new Assign("i", new Num(0)),
                  new Assign("sum", new Num(0)),
                  new Stm.While(
                      new Lt(new Id("i"), new Id("n")),
                      new Stm.Block(new util.Flist<Stm.T>().list(
                          new Assign("sum", new Add(new Id("sum"), new Id("i"))),
                          new Assign("i", new Add(new Id("i"), new Num(1)))
                      ))
                  )
              ),
              new Id("sum")
          )
      )
  );


  public static Program.T sumProg = new ProgramSingle(
      sum,
      new util.Flist<Ast.Class.T>().list(doit)
  );
}
