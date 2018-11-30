package codegen.bytecode;

import codegen.bytecode.Ast.Class;
import codegen.bytecode.Ast.Class.ClassSingle;
import codegen.bytecode.Ast.*;
import codegen.bytecode.Ast.Dec.DecSingle;
import codegen.bytecode.Ast.MainClass.MainClassSingle;
import codegen.bytecode.Ast.Method.MethodSingle;
import codegen.bytecode.Ast.Program.ProgramSingle;
import codegen.bytecode.Ast.Stm.*;
import codegen.bytecode.Ast.Type.Int;
import util.Bug;
import util.Label;

import java.util.Hashtable;
import java.util.LinkedList;

// Given a Java ast, translate it into Java bytecode.

public class TranslateVisitor implements ast.Visitor {
  private ClassTable table;
  private String classId;
  private int index;
  private Hashtable<String, Integer> indexTable;
  private Type.T type; // type after translation
  private Dec.T dec;
  private LinkedList<T> stms;
  private Method.T method;
  private Class.T classs;
  private MainClass.T mainClass;
  public Program.T program;

  public TranslateVisitor() {
    this.table = new ClassTable();
    this.classId = null;
    this.indexTable = null;
    this.type = null;
    this.dec = null;
    this.stms = new LinkedList<>();
    this.method = null;
    this.classs = null;
    this.mainClass = null;
    this.program = null;
  }

  private void emit(T s) {
    this.stms.add(s);
  }

  // /////////////////////////////////////////////////////
  // expressions
  @Override
  public void visit(ast.Ast.Exp.Add e) {
    emit(new Comment(e.toString()));
    e.left.accept(this);
    e.right.accept(this);
    emit(new Iadd());
  }

  @Override
  public void visit(ast.Ast.Exp.And e) {
    emit(new Comment(e.toString()));
    e.left.accept(this);
    e.right.accept(this);

    emit(new Iand());
  }

  @Override
  public void visit(ast.Ast.Exp.ArraySelect e) {
    e.array.accept(this);
    e.index.accept(this);
    emit(new Iaload());
  }

  @Override
  public void visit(ast.Ast.Exp.Call e) {
    e.exp.accept(this);
    for (ast.Ast.Exp.T x : e.args) {
      x.accept(this);
    }
    e.rt.accept(this);
    Type.T rt = this.type;
    LinkedList<Type.T> at = new LinkedList<>();
    for (ast.Ast.Type.T t : e.at) {
      t.accept(this);
      at.add(this.type);
    }
    emit(new Invokevirtual(e.id, e.type, at, rt));
  }

  @Override
  public void visit(ast.Ast.Exp.False e) {
    emit(new Ldc(0));
  }

  @Override
  public void visit(ast.Ast.Exp.Id e) {
    Integer index = this.indexTable.get(e.id);
    ClassBinding cb = table.get(classId);
    if (cb == null) {
      new Bug();
    }

    if (e.type == null) {
      for (Tuple t : cb.fields) {
        if (t.id.equals(e.id)) {
          this.type = t.type;
          break;
        }
      }
    } else {
      e.type.accept(this);
    }

    if (index == null) {
      emit(new Aload(0));
      emit(new Getfield(this.type, classId, e.id));
    } else {
      // local variables
      ast.Ast.Type.T type = e.type;
      if (type.getNum() > 0)// a reference
        emit(new Aload(index));
      else
        emit(new Iload(index));
    }
  }

  @Override
  public void visit(ast.Ast.Exp.Length e) {
    e.array.accept(this);
    emit(new Iarraylength());
  }

  @Override
  public void visit(ast.Ast.Exp.Lt e) {

    emit(new Comment(e.toString()));

    Label tl = new Label(), fl = new Label(), el = new Label();
    e.left.accept(this);
    e.right.accept(this);
    emit(new Ificmplt(tl));
    emit(new LabelJ(fl));
    emit(new Ldc(0));
    emit(new Goto(el));
    emit(new LabelJ(tl));
    emit(new Ldc(1));
    emit(new Goto(el));
    emit(new LabelJ(el));
  }

  @Override
  public void visit(ast.Ast.Exp.NewIntArray e) {
    e.exp.accept(this);
    emit(new Inewarray("int"));
  }

  @Override
  public void visit(ast.Ast.Exp.NewObject e) {
    emit(new New(e.id));
  }

