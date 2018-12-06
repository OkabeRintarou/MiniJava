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
import util.Bug;

import java.util.*;

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
  private Map<String, Integer> localsSet;
  private Map<String, List<Type.T>> classField;


  public PrettyPrintVisitor(TranslateVisitor visitor) {
    this.indentLevel = 2;

    this.methodInfo = new MethodInfo();
    for (Method.T method : visitor.methods) {
      methodInfo.add((MethodSingle) method);
    }

    this.methodName = null;
    this.localsSet = new HashMap<>();
    this.classField = new HashMap<>();
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

  private boolean verifyClassField(String var) {
    if (className != null && methodName != null) {
      if (!methodInfo.contains(className, methodName, var)) {
        this.say("this->");
        return true;
      }
    }
    return false;
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
    String assign;
    if (localsSet.containsKey(e.assign)) {
      assign = "frame." + e.assign;
    } else {
      assign = e.assign;
    }
    this.say("(" + assign + "=");
    e.exp.accept(this);
    this.say(", ");
    this.say(assign + "->vptr->" + e.id + "(" + assign);
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

    if (!verifyClassField(e.id) &&
        localsSet.containsKey(e.id)) {
      this.say("frame.");
    }

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

    if (!verifyClassField(s.id) &&
        localsSet.containsKey(s.id)) {
      this.say("frame.");
    }

    this.say(s.id + " = ");

    s.exp.accept(this);
    this.sayln(";");
  }

  @Override
  public void visit(AssignArray s) {
    this.printSpaces();

    if (!verifyClassField(s.id) &&
        localsSet.containsKey(s.id)) {
      this.say("frame.");
    }
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

    StringBuilder argumentGCMap = new StringBuilder();
    StringBuilder localGCMap = new StringBuilder();

    this.methodName = m.id;
    this.className = m.classId;
    this.localsSet.clear();

    String structName = outputGCStack(m);
    this.sayln("");

    m.retType.accept(this);
    this.say(" " + m.classId + "_" + m.id + "(");
    int size = m.formals.size();
    for (Dec.T d : m.formals) {
      DecSingle dec = (DecSingle) d;
      size--;
      dec.type.accept(this);

      if (dec.type.isReferenceType()) {
        argumentGCMap.append("1");
      } else {
        argumentGCMap.append("0");
      }

      this.say(" " + dec.id);
      if (size > 0)
        this.say(", ");
    }
    this.sayln(")");
    this.sayln("{");

    this.sayln("  struct " + structName + " frame = {0};");
    this.sayln("  frame.prev_ = prev;");
    this.sayln("  prev = &frame;");
    this.sayln("  frame.argument_base_address = ((int*)((void*)(&this)));");
    this.sayln("  frame.argument_gc_map = \"" + argumentGCMap.toString() + "\";");

    int index = 0;
    for (Dec.T d : m.locals) {
      DecSingle dec = (DecSingle) d;
      localsSet.put(dec.id, index);

      if (dec.type.isReferenceType()) {
        localGCMap.append("1");
      } else {
        localGCMap.append("0");
      }
      index++;
    }
    this.sayln("  frame.locals_gc_map = \"" + localGCMap.toString() + "\";");


    this.sayln("");
    for (Stm.T s : m.stms)
      s.accept(this);
    // pop up frame before return instruction
    this.sayln("  prev = frame.prev_;");

    this.say("  return ");
    m.retExp.accept(this);
    this.sayln(";");
    this.sayln("}");
    this.methodName = null;
    this.className = null;
  }

  private String outputGCStack(MethodSingle m) {

    String structName = m.classId + "_" + m.id + "_gc_frame";

    this.sayln("struct " + structName + " {");
    this.sayln("  void *prev_;");
    this.sayln("  int *argument_base_address;");
    this.sayln("  char *argument_gc_map;");
    this.sayln("  char *locals_gc_map;");
    for (Dec.T d : m.locals) {
      this.say("  ");

      DecSingle dec = (DecSingle) d;
      dec.type.accept(this);
      this.sayln(" " + dec.id + ";");
    }
    this.sayln("};");

    return structName;
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

    this.sayln("  char* " + v.id + "_gc_map;");
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

    StringBuilder sb = new StringBuilder();
    List<Type.T> fields = classField.get(v.id);
    if (fields == null) {
      new Bug();
    }
    for (Type.T t : fields) {
      if (t.isReferenceType()) {
        sb.append("1");
      } else {
        sb.append("0");
      }
    }
    this.sayln("  \"" + sb.toString() + "\",");

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
    this.sayln("  int _isObjOrArray;");
    this.sayln("  unsigned _length;");
    this.sayln("  void* _forwarding;");

    List<Type.T> fields = new ArrayList<>();


    for (Tuple t : c.decs) {
      fields.add(t.type);
      this.say("  ");
      t.type.accept(this);
      this.say(" ");
      this.sayln(t.id + ";");
    }
    this.classField.put(c.id, fields);
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
    this.sayln("#include <malloc.h>");
    this.sayln("extern int System_out_println (int);");
    this.sayln("extern void *Tiger_new (void *, int);");
    this.sayln("extern void *Tiger_new_array (int);");

    this.sayln("// global variables");
    this.sayln("extern void *prev;");
    this.sayln("");


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
