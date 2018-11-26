package codegen.C;

import codegen.C.Ast.Class.ClassSingle;
import codegen.C.Ast.*;
import codegen.C.Ast.Dec.DecSingle;
import codegen.C.Ast.Exp.*;
import codegen.C.Ast.MainMethod.MainMethodSingle;
import codegen.C.Ast.Method.MethodSingle;
import codegen.C.Ast.Program.ProgramSingle;
import codegen.C.Ast.Stm.*;
import codegen.C.Ast.Type.ClassType;
import codegen.C.Ast.Type.Int;
import codegen.C.Ast.Type.IntArray;
import codegen.C.Ast.Vtable.VtableSingle;
import control.Control;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

class MethodLocals {

  private Set<String> locals;

  MethodLocals(MethodSingle method) {

    locals = new HashSet<>();

    DecSingle dec;
    for (Dec.T local : method.formals) {
      dec = (DecSingle) local;
      locals.add(dec.id);
    }

    for (Dec.T local : method.locals) {
      dec = (DecSingle) local;
      locals.add(dec.id);
    }
  }

  boolean contains(String local) {
    return locals.contains(local);
  }
}

class MethodInfo {
  private Map<String, Map<String, MethodLocals>> info;

  MethodInfo() {
    info = new HashMap<>();
  }

  boolean contains(String className, String methodName, String var) {
    Map<String, MethodLocals> locals = info.get(className);
    if (locals != null) {
      MethodLocals m = locals.get(methodName);
      if (m != null) {
        return m.contains(var);
      }
    }
    return false;
  }

  void add(MethodSingle method) {
    String className = method.classId;
    String methodName = method.id;

    if (!info.containsKey(className)) {
      info.put(className, new HashMap<>());
    }

    Map<String, MethodLocals> mp = info.get(className);
    mp.put(methodName, new MethodLocals(method));
  }

}

public class PrettyPrintVisitor implements Visitor {
  private int indentLevel;
  private java.io.BufferedWriter writer;

  private MethodInfo methodInfo;
  private String className;
  private String methodName;


  public PrettyPrintVisitor(TranslateVisitor visitor) {
    this.indentLevel = 2;

    this.methodInfo = new MethodInfo();
    for (Method.T method : visitor.methods) {
      methodInfo.add((MethodSingle) method);
    }

    this.methodName = null;
  }

  private void indent() {
    this.indentLevel += 2;
  }

  private void unIndent() {
    this.indentLevel -= 2;
  }