  @Override
  public void visit(ast.Ast.Exp.Not e) {
    Label label1 = new Label(), label2 = new Label();
    e.exp.accept(this);
    emit(new Ifne(label1));
    emit(new Ldc(1));
    emit(new Goto(label2));
    emit(new LabelJ(label1));
    emit(new Ldc(0));
    emit(new LabelJ(label2));
  }

  @Override
  public void visit(ast.Ast.Exp.Num e) {
    emit(new Ldc(e.num));
  }

  @Override
  public void visit(ast.Ast.Exp.Sub e) {
    e.left.accept(this);
    e.right.accept(this);
    emit(new Isub());
  }

  @Override
  public void visit(ast.Ast.Exp.This e) {
    emit(new Aload(0));
  }

  @Override
  public void visit(ast.Ast.Exp.Times e) {
    e.left.accept(this);
    e.right.accept(this);
    emit(new Imul());
  }

  @Override
  public void visit(ast.Ast.Exp.True e) {
    emit(new Ldc(1));
  }

  // ///////////////////////////////////////////////////
  // statements
  @Override
  public void visit(ast.Ast.Stm.Assign s) {

    Integer index = this.indexTable.get(s.id);

    if (index == null) {
      // maybe instance field
      ClassBinding cb = this.table.get(classId);
      if (cb == null) {
        new Bug();
      }

      emit(new Aload(0));
      s.exp.accept(this);
      s.type.accept(this);
      emit(new Putfield(this.type, classId, s.id));
    } else {

      s.exp.accept(this);
      ast.Ast.Type.T type = s.type;
      if (type.getNum() > 0)
        emit(new Astore(index));
      else
        emit(new Istore(index));
    }
  }

  @Override
  public void visit(ast.Ast.Stm.AssignArray s) {
    Integer index = this.indexTable.get(s.id);
    if (index == null) {
      // maybe instance field
      ClassBinding cb = this.table.get(classId);
      if (cb == null) {
        new Bug();
      }
      emit(new Aload(0));
      emit(new Getfield(new Type.IntArray(), classId, s.id));


    } else {
      emit(new Astore(index));
    }

    s.index.accept(this);
    s.exp.accept(this);
    emit(new Iastore());

  }

  @Override
  public void visit(ast.Ast.Stm.Block s) {
    for (ast.Ast.Stm.T stm : s.stms) {
      stm.accept(this);
    }
  }

  @Override
  public void visit(ast.Ast.Stm.If s) {
    Label tl = new Label(), fl = new Label(), el = new Label();
    s.condition.accept(this);

    emit(new Ifne(tl));
    emit(new LabelJ(fl));
    s.elsee.accept(this);
    emit(new Goto(el));
    emit(new LabelJ(tl));
    s.thenn.accept(this);
    emit(new Goto(el));
    emit(new LabelJ(el));
  }

  @Override
  public void visit(ast.Ast.Stm.Print s) {
    s.exp.accept(this);
    emit(new Print());
  }

  @Override
  public void visit(ast.Ast.Stm.While s) {
    Label condLabel = new Label(), quitLabel = new Label();
    emit(new LabelJ(condLabel));
    s.condition.accept(this);
    emit(new Ifeq(quitLabel));
    s.body.accept(this);
    emit(new Goto(condLabel));
    emit(new LabelJ(quitLabel));
  }

  // type
  @Override
  public void visit(ast.Ast.Type.Boolean t) {
    this.type = new Int();
  }

  @Override
  public void visit(ast.Ast.Type.ClassType t) {
    this.type = new Type.ClassType(t.id);
  }

  @Override
  public void visit(ast.Ast.Type.Int t) {
    this.type = new Int();
  }

  @Override
  public void visit(ast.Ast.Type.IntArray t) {
    this.type = new Type.IntArray();
  }

  // dec
  @Override
  public void visit(ast.Ast.Dec.DecSingle d) {
    d.type.accept(this);
    this.dec = new DecSingle(this.type, d.id);

    if (this.indexTable != null) {
      this.indexTable.put(d.id, index++);
    }
  }

