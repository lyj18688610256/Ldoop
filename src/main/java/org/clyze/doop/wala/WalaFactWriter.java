package org.clyze.doop.wala;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ssa.ConstantValue;
import com.ibm.wala.ssa.SSABinaryOpInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeReference;
import org.clyze.doop.common.Database;
import org.clyze.doop.common.FactEncoders;
import org.clyze.doop.soot.Session;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.clyze.doop.common.PredicateFile.*;

/**
 * FactWriter determines the format of a fact and adds it to a
 * database.
 */
public class WalaFactWriter {
    private Database _db;
    private WalaRepresentation _rep;
    private Map<String, TypeReference> _varTypeMap;

    public WalaFactWriter(Database db) {
        _db = db;
        _rep = WalaRepresentation.getRepresentation();
        _varTypeMap = new ConcurrentHashMap<>();
    }

    private String str(int i) {
        return String.valueOf(i);
    }

    private String writeStringConstant(String constant) {
        String raw = FactEncoders.encodeStringConstant(constant);

        String result;
        if(raw.length() <= 256)
            result = raw;
        else
            result = "<<HASH:" + raw.hashCode() + ">>";

        _db.add(STRING_RAW, result, raw);
        _db.add(STRING_CONST, result);

        return result;
    }

    private String hashMethodNameIfLong(String methodRaw) {
        if (methodRaw.length() <= 1024)
            return methodRaw;
        else
            return "<<METHOD HASH:" + methodRaw.hashCode() + ">>";
    }

    String writeMethod(IMethod m) {
        String methodRaw = _rep.signature(m);
        String result = hashMethodNameIfLong(methodRaw);

        _db.add(STRING_RAW, result, methodRaw);
        _db.add(METHOD, result, _rep.simpleName(m), _rep.descriptor(m), writeType(m.getReference().getDeclaringClass()), writeType(m.getReturnType()), m.getDescriptor().toUnicodeString());
//        if (m.getTag("VisibilityAnnotationTag") != null) {
//            VisibilityAnnotationTag vTag = (VisibilityAnnotationTag) m.getTag("VisibilityAnnotationTag");
//            for (AnnotationTag aTag : vTag.getAnnotations()) {
//                _db.add(METHOD_ANNOTATION, result, soot.coffi.Util.v().jimpleTypeOfFieldDescriptor(aTag.getType()).getEscapedName());
//            }
//        }
//        if (m.getTag("VisibilityParameterAnnotationTag") != null) {
//            VisibilityParameterAnnotationTag vTag = (VisibilityParameterAnnotationTag) m.getTag("VisibilityParameterAnnotationTag");
//
//            ArrayList<VisibilityAnnotationTag> annList = vTag.getVisibilityAnnotations();
//            for (int i = 0; i < annList.size(); i++) {
//                if (annList.get(i) != null) {
//                    for (AnnotationTag aTag : annList.get(i).getAnnotations()) {
//                        _db.add(PARAM_ANNOTATION, result, str(i), soot.coffi.Util.v().jimpleTypeOfFieldDescriptor(aTag.getType()).getEscapedName());
//                    }
//                }
//            }
//        }
        return result;
    }

    void writeClassArtifact(String artifact, String className) { _db.add(CLASS_ARTIFACT, artifact, className); }

    void writeAndroidEntryPoint(IMethod m) {
        _db.add(ANDROID_ENTRY_POINT, _rep.signature(m));
    }

    void writeProperty(String path, String key, String value) {
        String pathId = writeStringConstant(path);
        String keyId = writeStringConstant(key);
        String valueId = writeStringConstant(value);
        _db.add(PROPERTIES, pathId, keyId, valueId);
    }

    void writeClassOrInterfaceType(IClass c) {
        String classStr = c.getName().getClassName().toString();
        if (c.isInterface()) {
            _db.add(INTERFACE_TYPE, classStr);
        }
        else {
            _db.add(CLASS_TYPE, classStr);
        }
        _db.add(CLASS_HEAP, _rep.classConstant(c), classStr);
//        if (c.getTag("VisibilityAnnotationTag") != null) {
//            VisibilityAnnotationTag vTag = (VisibilityAnnotationTag) c.getTag("VisibilityAnnotationTag");
//            for (AnnotationTag aTag : vTag.getAnnotations()) {
//                _db.add(CLASS_ANNOTATION, classStr, soot.coffi.Util.v().jimpleTypeOfFieldDescriptor(aTag.getType()).getEscapedName());
//            }
//        }
    }

    void writeDirectSuperclass(IClass sub, IClass sup) {
        _db.add(DIRECT_SUPER_CLASS, writeType(sub), writeType(sup));
    }

    void writeDirectSuperinterface(IClass clazz, IClass iface) {
        _db.add(DIRECT_SUPER_IFACE, writeType(clazz), writeType(iface));
    }

    private String writeType(IClass c) {
        String classStr = c.getName().getClassName().toString();
        // The type itself is already taken care of by writing the
        // IClass declaration, so we don't actually write the type
        // here, and just return the string.
        return classStr;
    }

