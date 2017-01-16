/*
 *  Copyright 2017 Alexey Andreev.
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
package org.teavm.model.analysis;

import com.carrotsearch.hppc.IntArrayDeque;
import com.carrotsearch.hppc.IntDeque;
import com.carrotsearch.hppc.IntStack;
import org.teavm.common.DisjointSet;
import org.teavm.common.Graph;
import org.teavm.common.GraphBuilder;
import org.teavm.model.BasicBlock;
import org.teavm.model.Incoming;
import org.teavm.model.Instruction;
import org.teavm.model.MethodReference;
import org.teavm.model.Phi;
import org.teavm.model.Program;
import org.teavm.model.Variable;
import org.teavm.model.instructions.AbstractInstructionVisitor;
import org.teavm.model.instructions.AssignInstruction;
import org.teavm.model.instructions.CastInstruction;
import org.teavm.model.instructions.ConstructInstruction;
import org.teavm.model.instructions.ExitInstruction;
import org.teavm.model.instructions.GetElementInstruction;
import org.teavm.model.instructions.GetFieldInstruction;
import org.teavm.model.instructions.InvokeInstruction;
import org.teavm.model.instructions.IsInstanceInstruction;
import org.teavm.model.instructions.MonitorEnterInstruction;
import org.teavm.model.instructions.MonitorExitInstruction;
import org.teavm.model.instructions.NullCheckInstruction;
import org.teavm.model.instructions.PutElementInstruction;
import org.teavm.model.instructions.PutFieldInstruction;
import org.teavm.model.instructions.RaiseInstruction;

public class EscapeAnalysis {
    private int[] definitionClasses;
    private Graph referenceGraph;
    private boolean[] escapingVars;
    private boolean[] locallyConstructedVars;

    public void analyze(Program program, MethodReference methodReference) {
        buildGraph(program, methodReference.parameterCount() + 1);
        detectLoops();
        propagateEscapeInfo(program);
        referenceGraph = null;
    }

    public boolean escapes(int var) {
        return escapingVars[definitionClasses[var]];
    }

    public boolean isLocallyConstructed(int var) {
        return locallyConstructedVars[var];
    }

    private void buildGraph(Program program, int paramCount) {
        PreparationVisitor visitor = new PreparationVisitor(program.variableCount());
        for (int i = 0; i < paramCount; ++i) {
            visitor.escapingVars[i] = true;
        }

        for (BasicBlock block : program.getBasicBlocks()) {
            for (Phi phi : block.getPhis()) {
                visitor.visit(phi);
            }
            for (Instruction insn : block) {
                insn.acceptVisitor(visitor);
            }
        }

        definitionClasses = visitor.definitionClasses.pack(program.variableCount());
        referenceGraph = visitor.referenceGraph.build();
        locallyConstructedVars = visitor.locallyConstructedVars;

        GraphBuilder reducedGraphBuilder = new GraphBuilder(program.variableCount());
        for (int i = 0; i < program.variableCount(); ++i) {
            for (int j : referenceGraph.outgoingEdges(i)) {
                reducedGraphBuilder.addEdge(definitionClasses[i], definitionClasses[j]);
            }
        }
        referenceGraph = reducedGraphBuilder.build();

        escapingVars = new boolean[program.variableCount()];
        for (int i = 0; i < program.variableCount(); ++i) {
            if (visitor.escapingVars[i]) {
                escapingVars[definitionClasses[i]] = true;
            }
        }
    }

    private void detectLoops() {
        IntStack stack = new IntStack();
        for (int i = 0; i < escapingVars.length; ++i) {
            if (referenceGraph.incomingEdgesCount(0) == 0) {
                stack.push(definitionClasses[i]);
            }
        }

        boolean[] complete = new boolean[referenceGraph.size()];
        boolean[] visiting = new boolean[referenceGraph.size()];

        while (!stack.isEmpty()) {
            int var = stack.pop();
            if (complete[var]) {
                continue;
            }
            if (visiting[var]) {
                visiting[var] = false;
                complete[var] = true;
            } else {
                visiting[var] = true;
                stack.push(var);
                for (int successor : referenceGraph.outgoingEdges(var)) {
                    if (!visiting[successor]) {
                        stack.push(successor);
                    } else {
                        escapingVars[successor] = true;
                    }
                }
            }
        }
    }

    private void propagateEscapeInfo(Program program) {
        IntDeque queue = new IntArrayDeque();
        for (int i = 0; i < escapingVars.length; ++i) {
            if (escapingVars[i]) {
                queue.addLast(i);
            }
        }

        boolean[] visited = new boolean[program.variableCount()];
        while (!queue.isEmpty()) {
            int var = queue.removeFirst();
            if (visited[var]) {
                continue;
            }
            visited[var] = true;
            escapingVars[var] = true;

            for (int successor : referenceGraph.outgoingEdges(var)) {
                if (!visited[successor]) {
                    queue.addLast(successor);
                }
            }
        }
    }

    private static class PreparationVisitor extends AbstractInstructionVisitor {
        DisjointSet definitionClasses;
        GraphBuilder referenceGraph;
        boolean[] escapingVars;
        boolean[] locallyConstructedVars;

        public PreparationVisitor(int variableCount) {
            definitionClasses = new DisjointSet();
            referenceGraph = new GraphBuilder(variableCount);
            for (int i = 0; i < variableCount; ++i) {
                definitionClasses.create();
            }
            escapingVars = new boolean[variableCount];
            locallyConstructedVars = new boolean[variableCount];
        }

        public void visit(Phi insn) {
            for (Incoming incoming : insn.getIncomings()) {
                definitionClasses.union(insn.getReceiver().getIndex(), incoming.getValue().getIndex());
            }
        }

        @Override
        public void visit(AssignInstruction insn) {
            definitionClasses.union(insn.getReceiver().getIndex(), insn.getAssignee().getIndex());
        }

        @Override
        public void visit(CastInstruction insn) {
            definitionClasses.union(insn.getReceiver().getIndex(), insn.getValue().getIndex());
        }

        @Override
        public void visit(ExitInstruction insn) {
            if (insn.getValueToReturn() != null) {
                escapingVars[insn.getValueToReturn().getIndex()] = true;
            }
        }

        @Override
        public void visit(RaiseInstruction insn) {
            escapingVars[insn.getException().getIndex()] = true;
        }

        @Override
        public void visit(ConstructInstruction insn) {
            locallyConstructedVars[insn.getReceiver().getIndex()] = true;
        }

        @Override
        public void visit(GetFieldInstruction insn) {
            registerReference(insn.getInstance(), insn.getReceiver());
        }

        @Override
        public void visit(PutFieldInstruction insn) {
            registerReference(insn.getInstance(), insn.getValue());
        }

        private void registerReference(Variable instance, Variable value) {
            if (instance != null) {
                referenceGraph.addEdge(instance.getIndex(), value.getIndex());
            } else {
                escapingVars[value.getIndex()] = true;
            }
        }

        @Override
        public void visit(GetElementInstruction insn) {
            escapingVars[insn.getReceiver().getIndex()] = true;
        }

        @Override
        public void visit(PutElementInstruction insn) {
            escapingVars[insn.getValue().getIndex()] = true;
        }

        @Override
        public void visit(InvokeInstruction insn) {
            if (insn.getInstance() != null) {
                escapingVars[insn.getInstance().getIndex()] = true;
            }
            for (Variable arg : insn.getArguments()) {
                escapingVars[arg.getIndex()] = true;
            }
            if (insn.getReceiver() != null) {
                escapingVars[insn.getReceiver().getIndex()] = true;
            }
        }

        @Override
        public void visit(IsInstanceInstruction insn) {
            escapingVars[insn.getValue().getIndex()] = true;
        }

        @Override
        public void visit(NullCheckInstruction insn) {
            definitionClasses.union(insn.getValue().getIndex(), insn.getReceiver().getIndex());
        }

        @Override
        public void visit(MonitorEnterInstruction insn) {
            escapingVars[insn.getObjectRef().getIndex()] = true;
        }

        @Override
        public void visit(MonitorExitInstruction insn) {
            escapingVars[insn.getObjectRef().getIndex()] = true;
        }
    }
}