  // method
  @Override
  public void visit(ast.Ast.Method.MethodSingle m) {
    // record, in a hash table, each var's index
    // this index will be used in the load store operation
    this.index = 1;
    this.indexTable = new Hashtable<>();

    m.retType.accept(this);
    Type.T newRetType = this.type;
    LinkedList<Dec.T> newFormals = new LinkedList<>();
    for (ast.Ast.Dec.T d : m.formals) {
      d.accept(this);
      newFormals.add(this.dec);
    }
    LinkedList<Dec.T> locals = new LinkedList<>();
    for (ast.Ast.Dec.T d : m.locals) {
      d.accept(this);
      locals.add(this.dec);
    }
    this.stms = new LinkedList<>();
    for (ast.Ast.Stm.T s : m.stms) {
      s.accept(this);
    }

    // return statement is specially treated
    m.retExp.accept(this);

    if (m.retType.getNum() > 0)
      emit(new Areturn());
    else
      emit(new Ireturn());

    this.method = new MethodSingle(newRetType, m.id, this.classId, newFormals,
        locals, this.stms, 0, this.index);

  }

  // class
  @Override
  public void visit(ast.Ast.Class.ClassSingle c) {
    this.classId = c.id;
    LinkedList<Dec.T> newDecs = new LinkedList<>();

    this.index = 1;
    this.indexTable = new Hashtable<>();

    for (ast.Ast.Dec.T dec : c.decs) {
      dec.accept(this);
      newDecs.add(this.dec);
    }
    LinkedList<Method.T> newMethods = new LinkedList<>();
    for (ast.Ast.Method.T m : c.methods) {
      m.accept(this);
      newMethods.add(this.method);
    }
    this.classs = new ClassSingle(c.id, c.extendss, newDecs, newMethods);
  }

  // main class
  @Override
  public void visit(ast.Ast.MainClass.MainClassSingle c) {
    c.stm.accept(this);
    this.mainClass = new MainClassSingle(c.id, c.arg, this.stms);
    this.stms = new LinkedList<>();
  }

  // program
  @Override
  public void visit(ast.Ast.Program.ProgramSingle p) {

    scanProgram(p);

    // do translations
    p.mainClass.accept(this);

    LinkedList<Class.T> newClasses = new LinkedList<>();
    for (ast.Ast.Class.T classes : p.classes) {
      classes.accept(this);
      newClasses.add(this.classs);
    }
    this.program = new ProgramSingle(this.mainClass, newClasses);
  }

  // /////////////////////////////////////////////////////
  // the first pass
  private void scanMain(ast.Ast.MainClass.T m) {
    this.table.init(((ast.Ast.MainClass.MainClassSingle) m).id, null);
    // this is a special hacking in that we don't want to
    // enter "main" into the table.
  }

  private void scanClasses(LinkedList<ast.Ast.Class.T> cs) {
    // put empty chuncks into the table
    for (ast.Ast.Class.T c : cs) {
      ast.Ast.Class.ClassSingle cc = (ast.Ast.Class.ClassSingle) c;
      this.table.init(cc.id, cc.extendss);
    }

    // put class fields and methods into the table
    for (ast.Ast.Class.T c : cs) {
      ast.Ast.Class.ClassSingle cc = (ast.Ast.Class.ClassSingle) c;
      LinkedList<Dec.T> newDecs = new LinkedList<>();
      for (ast.Ast.Dec.T dec : cc.decs) {
        dec.accept(this);
        newDecs.add(this.dec);
      }
      this.table.initDecs(cc.id, newDecs);

      // all methods
      LinkedList<ast.Ast.Method.T> methods = cc.methods;
      for (ast.Ast.Method.T mthd : methods) {
        ast.Ast.Method.MethodSingle m = (ast.Ast.Method.MethodSingle) mthd;
        LinkedList<Dec.T> newArgs = new LinkedList<>();
        for (ast.Ast.Dec.T arg : m.formals) {
          arg.accept(this);
          newArgs.add(this.dec);
        }
        m.retType.accept(this);
        Type.T newRet = this.type;
        this.table.initMethod(cc.id, newRet, newArgs, m.id);
      }
    }

    // calculate all inheritance information
    for (ast.Ast.Class.T c : cs) {
      ast.Ast.Class.ClassSingle cc = (ast.Ast.Class.ClassSingle) c;
      this.table.inherit(cc.id);
    }
  }

  private void scanProgram(ast.Ast.Program.T p) {
    ast.Ast.Program.ProgramSingle pp = (ast.Ast.Program.ProgramSingle) p;
    scanMain(pp.mainClass);
    scanClasses(pp.classes);
  }

  public ClassTable getClassTable() {
    return table;
  }
}