    private String writeType(TypeReference t) {
        String result = t.toString();

        if (t.isArrayType()) {
            _db.add(ARRAY_TYPE, result);
            TypeReference componentType = t.getArrayElementType();
            _db.add(COMPONENT_TYPE, result, writeType(componentType));
        }
        else if (t.isPrimitiveType() || t.isReferenceType() || t.isClassType()) {

        }
        else {
            throw new RuntimeException("Don't know what to do with type " + t);
        }

        return result;
    }
//
//    void writeEnterMonitor(IMethod m, Stmt stmt, Local var, Session session) {
//        int index = session.calcUnitNumber(stmt);
//        String insn = _rep.instruction(m, stmt, session, index);
//        String methodId = writeMethod(m);
//
//        _db.add(ENTER_MONITOR, insn, str(index), _rep.local(m, var), methodId);
//    }
//
//    void writeExitMonitor(IMethod m, Stmt stmt, Local var, Session session) {
//        int index = session.calcUnitNumber(stmt);
//        String insn = _rep.instruction(m, stmt, session, index);
//        String methodId = writeMethod(m);
//
//        _db.add(EXIT_MONITOR, insn, str(index), _rep.local(m, var), methodId);
//    }
//
//    void writeAssignLocal(IMethod m, Stmt stmt, Local to, Local from, Session session) {
//        int index = session.calcUnitNumber(stmt);
//        String insn = _rep.instruction(m, stmt, session, index);
//        String methodId = writeMethod(m);
//
//        _db.add(ASSIGN_LOCAL, insn, str(index), _rep.local(m, from), _rep.local(m, to), methodId);
//    }
//
//    void writeAssignLocal(IMethod m, Stmt stmt, Local to, ThisRef ref, Session session) {
//        int index = session.calcUnitNumber(stmt);
//        String insn = _rep.instruction(m, stmt, session, index);
//        String methodId = writeMethod(m);
//
//        _db.add(ASSIGN_LOCAL, insn, str(index), _rep.thisVar(m), _rep.local(m, to), methodId);
//    }
//
//    void writeAssignLocal(IMethod m, Stmt stmt, Local to, ParameterRef ref, Session session) {
//        int index = session.calcUnitNumber(stmt);
//        String insn = _rep.instruction(m, stmt, session, index);
//        String methodId = writeMethod(m);
//
//        _db.add(ASSIGN_LOCAL, insn, str(index), _rep.param(m, ref.getIndex()), _rep.local(m, to), methodId);
//    }
//
//    void writeAssignInvoke(IMethod inMethod, Stmt stmt, Local to, InvokeExpr expr, Session session) {
//        String insn = writeInvokeHelper(inMethod, stmt, expr, session);
//
//        _db.add(ASSIGN_RETURN_VALUE, insn, _rep.local(inMethod, to));
//    }
//
//    void writeAssignHeapAllocation(IMethod m, Stmt stmt, Local l, AnyNewExpr expr, Session session) {
//        String heap = _rep.heapAlloc(m, expr, session);
//
//
//        //_db.add(NORMAL_HEAP, heap, writeType(expr.getType()));
//
//        if (expr instanceof NewArrayExpr) {
//            NewArrayExpr newArray = (NewArrayExpr) expr;
//            Value sizeVal = newArray.getSize();
//
//            if (sizeVal instanceof IntConstant) {
//                IntConstant size = (IntConstant) sizeVal;
//
//                if(size.value == 0)
//                    _db.add(EMPTY_ARRAY, heap);
//            }
//        }
//
//        // statement
//        int index = session.calcUnitNumber(stmt);
//        String insn = _rep.instruction(m, stmt, session, index);
//        String methodId = writeMethod(m);
//        _db.add(ASSIGN_HEAP_ALLOC, insn, str(index), heap, _rep.local(m, l), methodId, ""+getLineNumberFromStmt(stmt));
//    }

    private static int getLineNumberFromStmt(SSAInstruction instruction) {
//        LineNumberTag tag = (LineNumberTag) stmt.getTag("LineNumberTag");
//        String lineNumber;
//        if (tag == null) {
//            return  0;
//        } else {
//            return tag.getLineNumber();
//        }
        return 0;
    }

    private TypeReference getComponentType(TypeReference type) {
        // Soot calls the component type of an array type the "element
        // type", which is rather confusing, since in an array type
        // A[][][], the JVM Spec defines A to be the element type, and
        // A[][] is the component type.
        return type.getArrayElementType();
    }

    /**
     * NewMultiArray is slightly complicated because an array needs to
     * be allocated separately for every dimension of the array.
     */
//    void writeAssignNewMultiArrayExpr(IMethod m, Stmt stmt, Local l, NewMultiArrayExpr expr, Session session) {
//        writeAssignNewMultiArrayExprHelper(m, stmt, l, _rep.local(m,l), expr, expr.getType(), session);
//    }
//
//    private void writeAssignNewMultiArrayExprHelper(IMethod m, Stmt stmt, Local l, String assignTo, NewMultiArrayExpr expr, TypeReference arrayType, Session session) {
//        String heap = _rep.heapMultiArrayAlloc(m, expr, arrayType, session);
//        int index = session.calcUnitNumber(stmt);
//        String insn = _rep.instruction(m, stmt, session, index);
//
//
//        String methodId = writeMethod(m);
//
//        _db.add(NORMAL_HEAP, heap, writeType(arrayType));
//        _db.add(ASSIGN_HEAP_ALLOC, insn, str(index), heap, assignTo, methodId, ""+getLineNumberFromStmt(stmt));
//
//        TypeReference componentType = getComponentType(arrayType);
//        if (componentType.isArrayType()) {
//            String childAssignTo = _rep.newLocalIntermediate(m, l, session);
//            writeAssignNewMultiArrayExprHelper(m, stmt, l, childAssignTo, expr, componentType, session);
//            int storeInsnIndex = session.calcUnitNumber(stmt);
//            String storeInsn = _rep.instruction(m, stmt, session, storeInsnIndex);
//
//            _db.add(STORE_ARRAY_INDEX, storeInsn, str(storeInsnIndex), childAssignTo, assignTo, methodId);
//            _db.add(VAR_TYPE, childAssignTo, writeType(componentType));
//            _db.add(VAR_DECLARING_METHOD, childAssignTo, methodId);
//        }
//    }