  private void printSpaces() {
    int i = this.indentLevel;
    while (i-- != 0)
      this.say(" ");
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

  private void say(String s) {
    try {
      this.writer.write(s);
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }
  }

  private void verifyClassField(String var) {
    if (className != null && methodName != null) {
      if (!methodInfo.contains(className, methodName, var)) {
        this.say("this->");
      }
    }
  }

  // /////////////////////////////////////////////////////
  // expressions
  @Override
  public void visit(Add e) {
    e.left.accept(this);
    this.say(" + ");
    e.right.accept(this);
  }

  @Override
  public void visit(And e) {
    e.left.accept(this);
    this.say(" && ");
    e.right.accept(this);
  }

  @Override
  public void visit(ArraySelect e) {
    e.array.accept(this);
    this.say("[");
    e.index.accept(this);
    this.say("]");
  }

  @Override
  public void visit(Call e) {
    this.say("(" + e.assign + "=");
    e.exp.accept(this);
    this.say(", ");
    this.say(e.assign + "->vptr->" + e.id + "(" + e.assign);
    int size = e.args.size();
    if (size == 0) {
      this.say("))");
      return;
    }
    for (Exp.T x : e.args) {
      this.say(", ");
      x.accept(this);
    }
    this.say("))");
  }

  @Override
  public void visit(Id e) {

    verifyClassField(e.id);

    this.say(e.id);
  }

  @Override
  public void visit(Length e) {
    this.say("length");
  }

  @Override
  public void visit(Lt e) {
    e.left.accept(this);
    this.say(" < ");
    e.right.accept(this);
  }

  @Override
  public void visit(NewIntArray e) {
    this.say("(int*)malloc(sizeof(int) * (");
    e.exp.accept(this);
    this.say("))");
  }

  @Override
  public void visit(NewObject e) {
    this.say("((struct " + e.id + "*)(Tiger_new (&" + e.id
        + "_vtable_, sizeof(struct " + e.id + "))))");
  }

  @Override
  public void visit(Not e) {
    this.say("!(");
    e.exp.accept(this);
    this.say(")");
  }

  @Override
  public void visit(Num e) {
    this.say(Integer.toString(e.num));
  }

  @Override
  public void visit(Sub e) {
    e.left.accept(this);
    this.say(" - ");
    e.right.accept(this);
  }

  @Override
  public void visit(This e) {
    this.say("this");
  }

  @Override
  public void visit(Times e) {
    e.left.accept(this);
    this.say(" * ");
    e.right.accept(this);
  }

  // statements
  @Override
  public void visit(Assign s) {
    this.printSpaces();

    verifyClassField(s.id);

    this.say(s.id + " = ");

    s.exp.accept(this);
    this.sayln(";");
  }

  @Override
  public void visit(AssignArray s) {
    this.printSpaces();

    verifyClassField(s.id);
    this.say(s.id + "[");

    s.index.accept(this);
    this.say("] = ");
    s.exp.accept(this);
    this.sayln(";");
  }

  @Override
  public void visit(Block s) {
    this.printSpaces();
    this.sayln("{");
    this.indent();
    for (Stm.T stm : s.stms) {
      stm.accept(this);
    }
    this.unIndent();
    this.printSpaces();
    this.sayln("}");
  }

  @Override
  public void visit(If s) {
    this.printSpaces();
    this.say("if (");
    s.condition.accept(this);
    this.sayln(")");
    this.indent();
    s.thenn.accept(this);
    this.unIndent();
    this.sayln("");
    this.printSpaces();
    this.sayln("else");
    this.indent();
    s.elsee.accept(this);
    this.sayln("");
    this.unIndent();
  }

  @Override
  public void visit(Print s) {
    this.printSpaces();
    this.say("System_out_println (");
    s.exp.accept(this);
    this.sayln(");");
  }

  @Override
  public void visit(While s) {
    this.printSpaces();
    this.say("while ( ");
    s.condition.accept(this);
    this.sayln(" )");
    this.indent();
    s.body.accept(this);
    this.unIndent();
  }

  // type
  @Override
  public void visit(ClassType t) {
    this.say("struct " + t.id + " *");
  }

  @Override
  public void visit(Int t) {
    this.say("int");
  }

  @Override
  public void visit(IntArray t) {
    this.say("int*");
  }

  // dec
  @Override
  public void visit(DecSingle d) {
    d.type.accept(this);
    this.sayln(" " + d.id + ";");
  }

  // method
  @Override
  public void visit(MethodSingle m) {

    this.methodName = m.id;
    this.className = m.classId;

    m.retType.accept(this);
    this.say(" " + m.classId + "_" + m.id + "(");
    int size = m.formals.size();
    for (Dec.T d : m.formals) {
      DecSingle dec = (DecSingle) d;
      size--;
      dec.type.accept(this);
      this.say(" " + dec.id);
      if (size > 0)
        this.say(", ");
    }
    this.sayln(")");
    this.sayln("{");

    for (Dec.T d : m.locals) {
      DecSingle dec = (DecSingle) d;
      this.say("  ");
      dec.type.accept(this);
      this.say(" " + dec.id + ";\n");
    }
    this.sayln("");
    for (Stm.T s : m.stms)
      s.accept(this);
    this.say("  return ");
    m.retExp.accept(this);
    this.sayln(";");
    this.sayln("}");
    this.methodName = null;
    this.className = null;
  }

  @Override
  public void visit(MainMethodSingle m) {

    this.methodName = "Tiger_main";
    this.className = null;

    this.sayln("int Tiger_main ()");
    this.sayln("{");
    for (Dec.T dec : m.locals) {
      this.say("  ");
      DecSingle d = (DecSingle) dec;
      d.type.accept(this);
      this.say(" ");
      this.sayln(d.id + ";");
    }
    m.stm.accept(this);
    this.sayln("}\n");

    this.methodName = null;
  }

  // vtables
  @Override
  public void visit(VtableSingle v) {
    this.sayln("struct " + v.id + "_vtable");
    this.sayln("{");
    for (Ftuple t : v.ms) {
      this.say("  ");
      t.ret.accept(this);

      this.say(" (*" + t.id + ")(struct " + t.classs + "*");
      int size = t.args.size();
      if (size > 0) {
        this.say(", ");
      }
      for (int i = 0; i < size; i++) {
        DecSingle dec = (DecSingle) t.args.get(i);
        dec.type.accept(this);
        if (i != size - 1) {
          this.say(", ");
        }
      }

      this.sayln(");");
    }
    this.sayln("};\n");
  }

  private void outputVtable(VtableSingle v) {
    this.sayln("struct " + v.id + "_vtable " + v.id + "_vtable_ = ");
    this.sayln("{");
    for (Ftuple t : v.ms) {
      this.say("  ");
      this.sayln(t.classs + "_" + t.id + ",");
    }
    this.sayln("};\n");
  }

  // class
  @Override
  public void visit(ClassSingle c) {
    this.sayln("struct " + c.id);
    this.sayln("{");
    this.sayln("  struct " + c.id + "_vtable *vptr;");
    for (Tuple t : c.decs) {
      this.say("  ");
      t.type.accept(this);
      this.say(" ");
      this.sayln(t.id + ";");
    }
    this.sayln("};");

  }

  private void outputSignature(MethodSingle m) {
    m.retType.accept(this);
    this.say(" " + m.classId + "_" + m.id + "(");
    int size = m.formals.size();
    for (Dec.T d : m.formals) {
      DecSingle dec = (DecSingle) d;
      size--;
      dec.type.accept(this);
      this.say(" " + dec.id);
      if (size > 0)
        this.say(", ");
    }
    this.sayln(");");
  }

  // program
  @Override
  public void visit(ProgramSingle p) {
    // we'd like to output to a file, rather than the "stdout".
    try {
      String outputName;
      if (Control.ConCodeGen.outputName != null)
        outputName = Control.ConCodeGen.outputName;
      else if (Control.ConCodeGen.fileName != null)
        outputName = Control.ConCodeGen.fileName + ".c";
      else
        outputName = "a.c";

      this.writer = new java.io.BufferedWriter(new java.io.OutputStreamWriter(
          new java.io.FileOutputStream(outputName)));
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }

    this.sayln("// This is automatically generated by the Tiger compiler.");
    this.sayln("// Do NOT modify!\n");

    this.sayln("// structures");
    for (Ast.Class.T c : p.classes) {
      c.accept(this);
    }

    this.sayln("// method signature");
    for (Method.T m : p.methods) {
      outputSignature((MethodSingle) m);
    }

    this.sayln("// vtables structures");
    for (Vtable.T v : p.vtables) {
      v.accept(this);
    }
    this.sayln("");

    this.sayln("// vtables");
    for (Vtable.T v : p.vtables) {
      outputVtable((VtableSingle) v);
    }
    this.sayln("");

    this.sayln("// methods");
    for (Method.T m : p.methods) {
      m.accept(this);
    }
    this.sayln("");

    this.sayln("// main method");
    p.mainMethod.accept(this);
    this.sayln("");

    this.say("\n\n");

    try {
      this.writer.close();
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }

  }

}
