/*
 *  Copyright 2014 Alexey Andreev.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.teavm.cache;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.teavm.model.BasicBlock;
import org.teavm.model.Instruction;
import org.teavm.model.InvokeDynamicInstruction;
import org.teavm.model.MethodHandle;
import org.teavm.model.MethodReference;
import org.teavm.model.Program;
import org.teavm.model.ProgramCache;
import org.teavm.model.RuntimeConstant;
import org.teavm.model.instructions.ArrayLengthInstruction;
import org.teavm.model.instructions.AssignInstruction;
import org.teavm.model.instructions.BinaryBranchingInstruction;
import org.teavm.model.instructions.BinaryInstruction;
import org.teavm.model.instructions.BranchingInstruction;
import org.teavm.model.instructions.CastInstruction;
import org.teavm.model.instructions.CastIntegerInstruction;
import org.teavm.model.instructions.CastNumberInstruction;
import org.teavm.model.instructions.ClassConstantInstruction;
import org.teavm.model.instructions.CloneArrayInstruction;
import org.teavm.model.instructions.ConstructArrayInstruction;
import org.teavm.model.instructions.ConstructInstruction;
import org.teavm.model.instructions.ConstructMultiArrayInstruction;
import org.teavm.model.instructions.DoubleConstantInstruction;
import org.teavm.model.instructions.EmptyInstruction;
import org.teavm.model.instructions.ExitInstruction;
import org.teavm.model.instructions.FloatConstantInstruction;
import org.teavm.model.instructions.GetElementInstruction;
import org.teavm.model.instructions.GetFieldInstruction;
import org.teavm.model.instructions.InitClassInstruction;
import org.teavm.model.instructions.InstructionVisitor;
import org.teavm.model.instructions.IntegerConstantInstruction;
import org.teavm.model.instructions.InvokeInstruction;
import org.teavm.model.instructions.IsInstanceInstruction;
import org.teavm.model.instructions.JumpInstruction;
import org.teavm.model.instructions.LongConstantInstruction;
import org.teavm.model.instructions.MonitorEnterInstruction;
import org.teavm.model.instructions.MonitorExitInstruction;
import org.teavm.model.instructions.NegateInstruction;
import org.teavm.model.instructions.NullCheckInstruction;
import org.teavm.model.instructions.NullConstantInstruction;
import org.teavm.model.instructions.PutElementInstruction;
import org.teavm.model.instructions.PutFieldInstruction;
import org.teavm.model.instructions.RaiseInstruction;
import org.teavm.model.instructions.StringConstantInstruction;
import org.teavm.model.instructions.SwitchInstruction;
import org.teavm.model.instructions.UnwrapArrayInstruction;
import org.teavm.parsing.ClassDateProvider;

public class DiskProgramCache implements ProgramCache {
    private final File directory;
    private final ProgramIO programIO;
    private final Map<MethodReference, Item> cache = new HashMap<>();
    private final Set<MethodReference> newMethods = new HashSet<>();
    private final ClassDateProvider classDateProvider;

    public DiskProgramCache(File directory, SymbolTable symbolTable, SymbolTable fileTable,
            ClassDateProvider classDateProvider) {
        this.directory = directory;
        programIO = new ProgramIO(symbolTable, fileTable);
        this.classDateProvider = classDateProvider;
    }

    @Override
    public Program get(MethodReference method) {
        Item item = cache.get(method);
        if (item == null) {
            item = new Item();
            cache.put(method, item);
            File file = getMethodFile(method);
            if (file.exists()) {
                try (InputStream stream = new BufferedInputStream(new FileInputStream(file))) {
                    DataInput input = new DataInputStream(stream);
                    int depCount = input.readShort();
                    boolean dependenciesChanged = false;
                    for (int i = 0; i < depCount; ++i) {
                        String depClass = input.readUTF();
                        Date depDate = classDateProvider.getModificationDate(depClass);
                        if (depDate == null || depDate.after(new Date(file.lastModified()))) {
                            dependenciesChanged = true;
                            break;
                        }
                    }
                    if (!dependenciesChanged) {
                        item.program = programIO.read(stream);
                    }
                } catch (IOException e) {
                    // we could not read program, just leave it empty
                }
            }
        }
        return item.program;
    }

    @Override
    public void store(MethodReference method, Program program) {
        Item item = new Item();
        cache.put(method, item);
        item.program = program;
        newMethods.add(method);
    }

    public void flush() throws IOException {
        for (MethodReference method : newMethods) {
            File file = getMethodFile(method);
            ProgramDependencyAnalyzer analyzer = new ProgramDependencyAnalyzer();
            analyzer.dependencies.add(method.getClassName());
            Program program = cache.get(method).program;
            for (int i = 0; i < program.basicBlockCount(); ++i) {
                BasicBlock block = program.basicBlockAt(i);
                for (Instruction insn : block) {
                    insn.acceptVisitor(analyzer);
                }
            }
            file.getParentFile().mkdirs();
            try (OutputStream stream = new BufferedOutputStream(new FileOutputStream(file))) {
                DataOutput output = new DataOutputStream(stream);
                output.writeShort(analyzer.dependencies.size());
                for (String dep : analyzer.dependencies) {
                    output.writeUTF(dep);
                }
                programIO.write(program, stream);
            }
        }
    }

    private File getMethodFile(MethodReference method) {
        File dir = new File(directory, method.getClassName().replace('.', '/'));
        return new File(dir, FileNameEncoder.encodeFileName(method.getDescriptor().toString()) + ".teavm-opt");
    }

    static class Item {
        Program program;
    }

    static class ProgramDependencyAnalyzer implements InstructionVisitor {
        Set<String> dependencies = new HashSet<>();
        @Override public void visit(GetFieldInstruction insn) {
            dependencies.add(insn.getField().className);
        }
        @Override public void visit(PutFieldInstruction insn) {
            dependencies.add(insn.getField().className);
        }
        @Override public void visit(InvokeInstruction insn) {
            dependencies.add(insn.getMethod().getClassName());
        }
        @Override
        public void visit(InvokeDynamicInstruction insn) {
            for (RuntimeConstant cst : insn.getBootstrapArguments()) {
                if (cst.getKind() == RuntimeConstant.METHOD_HANDLE) {
                    MethodHandle handle = cst.getMethodHandle();
                    dependencies.add(handle.getClassName());
                }
            }
        }
        @Override public void visit(EmptyInstruction insn) { }
        @Override public void visit(ClassConstantInstruction insn) { }
        @Override public void visit(NullConstantInstruction insn) { }
        @Override public void visit(IntegerConstantInstruction insn) { }
        @Override public void visit(LongConstantInstruction insn) { }
        @Override public void visit(FloatConstantInstruction insn) { }
        @Override public void visit(DoubleConstantInstruction insn) { }
        @Override public void visit(StringConstantInstruction insn) { }
        @Override public void visit(BinaryInstruction insn) { }
        @Override public void visit(NegateInstruction insn) { }
        @Override public void visit(AssignInstruction insn) { }
        @Override public void visit(CastInstruction insn) { }
        @Override public void visit(CastNumberInstruction insn) { }
        @Override public void visit(CastIntegerInstruction insn) { }
        @Override public void visit(BranchingInstruction insn) { }
        @Override public void visit(BinaryBranchingInstruction insn) { }
        @Override public void visit(JumpInstruction insn) { }
        @Override public void visit(SwitchInstruction insn) { }
        @Override public void visit(ExitInstruction insn) { }
        @Override public void visit(RaiseInstruction insn) { }
        @Override public void visit(ConstructArrayInstruction insn) { }
        @Override public void visit(ConstructInstruction insn) { }
        @Override public void visit(ConstructMultiArrayInstruction insn) { }
        @Override public void visit(ArrayLengthInstruction insn) { }
        @Override public void visit(CloneArrayInstruction insn) { }
        @Override public void visit(UnwrapArrayInstruction insn) { }
        @Override public void visit(GetElementInstruction insn) { }
        @Override public void visit(PutElementInstruction insn) { }
        @Override public void visit(IsInstanceInstruction insn) { }
        @Override public void visit(InitClassInstruction insn) { }
        @Override public void visit(NullCheckInstruction insn) { }
        @Override public void visit(MonitorEnterInstruction insn) { }
        @Override public void visit(MonitorExitInstruction insn) { }
    }
}