    // The commented-out code below is what used to be in Doop2. It is not
    // equivalent to code in old Doop. I (YS) tried to have a more compatible
    // approach for comparison purposes.
    /*
    public void writeAssignNewMultiArrayExpr(IMethod m, Stmt stmt, Local l, NewMultiArrayExpr expr, Session session) {
        // what is a normal object?
        String heap = _rep.heapAlloc(m, expr, session);

        _db.addInput("NormalObject",
                _db.asEntity(heap),
                writeType(expr.getType()));

        // local variable to assign the current array allocation to.
        String assignTo = _rep.local(m, l);

        Type type = (ArrayType) expr.getType();
        int dimensions = 0;
        while(type instanceof ArrayType)
            {
                ArrayType arrayType = (ArrayType) type;

                // make sure we store the type
                writeType(type);

                type = getComponentType(arrayType);
                dimensions++;
            }

        Type elementType = type;

        int index = session.calcUnitNumber(stmt);
        String rep = _rep.instruction(m, stmt, session, index);

        _db.addInput("AssignMultiArrayAllocation",
                _db.asEntity(rep),
                _db.asIntColumn(str(index)),
                _db.asEntity(heap),
                _db.asIntColumn(str(dimensions)),
                _db.asEntity(assignTo),
                _db.asEntity("Method", _rep.method(m)));

    // idea: do generate the heap allocations, but not the assignments
    // (to array indices). Do store the type of those heap allocations
    }
    */

    void writeAssignStringConstant(IMethod m, SSAInstruction instruction, Local l, ConstantValue s) {
        String constant = s.getValue().toString();
        String heapId = writeStringConstant(constant);

        String insn = _rep.instruction(m, instruction);
        String methodId = writeMethod(m);

        _db.add(ASSIGN_HEAP_ALLOC, insn, str(instruction.iindex), heapId, _rep.local(m, l), methodId, ""+getLineNumberFromStmt(instruction));
    }

    void writeAssignNull(IMethod m, SSAInstruction instruction, Local l) {
        String insn = _rep.instruction(m, instruction);
        String methodId = writeMethod(m);

        _db.add(ASSIGN_NULL, insn, str(instruction.iindex), _rep.local(m, l), methodId);
    }

    void writeAssignNumConstant(IMethod m, SSAInstruction instruction, Local l, ConstantValue constant) {
        String insn = _rep.instruction(m, instruction);
        String methodId = writeMethod(m);

        _db.add(ASSIGN_NUM_CONST, insn, str(instruction.iindex), constant.toString(), _rep.local(m, l), methodId);
    }
//
//    private void writeAssignMethodHandleConstant(IMethod m, Stmt stmt, Local l, MethodHandle constant, Session session) {
//        int index = session.calcUnitNumber(stmt);
//        String insn = _rep.instruction(m, stmt, session, index);
//        String handleName = constant.getMethodRef().toString();
//        String heap = _rep.methodHandleConstant(handleName);
//        String methodId = writeMethod(m);
//
//        _db.add(METHOD_HANDLE_CONSTANT, heap, handleName);
//        _db.add(ASSIGN_HEAP_ALLOC, insn, str(index), heap, _rep.local(m, l), methodId, "0");
//    }

