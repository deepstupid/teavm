/*
 *  Copyright 2016 Alexey Andreev.
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
package org.teavm.backend.javascript;

import org.teavm.diagnostics.Diagnostics;
import org.teavm.model.BasicBlock;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.Instruction;
import org.teavm.model.MethodHolder;
import org.teavm.model.Program;
import org.teavm.model.Variable;
import org.teavm.model.instructions.InvocationType;
import org.teavm.model.instructions.InvokeInstruction;
import org.teavm.model.instructions.NullCheckInstruction;

public class NullPointerExceptionTransformer implements ClassHolderTransformer {
    @Override
    public void transformClass(ClassHolder cls, ClassReaderSource innerSource, Diagnostics diagnostics) {
        for (MethodHolder method : cls.getMethods()) {
            Program program = method.getProgram();
            if (program == null) {
                continue;
            }
            for (int i = 0; i < program.basicBlockCount(); ++i) {
                BasicBlock block = program.basicBlockAt(i);
                transformBlock(block);
            }
        }
    }

    private void transformBlock(BasicBlock block) {
        for (Instruction insn : block) {
            if (insn instanceof InvokeInstruction) {
                InvokeInstruction invoke = (InvokeInstruction) insn;
                if (invoke.getType() != InvocationType.VIRTUAL) {
                    continue;
                }
                NullCheckInstruction nullCheck = new NullCheckInstruction();
                nullCheck.setValue(invoke.getInstance());
                Variable var = block.getProgram().createVariable();
                nullCheck.setReceiver(var);
                invoke.setInstance(var);
                insn.insertPrevious(nullCheck);
            }
        }
    }
}
