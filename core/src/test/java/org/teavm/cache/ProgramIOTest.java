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

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.teavm.model.BasicBlock;
import org.teavm.model.Instruction;
import org.teavm.model.Program;
import org.teavm.model.ValueType;
import org.teavm.model.instructions.BinaryInstruction;
import org.teavm.model.instructions.BinaryOperation;
import org.teavm.model.instructions.ClassConstantInstruction;
import org.teavm.model.instructions.DoubleConstantInstruction;
import org.teavm.model.instructions.EmptyInstruction;
import org.teavm.model.instructions.FloatConstantInstruction;
import org.teavm.model.instructions.IntegerConstantInstruction;
import org.teavm.model.instructions.LongConstantInstruction;
import org.teavm.model.instructions.NullConstantInstruction;
import org.teavm.model.instructions.NumericOperandType;
import org.teavm.model.instructions.StringConstantInstruction;

public class ProgramIOTest {
    @Test
    public void emptyInstruction() {
        Program program = new Program();
        BasicBlock block = program.createBasicBlock();
        block.add(new EmptyInstruction());

        program = inputOutput(program);
        block = program.basicBlockAt(0);

        assertThat(block.instructionCount(), is(1));
        assertThat(block.getFirstInstruction(), instanceOf(EmptyInstruction.class));
    }

    @Test
    public void constants() {
        Program program = new Program();
        BasicBlock block = program.createBasicBlock();
        ClassConstantInstruction classConstInsn = new ClassConstantInstruction();
        classConstInsn.setConstant(ValueType.BYTE);
        classConstInsn.setReceiver(program.createVariable());
        block.add(classConstInsn);
        NullConstantInstruction nullConstInsn = new NullConstantInstruction();
        nullConstInsn.setReceiver(program.createVariable());
        block.add(nullConstInsn);
        IntegerConstantInstruction intConsInsn = new IntegerConstantInstruction();
        intConsInsn.setReceiver(program.createVariable());
        intConsInsn.setConstant(23);
        block.add(intConsInsn);
        LongConstantInstruction longConsInsn = new LongConstantInstruction();
        longConsInsn.setReceiver(program.createVariable());
        longConsInsn.setConstant(234);
        block.add(longConsInsn);
        FloatConstantInstruction floatConsInsn = new FloatConstantInstruction();
        floatConsInsn.setReceiver(program.createVariable());
        floatConsInsn.setConstant(3.14f);
        block.add(floatConsInsn);
        DoubleConstantInstruction doubleConsInsn = new DoubleConstantInstruction();
        doubleConsInsn.setReceiver(program.createVariable());
        doubleConsInsn.setConstant(3.14159);
        block.add(doubleConsInsn);
        StringConstantInstruction stringConsInsn = new StringConstantInstruction();
        stringConsInsn.setReceiver(program.createVariable());
        stringConsInsn.setConstant("foo");
        block.add(stringConsInsn);

        program = inputOutput(program);
        block = program.basicBlockAt(0);

        assertThat(block.instructionCount(), is(7));
        Instruction insn = block.getFirstInstruction();
        assertThat(insn, instanceOf(ClassConstantInstruction.class));
        insn = insn.getNext();
        assertThat(insn, instanceOf(NullConstantInstruction.class));
        insn = insn.getNext();
        assertThat(insn, instanceOf(IntegerConstantInstruction.class));
        insn = insn.getNext();
        assertThat(insn, instanceOf(LongConstantInstruction.class));
        insn = insn.getNext();
        assertThat(insn, instanceOf(FloatConstantInstruction.class));
        insn = insn.getNext();
        assertThat(insn, instanceOf(DoubleConstantInstruction.class));
        insn = insn.getNext();
        assertThat(insn, instanceOf(StringConstantInstruction.class));

        insn = block.getFirstInstruction();
        classConstInsn = (ClassConstantInstruction) insn;
        assertThat(classConstInsn.getReceiver().getIndex(), is(0));
        assertThat(classConstInsn.getConstant().toString(), is(ValueType.BYTE.toString()));

        insn = insn.getNext();
        nullConstInsn = (NullConstantInstruction) insn;
        assertThat(nullConstInsn.getReceiver().getIndex(), is(1));

        insn = insn.getNext();
        intConsInsn = (IntegerConstantInstruction) insn;
        assertThat(intConsInsn.getConstant(), is(23));
        assertThat(intConsInsn.getReceiver().getIndex(), is(2));

        insn = insn.getNext();
        longConsInsn = (LongConstantInstruction) insn;
        assertThat(longConsInsn.getConstant(), is(234L));
        assertThat(longConsInsn.getReceiver().getIndex(), is(3));

        insn = insn.getNext();
        floatConsInsn = (FloatConstantInstruction) insn;
        assertThat(floatConsInsn.getConstant(), is(3.14f));
        assertThat(floatConsInsn.getReceiver().getIndex(), is(4));

        insn = insn.getNext();
        doubleConsInsn = (DoubleConstantInstruction) insn;
        assertThat(doubleConsInsn.getConstant(), is(3.14159));
        assertThat(doubleConsInsn.getReceiver().getIndex(), is(5));

        insn = insn.getNext();
        stringConsInsn = (StringConstantInstruction) insn;
        assertThat(stringConsInsn.getConstant(), is("foo"));
        assertThat(stringConsInsn.getReceiver().getIndex(), is(6));
    }

    @Test
    public void binaryOperation() {
        Program program = new Program();
        BasicBlock block = program.createBasicBlock();
        IntegerConstantInstruction constInsn = new IntegerConstantInstruction();
        constInsn.setConstant(3);
        constInsn.setReceiver(program.createVariable());
        block.add(constInsn);
        constInsn = new IntegerConstantInstruction();
        constInsn.setConstant(2);
        constInsn.setReceiver(program.createVariable());
        block.add(constInsn);
        BinaryInstruction addInsn = new BinaryInstruction(BinaryOperation.ADD, NumericOperandType.INT);
        addInsn.setFirstOperand(program.variableAt(0));
        addInsn.setSecondOperand(program.variableAt(1));
        addInsn.setReceiver(program.createVariable());
        block.add(addInsn);
        BinaryInstruction subInsn = new BinaryInstruction(BinaryOperation.SUBTRACT, NumericOperandType.INT);
        subInsn.setFirstOperand(program.variableAt(2));
        subInsn.setSecondOperand(program.variableAt(0));
        subInsn.setReceiver(program.createVariable());
        block.add(subInsn);

        assertThat(block.instructionCount(), is(4));
    }

    private Program inputOutput(Program program) {
        InMemorySymbolTable symbolTable = new InMemorySymbolTable();
        InMemorySymbolTable fileTable = new InMemorySymbolTable();
        ProgramIO programIO = new ProgramIO(symbolTable, fileTable);
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            programIO.write(program, output);
            try (ByteArrayInputStream input = new ByteArrayInputStream(output.toByteArray())) {
                return programIO.read(input);
            }
        } catch (IOException e) {
            throw new AssertionError("This exception should not be thrown", e);
        }
    }

    private static class InMemorySymbolTable implements SymbolTable {
        private List<String> symbols = new ArrayList<>();
        private Map<String, Integer> indexes = new HashMap<>();

        @Override
        public String at(int index) {
            return symbols.get(index);
        }

        @Override
        public int lookup(String symbol) {
            Integer index = indexes.get(symbol);
            if (index == null) {
                index = symbols.size();
                symbols.add(symbol);
                indexes.put(symbol, index);
            }
            return index;
        }
    }
}
