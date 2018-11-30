package codegen.bytecode;

import codegen.bytecode.Ast.Class.ClassSingle;
import codegen.bytecode.Ast.Dec.DecSingle;
import codegen.bytecode.Ast.MainClass.MainClassSingle;
import codegen.bytecode.Ast.Method.MethodSingle;
import codegen.bytecode.Ast.Program.ProgramSingle;
import codegen.bytecode.Ast.Stm.*;
import codegen.bytecode.Ast.Type.ClassType;
import codegen.bytecode.Ast.Type.Int;
import codegen.bytecode.Ast.Type.IntArray;

public interface Visitor {

  void visit(Comment s);

  // statements
  void visit(Iaload s);

  void visit(Iastore s);

  void visit(Aload s);

  void visit(Areturn s);

  void visit(Astore s);

  void visit(Putfield s);

  void visit(Getfield s);

  void visit(Goto s);

  void visit(Ificmplt s);

  void visit(Ifne s);

  void visit(Ifeq s);

  void visit(Iload s);

  void visit(Imul s);

  void visit(Ireturn s);

  void visit(Istore s);

  void visit(Isub s);

  void visit(Iadd s);

  void visit(Iand s);

  void visit(Invokevirtual s);

  void visit(Iarraylength s);

  void visit(Inewarray s);

  void visit(LabelJ s);

  void visit(Ldc s);

  void visit(Print s);

  void visit(New s);

  // type
  void visit(ClassType t);

  void visit(Int t);

  void visit(IntArray t);

  // dec
  void visit(DecSingle d);

  // method
  void visit(MethodSingle m);

  // class
  void visit(ClassSingle c);

  // main class
  void visit(MainClassSingle c);

  // program
  void visit(ProgramSingle p);
}