    void writeAssignClassConstant(IMethod m, SSAInstruction instruction, Local l, ConstantValue constant) {
        String s = constant.toString().replace('/', '.');
        String heap;
        String actualType;

        /* There is some weirdness in class constants: normal Java class
           types seem to have been translated to a syntax with the initial
           L, but arrays are still represented as [, for example [C for
           char[] */
        if (s.charAt(0) == '[' || (s.charAt(0) == 'L' && s.endsWith(";")) ) {
            // array type
            TypeReference t = TypeReference.find(ClassLoaderReference.Primordial, s);

            heap = _rep.classConstant(t);
            actualType = t.toString();
        }
        else {

            throw new RuntimeException("Unexpected class constant: " + constant);
//            heap =  _rep.classConstant(c);
//            actualType = c.getName();
//            // The code above should be functionally equivalent with the simple code below,
            // but the above causes a concurrent modification exception due to a Soot
            // bug that adds a phantom class to the Scene's hierarchy, although
            // (based on their own comments) it shouldn't.
//            heap = _rep.classConstant(s);
//            actualType = s;
        }

        _db.add(CLASS_HEAP, heap, actualType);

        String insn = _rep.instruction(m, instruction);
        String methodId = writeMethod(m);

        // REVIEW: the class object is not explicitly written. Is this always ok?
        _db.add(ASSIGN_HEAP_ALLOC, insn, str(instruction.iindex), heap, _rep.local(m, l), methodId, "0");
    }

//    void writeAssignCast(IMethod m, Stmt stmt, Local to, Local from, TypeReference t, Session session) {
//        int index = session.calcUnitNumber(stmt);
//        String insn = _rep.instruction(m, stmt, session, index);
//        String methodId = writeMethod(m);
//
//        _db.add(ASSIGN_CAST, insn, str(index), _rep.local(m, from), _rep.local(m, to), writeType(t), methodId);
//    }
//
//    void writeAssignCastNumericConstant(IMethod m, Stmt stmt, Local to, NumericConstant constant, TypeReference t, Session session) {
//        int index = session.calcUnitNumber(stmt);
//        String insn = _rep.instruction(m, stmt, session, index);
//        String methodId = writeMethod(m);
//
//        _db.add(ASSIGN_CAST_NUM_CONST, insn, str(index), constant.toString(), _rep.local(m, to), writeType(t), methodId);
//    }
//
//    void writeAssignCastNull(IMethod m, Stmt stmt, Local to, TypeReference t, Session session) {
//        int index = session.calcUnitNumber(stmt);
//        String insn = _rep.instruction(m, stmt, session, index);
//        String methodId = writeMethod(m);
//
//        _db.add(ASSIGN_CAST_NULL, insn, str(index), _rep.local(m, to), writeType(t), methodId);
//    }
//
//    void writeStoreInstanceField(IMethod m, Stmt stmt, IField f, Local base, Local from, Session session) {
//        writeInstanceField(m, stmt, f, base, from, session, STORE_INST_FIELD);
//    }
//
//    void writeLoadInstanceField(IMethod m, Stmt stmt, IField f, Local base, Local to, Session session) {
//        writeInstanceField(m, stmt, f, base, to, session, LOAD_INST_FIELD);
//    }
//
//    private void writeInstanceField(IMethod m, Stmt stmt, IField f, Local base, Local var, Session session, PredicateFile storeInstField) {
//        int index = session.calcUnitNumber(stmt);
//        String insn = _rep.instruction(m, stmt, session, index);
//        String methodId = writeMethod(m);
//
//        String fieldId = writeField(f);
//        _db.add(storeInstField, insn, str(index), _rep.local(m, var), _rep.local(m, base), fieldId, methodId);
//    }
//
//    void writeStoreStaticField(IMethod m, Stmt stmt, IField f, Local from, Session session) {
//        writeStaticField(m, stmt, f, from, session, STORE_STATIC_FIELD);
//    }
//
//    void writeLoadStaticField(IMethod m, Stmt stmt, IField f, Local to, Session session) {
//        writeStaticField(m, stmt, f, to, session, LOAD_STATIC_FIELD);
//    }
//
//    private void writeStaticField(IMethod m, Stmt stmt, IField f, Local var, Session session, PredicateFile loadStaticField) {
//        int index = session.calcUnitNumber(stmt);
//        String insn = _rep.instruction(m, stmt, session, index);
//        String methodId = writeMethod(m);
//
//        String fieldId = writeField(f);
//        _db.add(loadStaticField, insn, str(index), _rep.local(m, var), fieldId, methodId);
//    }
//
//    void writeLoadArrayIndex(IMethod m, Stmt stmt, Local base, Local to, Local arrIndex, Session session) {
//        writeFieldOrIndex(m, stmt, base, to, arrIndex, session, LOAD_ARRAY_INDEX);
//    }
//
//    void writeStoreArrayIndex(IMethod m, Stmt stmt, Local base, Local from, Local arrIndex, Session session) {
//        writeFieldOrIndex(m, stmt, base, from, arrIndex, session, STORE_ARRAY_INDEX);
//    }
//
//    private void writeFieldOrIndex(IMethod m, Stmt stmt, Local base, Local var, Local arrIndex, Session session, PredicateFile predicateFile) {
//        int index = session.calcUnitNumber(stmt);
//        String insn = _rep.instruction(m, stmt, session, index);
//        String methodId = writeMethod(m);
//
//        _db.add(predicateFile, insn, str(index), _rep.local(m, var), _rep.local(m, base), methodId);
//
//        if (arrIndex != null)
//            _db.add(ARRAY_INSN_INDEX, insn, _rep.local(m, arrIndex));
//    }

    void writeApplicationClass(IClass application) {
        _db.add(APP_CLASS, writeType(application));
    }

    String writeField(IField f) {
        String fieldId = _rep.signature(f);
        _db.add(FIELD_SIGNATURE, fieldId, writeType(f.getReference().getDeclaringClass()), _rep.simpleName(f), writeType(f.getFieldTypeReference()));
//        if (f.getTag("VisibilityAnnotationTag") != null) {
//            VisibilityAnnotationTag vTag = (VisibilityAnnotationTag) f.getTag("VisibilityAnnotationTag");
//            for (AnnotationTag aTag : vTag.getAnnotations()) {
//                _db.add(FIELD_ANNOTATION, fieldId, soot.coffi.Util.v().jimpleTypeOfFieldDescriptor(aTag.getType()).getEscapedName());
//            }
//        }
        return fieldId;
    }

    void writeFieldModifier(IField f, String modifier) {
        String fieldId = writeField(f);
        _db.add(FIELD_MODIFIER, modifier, fieldId);
    }

    void writeClassModifier(IClass c, String modifier) {
        String type = c.getName().getClassName().toString();
        if (c.isInterface()) {
            _db.add(INTERFACE_TYPE, type);
        }
        else {
            _db.add(CLASS_TYPE, type);
        }
        _db.add(CLASS_MODIFIER, modifier, type);
    }

