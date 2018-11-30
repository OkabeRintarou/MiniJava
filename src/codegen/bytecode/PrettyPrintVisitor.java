package codegen.bytecode;

import codegen.bytecode.Ast.Class;
import codegen.bytecode.Ast.Class.ClassSingle;
import codegen.bytecode.Ast.Dec;
import codegen.bytecode.Ast.Dec.DecSingle;
import codegen.bytecode.Ast.MainClass.MainClassSingle;
import codegen.bytecode.Ast.Method;
import codegen.bytecode.Ast.Method.MethodSingle;
import codegen.bytecode.Ast.Program.ProgramSingle;
import codegen.bytecode.Ast.Stm.*;
import codegen.bytecode.Ast.Type.ClassType;
import codegen.bytecode.Ast.Type.Int;
import codegen.bytecode.Ast.Type.IntArray;
import util.Bug;

public class PrettyPrintVisitor implements Visitor {
  private java.io.BufferedWriter writer;
  private ClassTable table;

  public PrettyPrintVisitor(ClassTable table) {
    this.table = table;
  }

  private void sayln(String s) {
    say(s);
    try {
      this.writer.write("\n");
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }
  }

  private void isayln(String s) {
    say("    ");
    say(s);
    try {
      this.writer.write("\n");
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }
  }

  private void isay(String s) {
    say("    " + s);
  }

  private void say(String s) {
    try {
      this.writer.write(s);
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }
  }

  @Override
  public void visit(Comment s) {
    this.sayln("; " + s);
  }

  // /////////////////////////////////////////////////////
  // statements


  @Override
  public void visit(Iaload s) {
    this.isayln("iaload");
  }

  @Override
  public void visit(Iastore s) {
    this.isayln("iastore");
  }

  @Override
  public void visit(Aload s) {
    this.isayln("aload " + s.index);
  }

  @Override
  public void visit(Areturn s) {
    this.isayln("areturn");
  }

  @Override
  public void visit(Astore s) {
    this.isayln("astore " + s.index);
  }

  @Override
  public void visit(Putfield s) {
    this.isay("putfield " + s.className + "/" + s.fieldName + " ");
    s.type.accept(this);
    say("\n");
  }

  @Override
  public void visit(Getfield s) {
    this.isay("getfield " + s.className + "/" + s.fieldName + " ");
    s.type.accept(this);
    say("\n");
  }

  @Override
  public void visit(Goto s) {
    this.isayln("goto " + s.l.toString());
  }

  @Override
  public void visit(Ificmplt s) {
    this.isayln("if_icmplt " + s.l.toString());
  }

  @Override
  public void visit(Ifne s) {
    this.isayln("ifne " + s.l.toString());
  }

  @Override
  public void visit(Ifeq s) {
    this.isayln("ifeq " + s.l.toString());
  }

  @Override
  public void visit(Iload s) {
    this.isayln("iload " + s.index);
  }

  @Override
  public void visit(Imul s) {
    this.isayln("imul");
  }

  @Override
  public void visit(Iand s) {
    this.isayln("iand");
  }

  @Override
  public void visit(Invokevirtual s) {
    this.say("    invokevirtual " + s.c + "/" + s.f + "(");
    ClassBinding cb = table.get(s.c);
    if (cb == null) {
      new Bug();
    }

    for (Ftuple method : cb.methods) {
      if (method.id.equals(s.f)) {
        for (Dec.T d : method.args) {
          ((DecSingle) d).type.accept(this);
        }

        break;
      }

    }

    this.say(")");
    s.rt.accept(this);
    this.sayln("");
  }

  @Override
  public void visit(Iarraylength s) {
    this.isayln("arraylength");
  }

  @Override
  public void visit(Inewarray s) {
    this.isayln("newarray " + s.type);
  }

  @Override
  public void visit(Ireturn s) {
    this.isayln("ireturn");
  }

