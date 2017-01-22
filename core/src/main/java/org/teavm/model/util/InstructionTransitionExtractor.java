/*
 *  Copyright 2013 Alexey Andreev.
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
package org.teavm.model.util;

import java.util.List;
import org.teavm.model.BasicBlock;
import org.teavm.model.InvokeDynamicInstruction;
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
import org.teavm.model.instructions.SwitchTableEntry;
import org.teavm.model.instructions.UnwrapArrayInstruction;

/**
 *
 * @author Alexey Andreev
 */
public class InstructionTransitionExtractor implements InstructionVisitor {
    private BasicBlock[] targets;

    public BasicBlock[] getTargets() {
        return targets;
    }

    @Override
    public void visit(EmptyInstruction insn) {
        targets = null;
    }

    @Override
    public void visit(ClassConstantInstruction insn) {
        targets = null;
    }

    @Override
    public void visit(NullConstantInstruction insn) {
        targets = null;
    }

    @Override
    public void visit(IntegerConstantInstruction insn) {
        targets = null;
    }

    @Override
    public void visit(LongConstantInstruction insn) {
        targets = null;
    }

    @Override
    public void visit(FloatConstantInstruction insn) {
        targets = null;
    }

    @Override
    public void visit(DoubleConstantInstruction insn) {
        targets = null;
    }

    @Override
    public void visit(StringConstantInstruction insn) {
        targets = null;
    }

    @Override
    public void visit(BinaryInstruction insn) {
        targets = null;
    }

    @Override
    public void visit(NegateInstruction insn) {
        targets = null;
    }

    @Override
    public void visit(AssignInstruction insn) {
        targets = null;
    }

    @Override
    public void visit(BranchingInstruction insn) {
        targets = new BasicBlock[] { insn.getConsequent(), insn.getAlternative() };
    }

    @Override
    public void visit(BinaryBranchingInstruction insn) {
        targets = new BasicBlock[] { insn.getConsequent(), insn.getAlternative() };
    }

    @Override
    public void visit(JumpInstruction insn) {
        targets = new BasicBlock[] { insn.getTarget() };
    }

    @Override
    public void visit(SwitchInstruction insn) {
        List<SwitchTableEntry> entries = insn.getEntries();
        targets = new BasicBlock[entries.size() + 1];
        for (int i = 0; i < entries.size(); ++i) {
            targets[i] = entries.get(i).getTarget();
        }
        targets[entries.size()] = insn.getDefaultTarget();
    }

    @Override
    public void visit(ExitInstruction insn) {
        targets = new BasicBlock[0];
    }

    @Override
    public void visit(RaiseInstruction insn) {
        targets = new BasicBlock[0];
    }

    @Override
    public void visit(ConstructArrayInstruction insn) {
        targets = null;
    }

    @Override
    public void visit(ConstructInstruction insn) {
        targets = null;
    }

    @Override
    public void visit(ConstructMultiArrayInstruction insn) {
        targets = null;
    }

    @Override
    public void visit(GetFieldInstruction insn) {
        targets = null;
    }

    @Override
    public void visit(PutFieldInstruction insn) {
        targets = null;
    }

    @Override
    public void visit(GetElementInstruction insn) {
        targets = null;
    }

    @Override
    public void visit(PutElementInstruction insn) {
        targets = null;
    }

    @Override
    public void visit(InvokeInstruction insn) {
        targets = null;
    }

    @Override
    public void visit(InvokeDynamicInstruction insn) {
        targets = null;
    }

    @Override
    public void visit(IsInstanceInstruction insn) {
        targets = null;
    }

    @Override
    public void visit(CastInstruction insn) {
        targets = null;
    }

    @Override
    public void visit(CastNumberInstruction insn) {
        targets = null;
    }

    @Override
    public void visit(CastIntegerInstruction insn) {
        targets = null;
    }

    @Override
    public void visit(ArrayLengthInstruction insn) {
        targets = null;
    }

    @Override
    public void visit(UnwrapArrayInstruction insn) {
        targets = null;
    }

    @Override
    public void visit(CloneArrayInstruction insn) {
        targets = null;
    }

    @Override
    public void visit(InitClassInstruction insn) {
        targets = null;
    }

    @Override
    public void visit(NullCheckInstruction insn) {
        targets = null;
    }

    @Override
    public void visit(MonitorEnterInstruction insn) {
        targets = null;
    }

    @Override
    public void visit(MonitorExitInstruction insn) {
        targets = null;
    }
}