    void writeMethodModifier(IMethod m, String modifier) {
        String methodId = writeMethod(m);
        _db.add(METHOD_MODIFIER, modifier, methodId);
    }


    void writeReturn(IMethod m, SSAInstruction instruction, Local l) {
        String insn = _rep.instruction(m, instruction);
        String methodId = writeMethod(m);

        _db.add(RETURN, insn, str(instruction.iindex), _rep.local(m, l), methodId);
    }

    void writeReturnVoid(IMethod m, SSAInstruction instruction) {
        String insn = _rep.instruction(m, instruction);
        String methodId = writeMethod(m);

        _db.add(RETURN_VOID, insn, str(instruction.iindex), methodId);
    }

    // The return var of native methods is exceptional, in that it does not
    // correspond to a return instruction.
    void writeNativeReturnVar(IMethod m) {
        String methodId = writeMethod(m);

        //if (!(m.getReturnType() instanceof VoidType)) {
        String  var = _rep.nativeReturnVar(m);
        _db.add(NATIVE_RETURN_VAR, var, methodId);
        _db.add(VAR_TYPE, var, writeType(m.getReturnType()));
        _db.add(VAR_DECLARING_METHOD, var, methodId);
        //}
    }

//    void writeGoto(IMethod m, Stmt stmt, Unit to, Session session) {
//        session.calcUnitNumber(stmt);
//        int index = session.getUnitNumber(stmt);
//        session.calcUnitNumber(to);
//        int indexTo = session.getUnitNumber(to);
//        String insn = _rep.instruction(m, stmt, session, index);
//        String methodId = writeMethod(m);
//
//        _db.add(GOTO, insn, str(index), str(indexTo), methodId);
//    }
//
//    /**
//     * If
//     */
//    void writeIf(IMethod m, Stmt stmt, Unit to, Session session) {
//        // index was already computed earlier
//        int index = session.getUnitNumber(stmt);
//        session.calcUnitNumber(to);
//        int indexTo = session.getUnitNumber(to);
//        String insn = _rep.instruction(m, stmt, session, index);
//        String methodId = writeMethod(m);
//
//        _db.add(IF, insn, str(index), str(indexTo), methodId);
//
//        Value condStmt = ((IfStmt) stmt).getCondition();
//        if (condStmt instanceof ConditionExpr) {
//            ConditionExpr condition = (ConditionExpr) condStmt;
//            if (condition.getOp1() instanceof Local) {
//                Local op1 = (Local) condition.getOp1();
//                _db.add(IF_VAR, insn, _rep.local(m, op1));
//            }
//            if (condition.getOp2() instanceof Local) {
//                Local op2 = (Local) condition.getOp2();
//                _db.add(IF_VAR, insn, _rep.local(m, op2));
//            }
//        }
//    }
//
//    void writeTableSwitch(IMethod inMethod, TableSwitchStmt stmt, Session session) {
//        int stmtIndex = session.getUnitNumber(stmt);
//
//        Value v = writeImmediate(inMethod, stmt, stmt.getKey(), session);
//
//        if(!(v instanceof Local))
//            throw new RuntimeException("Unexpected key for TableSwitch statement " + v + " " + v.getClass());
//
//        Local l = (Local) v;
//        String insn = _rep.instruction(inMethod, stmt, session, stmtIndex);
//        String methodId = writeMethod(inMethod);
//
//        _db.add(TABLE_SWITCH, insn, str(stmtIndex), _rep.local(inMethod, l), methodId);
//
//        for (int tgIndex = stmt.getLowIndex(), i = 0; tgIndex <= stmt.getHighIndex(); tgIndex++, i++) {
//            session.calcUnitNumber(stmt.getTarget(i));
//            int indexTo = session.getUnitNumber(stmt.getTarget(i));
//
//            _db.add(TABLE_SWITCH_TARGET, insn, str(tgIndex), str(indexTo));
//        }
//
//        session.calcUnitNumber(stmt.getDefaultTarget());
//        int defaultIndex = session.getUnitNumber(stmt.getDefaultTarget());
//
//        _db.add(TABLE_SWITCH_DEFAULT, insn, str(defaultIndex));
//    }
//
//    void writeLookupSwitch(IMethod inMethod, LookupSwitchStmt stmt, Session session) {
//        int stmtIndex = session.getUnitNumber(stmt);
//
//        Value v = writeImmediate(inMethod, stmt, stmt.getKey(), session);
//
//        if(!(v instanceof Local))
//            throw new RuntimeException("Unexpected key for TableSwitch statement " + v + " " + v.getClass());
//
//        Local l = (Local) v;
//        String insn = _rep.instruction(inMethod, stmt, session, stmtIndex);
//        String methodId = writeMethod(inMethod);
//
//        _db.add(LOOKUP_SWITCH, insn, str(stmtIndex), _rep.local(inMethod, l), methodId);
//
//        for(int i = 0, end = stmt.getTargetCount(); i < end; i++) {
//            int tgIndex = stmt.getLookupValue(i);
//            session.calcUnitNumber(stmt.getTarget(i));
//            int indexTo = session.getUnitNumber(stmt.getTarget(i));
//
//            _db.add(LOOKUP_SWITCH_TARGET, insn, str(tgIndex), str(indexTo));
//        }
//
//        session.calcUnitNumber(stmt.getDefaultTarget());
//        int defaultIndex = session.getUnitNumber(stmt.getDefaultTarget());
//
//        _db.add(LOOKUP_SWITCH_DEFAULT, insn, str(defaultIndex));
//    }