  @Override
  public void visit(Istore s) {
    this.isayln("istore " + s.index);
  }

  @Override
  public void visit(Isub s) {
    this.isayln("isub");
  }

  @Override
  public void visit(Iadd s) {
    this.isayln("iadd");
  }

  @Override
  public void visit(LabelJ s) {
    this.sayln(s.l.toString() + ":");
  }

  @Override
  public void visit(Ldc s) {
    this.isayln("ldc " + s.i);
  }

  @Override
  public void visit(New s) {
    this.isayln("new " + s.c);
    this.isayln("dup");
    this.isayln("invokespecial " + s.c + "/<init>()V");
  }

  @Override
  public void visit(Print s) {
    this.isayln("getstatic java/lang/System/out Ljava/io/PrintStream;");
    this.isayln("swap");
    this.isayln("invokevirtual java/io/PrintStream/println(I)V");
  }

  // type
  @Override
  public void visit(ClassType t) {
    this.say("L" + t.id + ";");
  }

  @Override
  public void visit(Int t) {
    this.say("I");
  }

  @Override
  public void visit(IntArray t) {
    this.say("[I");
  }

  // dec
  @Override
  public void visit(DecSingle d) {
  }

  // method
  @Override
  public void visit(MethodSingle m) {
    this.say(".method public " + m.id + "(");
    for (Dec.T d : m.formals) {
      DecSingle dd = (DecSingle) d;
      dd.type.accept(this);
    }
    this.say(")");
    m.retType.accept(this);
    this.sayln("");
    this.sayln(".limit stack 4096");
    this.sayln(".limit locals " + (m.index + 1));

    for (T s : m.stms)
      s.accept(this);

    this.sayln(".end method");
  }

  // class
  @Override
  public void visit(ClassSingle c) {
    // Every class must go into its own class file.
    try {
      this.writer = new java.io.BufferedWriter(new java.io.OutputStreamWriter(
          new java.io.FileOutputStream(c.id + ".j")));
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }

    // header
    this.sayln("; This is automatically generated by the Tiger compiler.");
    this.sayln("; Do NOT modify!\n");

    this.sayln(".class public " + c.id);
    if (c.extendss == null)
      this.sayln(".super java/lang/Object\n");
    else
      this.sayln(".super " + c.extendss);

    // fields
    for (Dec.T d : c.decs) {
      DecSingle dd = (DecSingle) d;
      this.say(".field public " + dd.id + " ");
      dd.type.accept(this);
      this.sayln("");
    }

    // methods
    this.sayln(".method public <init>()V");
    this.isayln("aload 0");
    if (c.extendss == null)
      this.isayln("invokespecial java/lang/Object/<init>()V");
    else
      this.isayln("invokespecial " + c.extendss + "/<init>()V");
    this.isayln("return");
    this.sayln(".end method\n\n");

    for (Method.T m : c.methods) {
      m.accept(this);
    }

    try {
      this.writer.close();
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }
  }

  // main class
  @Override
  public void visit(MainClassSingle c) {
    // Every class must go into its own class file.
    try {
      this.writer = new java.io.BufferedWriter(new java.io.OutputStreamWriter(
          new java.io.FileOutputStream(c.id + ".j")));
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }

    this.sayln("; This is automatically generated by the Tiger compiler.");
    this.sayln("; Do NOT modify!\n");

    this.sayln(".class public " + c.id);
    this.sayln(".super java/lang/Object\n");
    this.sayln(".method public static main([Ljava/lang/String;)V");
    this.isayln(".limit stack 4096");
    this.isayln(".limit locals 2");
    for (T s : c.stms)
      s.accept(this);
    this.isayln("return");
    this.sayln(".end method");

    try {
      this.writer.close();
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }
  }

  // program
  @Override
  public void visit(ProgramSingle p) {

    p.mainClass.accept(this);

    for (Class.T c : p.classes) {
      c.accept(this);
    }

  }

}