    void writeUnsupported(IMethod m, SSAInstruction instruction, Session session) {
        String insn = _rep.unsupported(m, instruction, instruction.iindex);
        String methodId = writeMethod(m);

        _db.add(UNSUPPORTED_INSTRUCTION, insn, str(instruction.iindex), methodId);
    }

    /**
     * Throw statement
     */
//    void writeThrow(IMethod m, Stmt stmt, Local l, Session session) {
//        int index = session.calcUnitNumber(stmt);
//        String insn = _rep.throwLocal(m, l, session);
//        String methodId = writeMethod(m);
//
//        //_db.add(THROW, insn, str(index), _rep.local(m, l), methodId);
//    }

    /**
     * Throw null
     */
//    void writeThrowNull(IMethod m, Stmt stmt, Session session) {
//        int index = session.calcUnitNumber(stmt);
//        String insn = _rep.instruction(m, stmt, session, index);
//        String methodId = writeMethod(m);
//
//        _db.add(THROW_NULL, insn, str(index), methodId);
//    }

//    void writeExceptionHandlerPrevious(IMethod m, Trap current, Trap previous, Session session) {
//        _db.add(EXCEPT_HANDLER_PREV, _rep.handler(m, current, session), _rep.handler(m, previous, session));
//    }
//
//    void writeExceptionHandler(IMethod m, Trap handler, Session session) {
//        IClass exc = handler.getException();
//
//        Local caught;
//        {
//            Unit handlerUnit = handler.getHandlerUnit();
//            IdentityStmt stmt = (IdentityStmt) handlerUnit;
//            Value left = stmt.getLeftOp();
//            Value right = stmt.getRightOp();
//
//            if (right instanceof CaughtExceptionRef && left instanceof Local) {
//                caught = (Local) left;
//            }
//            else {
//                throw new RuntimeException("Unexpected start of exception handler: " + handlerUnit);
//            }
//        }
//
//        String insn = _rep.handler(m, handler, session);
//        int handlerIndex = session.getUnitNumber(handler.getHandlerUnit());
//        session.calcUnitNumber(handler.getBeginUnit());
//        int beginIndex = session.getUnitNumber(handler.getBeginUnit());
//        session.calcUnitNumber(handler.getEndUnit());
//        int endIndex = session.getUnitNumber(handler.getEndUnit());
//        _db.add(EXCEPTION_HANDLER, insn, _rep.signature(m), str(handlerIndex), exc.getName().getClassName().toString(), _rep.local(m, caught), str(beginIndex), str(endIndex));
//    }

    void writeThisVar(IMethod m) {
        String methodId = writeMethod(m);
        String thisVar = _rep.thisVar(m);
        _db.add(THIS_VAR, methodId, thisVar);
        _db.add(VAR_TYPE, thisVar, writeType(m.getReference().getDeclaringClass()));
        _db.add(VAR_DECLARING_METHOD, thisVar, methodId);
    }

    void writeMethodDeclaresException(IMethod m, TypeReference exception) {
        _db.add(METHOD_DECL_EXCEPTION, writeType(exception), writeMethod(m));
    }

    void writeFormalParam(IMethod m, int i) {
        String methodId = writeMethod(m);
        String var = _rep.param(m, i);
        _db.add(FORMAL_PARAM, str(i), methodId, var);
        _db.add(VAR_TYPE, var, writeType(m.getParameterType(i)));
        _db.add(VAR_DECLARING_METHOD, var, methodId);
    }

    void writeLocal(IMethod m, Local l) {
        String local = _rep.local(m, l);
        TypeReference type;

        if (_varTypeMap.containsKey(local))
            type = _varTypeMap.get(local);
        else {
            type = l.getType();
            _varTypeMap.put(local, type);
        }

        _db.add(VAR_TYPE, local, writeType(type));
        _db.add(VAR_DECLARING_METHOD, local, writeMethod(m));
    }

    Local writeStringConstantExpression(IMethod inMethod, SSAInstruction instruction, Local l, ConstantValue constant) {
        // introduce a new temporary variable
        writeLocal(inMethod, l);
        writeAssignStringConstant(inMethod, instruction, l, constant);
        return l;
    }

    Local writeNullExpression(IMethod inMethod, SSAInstruction instruction, Local l) {
        // introduce a new temporary variable
        writeLocal(inMethod, l);
        writeAssignNull(inMethod, instruction, l);
        return l;
    }

    Local writeNumConstantExpression(IMethod inMethod, SSAInstruction instruction, Local l, ConstantValue constant) {
        // introduce a new temporary variable
        writeLocal(inMethod, l);
        writeAssignNumConstant(inMethod, instruction, l, constant);
        return l;
    }

    Local writeClassConstantExpression(IMethod inMethod, SSAInstruction instruction, Local l, ConstantValue constant) {
        // introduce a new temporary variable
        writeLocal(inMethod, l);
        writeAssignClassConstant(inMethod, instruction, l, constant);
        return l;
    }

    private Local writeMethodHandleConstantExpression(IMethod inMethod, SSAInstruction instruction, ConstantValue constant, Session session) {
        // introduce a new temporary variable
        String basename = "$mhandleconstant";
        String varname = basename + session.nextNumber(basename);
        Local l = new Local(varname, TypeReference.JavaLangInvokeMethodHandle);
        writeLocal(inMethod, l);
        //writeAssignMethodHandleConstant(inMethod, instruction, l, constant, session);
        return l;
    }

//    private Value writeActualParam(IMethod inMethod, Stmt stmt, InvokeExpr expr, Session session, Value v, int idx) {
////        if (v instanceof StringConstant)
////            //return writeStringConstantExpression(inMethod, stmt, (StringConstant) v, session);
////        else if (v instanceof ClassConstant)
////            return writeClassConstantExpression(inMethod, stmt, (ClassConstant) v, session);
////        else if (v instanceof NumericConstant)
////            return writeNumConstantExpression(inMethod, stmt, (NumericConstant) v, session);
////        else if (v instanceof MethodHandle)
////            return writeMethodHandleConstantExpression(inMethod, stmt, (MethodHandle) v, session);
////        else if (v instanceof NullConstant) {
////            // Giving the type of the formal argument to be used in the creation of
////            // temporary var for the actual argument (whose value is null).
////            Type argType = expr.getMethodRef().parameterType(idx);
////            return writeNullExpression(inMethod, stmt, argType, session);
////        }
////        else if (v instanceof Constant)
////            throw new RuntimeException("Value has unknown constant type: " + v);
//        return v;
//    }

//    private void writeActualParams(IMethod inMethod, Stmt stmt, InvokeExpr expr, String invokeExprRepr, Session session) {
//        for(int i = 0; i < expr.getArgCount(); i++) {
//            Value v = writeActualParam(inMethod, stmt, expr, session, expr.getArg(i), i);
//
//            if (v instanceof Local) {
//                Local l = (Local) v;
//                //_db.add(ACTUAL_PARAMETER, str(i), invokeExprRepr, _rep.local(inMethod, l));
//            }
//            else {
//                throw new RuntimeException("Actual parameter is not a local: " + v + " " + v.getClass());
//            }
//        }
//        if (expr instanceof DynamicInvokeExpr) {
//            DynamicInvokeExpr di = (DynamicInvokeExpr)expr;
//            for (int j = 0; j < di.getBootstrapArgCount(); j++) {
//                Value v = di.getBootstrapArg(j);
//                if (v instanceof Constant) {
//                    Value vConst = writeActualParam(inMethod, stmt, expr, session, (Value)v, j);
//                    if (vConst instanceof Local) {
//                        Local l = (Local) vConst;
//                        //_db.add(BOOTSTRAP_PARAMETER, str(j), invokeExprRepr, _rep.local(inMethod, l));
//                    }
//                    else {
//                        throw new RuntimeException("Unknown actual parameter: " + v + " of type " + v.getClass().getName());
//                    }
//                }
//                else {
//                    throw new RuntimeException("Found non-constant argument to bootstrap method: " + di);
//                }
//            }
//        }
//    }

//    void writeInvoke(IMethod inMethod, Stmt stmt, InvokeExpr expr, Session session) {
//        writeInvokeHelper(inMethod, stmt, expr, session);
//    }
//
//    private String writeInvokeHelper(IMethod inMethod, Stmt stmt, InvokeExpr expr, Session session) {
//        int index = session.calcUnitNumber(stmt);
//        String insn = _rep.invoke(inMethod, expr, session);
//        String methodId = writeMethod(inMethod);
//
//        writeActualParams(inMethod, stmt, expr, insn, session);
//
//        LineNumberTag tag = (LineNumberTag) stmt.getTag("LineNumberTag");
//        if (tag != null) {
//            _db.add(METHOD_INV_LINE, insn, str(tag.getLineNumber()));
//        }

//        if (expr instanceof StaticInvokeExpr) {
//            _db.add(STATIC_METHOD_INV, insn, str(index), _rep.signature(expr.getMethod()), methodId);
//        }
//        else if (expr instanceof VirtualInvokeExpr || expr instanceof InterfaceInvokeExpr) {
//            _db.add(VIRTUAL_METHOD_INV, insn, str(index), _rep.signature(expr.getMethod()), _rep.local(inMethod, (Local) ((InstanceInvokeExpr) expr).getBase()), methodId);
//        }
//        else if (expr instanceof SpecialInvokeExpr) {
//            _db.add(SPECIAL_METHOD_INV, insn, str(index), _rep.signature(expr.getMethod()), _rep.local(inMethod, (Local) ((InstanceInvokeExpr) expr).getBase()), methodId);
//        }
//        else if (expr instanceof DynamicInvokeExpr) {
//            DynamicInvokeExpr di = (DynamicInvokeExpr)expr;
//            SootMethodRef dynInfo = di.getMethodRef();
//            String dynArity = String.valueOf(dynInfo.parameterTypes().size());
//            _db.add(DYNAMIC_METHOD_INV, insn, str(index), _rep.signature(di.getBootstrapMethodRef().resolve()), dynInfo.name(), dynInfo.returnType().toString(), dynArity, dynInfo.parameterTypes().toString(), methodId);
//        }
//        else {
//            throw new RuntimeException("Cannot handle invoke expr: " + expr);
//        }
//
//        return insn;
//    }

    //    private Value writeImmediate(IMethod inMethod, Stmt stmt, Value v, Session session) {
////        if (v instanceof StringConstant)
////            v = writeStringConstantExpression(inMethod, stmt, (StringConstant) v, session);
////        else if (v instanceof ClassConstant)
////            v = writeClassConstantExpression(inMethod, stmt, (ClassConstant) v, session);
////        else if (v instanceof NumericConstant)
////            v = writeNumConstantExpression(inMethod, stmt, (NumericConstant) v, session);
//
//        return v;
//    }
//
    void writeAssignBinop(IMethod m, SSABinaryOpInstruction instruction, Local left, Local op1, Local op2) {
        String insn = _rep.instruction(m, instruction);
        String methodId = writeMethod(m);

        _db.add(ASSIGN_BINOP, insn, str(instruction.iindex), _rep.local(m, left), methodId);
        _db.add(ASSIGN_OPER_TYPE, insn, instruction.getOperator().toString());

        _db.add(ASSIGN_OPER_FROM, insn, _rep.local(m, op1));
        _db.add(ASSIGN_OPER_FROM, insn, _rep.local(m, op2));

    }
//
//    void writeAssignUnop(IMethod m, AssignStmt stmt, Local left, UnopExpr right, Session session) {
////        int index = session.calcUnitNumber(stmt);
////        String insn = _rep.instruction(m, stmt, session, index);
////        String methodId = writeMethod(m);
////
////        _db.add(ASSIGN_UNOP, insn, str(index), _rep.local(m, left), methodId);
////
////        if (right instanceof LengthExpr) {
////            _db.add(ASSIGN_OPER_TYPE, insn, " length ");
////        }
////        else if (right instanceof NegExpr) {
////            _db.add(ASSIGN_OPER_TYPE, insn, " !");
////        }
////
////        if (right.getOp() instanceof Local) {
////            Local op = (Local) right.getOp();
////            _db.add(ASSIGN_OPER_FROM, insn, _rep.local(m, op));
////        }
//    }
//
//    void writeAssignInstanceOf(IMethod m, AssignStmt stmt, Local to, Local from, TypeReference t, Session session) {
////        int index = session.calcUnitNumber(stmt);
////        String insn = _rep.instruction(m, stmt, session, index);
////        String methodId = writeMethod(m);
////
////        _db.add(ASSIGN_INSTANCE_OF, insn, str(index), _rep.local(m, from), _rep.local(m, to), writeType(t), methodId);
//    }
//
//    void writeAssignPhantomInvoke(IMethod m, Stmt stmt, Session session) {
////        int index = session.calcUnitNumber(stmt);
////        String insn = _rep.instruction(m, stmt, session, index);
////        String methodId = writeMethod(m);
////
////        _db.add(ASSIGN_PHANTOM_INVOKE, insn, str(index), methodId);
//    }
//
//    void writePhantomInvoke(IMethod m, Stmt stmt, Session session) {
////        int index = session.calcUnitNumber(stmt);
////        String insn = _rep.instruction(m, stmt, session, index);
////        String methodId = writeMethod(m);
////
////        _db.add(PHANTOM_INVOKE, insn, str(index), methodId);
//    }
//
//    void writeBreakpointStmt(IMethod m, Stmt stmt, Session session) {
////        int index = session.calcUnitNumber(stmt);
////        String insn = _rep.instruction(m, stmt, session, index);
////        String methodId = writeMethod(m);
////
////        _db.add(BREAKPOINT_STMT, insn, str(index), methodId);
//    }

    public void writeApplication(String applicationName) { _db.add(ANDROID_APPLICATION, applicationName); }

    public void writeActivity(String activity) {
        _db.add(ACTIVITY, activity);
    }

    public void writeService(String service) {
        _db.add(SERVICE, service);
    }

    public void writeContentProvider(String contentProvider) {
        _db.add(CONTENT_PROVIDER, contentProvider);
    }

    public void writeBroadcastReceiver(String broadcastReceiver) {
        _db.add(BROADCAST_RECEIVER, broadcastReceiver);
    }

    public void writeCallbackMethod(String callbackMethod) {
        _db.add(CALLBACK_METHOD, callbackMethod);
    }

    public void writeLayoutControl(Integer id, String layoutControl, Integer parentID) {
        _db.add(LAYOUT_CONTROL, id.toString(), layoutControl, parentID.toString());
    }

    public void writeSensitiveLayoutControl(Integer id, String layoutControl, Integer parentID) {
        _db.add(SENSITIVE_LAYOUT_CONTROL, id.toString(), layoutControl, parentID.toString());
    }

    void writeFieldInitialValue(IField f) {
        String fieldId = _rep.signature(f);
//        String valueString = f.getInitialValueString();
//        if (valueString != null && !valueString.equals("")) {
//            int pos = valueString.indexOf('@');
//            if (pos < 0)
//                System.err.println("Unexpected format (no @) in initial field value");
//            else {
//                try {
//                    int value = (int) Long.parseLong(valueString.substring(pos+1), 16); // parse hex string, possibly negative int
//                    _db.add(FIELD_INITIAL_VALUE, fieldId, Integer.toString(value));
//                } catch (NumberFormatException e) {
//                    _db.add(FIELD_INITIAL_VALUE, fieldId, valueString.substring(pos+1));
//                    // if we failed to parse the value as a hex int, output it in full
//                }
//            }
//        }
    }
}
