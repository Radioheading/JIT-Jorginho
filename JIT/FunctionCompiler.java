package Jorginho.JIT;

import ASM.Compound.ASMBlock;
import ASM.Compound.ASMProgram;
import ASM.Compound.ASMFunction;
import ASM.GlobalValue;
import ASM.Instruction.LaInst;
import ASM.Instruction.LoadInst;
import ASM.Operand.Imm;
import Backend.GraphColoring;
import Backend.InstSelector;
import IROptimize.AllocElimination;
import IROptimize.Utils.CFG;
import IROptimize.Utils.CallGraphContruct;
import llvmIR.Entity.IRGlobalVar;
import llvmIR.Function;
import llvmIR.Program;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class FunctionCompiler {
    Program myProgram;
    CallGraphContruct callGraphContruct;
    AllocElimination allocElimination;
    InstSelector instSelector = new InstSelector(new ASMProgram());
    GraphColoring graphColoring = new GraphColoring(new ASMProgram());

    HashSet<String> doneFuncs = new HashSet<>();

    HashMap<String, ASMFunction> compiledFuncs = new HashMap<>();
    HashMap<String, String> compiledFuncsString = new HashMap<>();

    String functionString;

    public FunctionCompiler(Program myProgram, AllocElimination allocElimination) {
        this.myProgram = myProgram;
        this.callGraphContruct = new CallGraphContruct(myProgram);
        new CFG(myProgram).buildCFG();
        callGraphContruct.work();
        this.allocElimination = allocElimination;

        instSelector.init(myProgram);
    }

    ASMFunction getCompiledFunction(String funcName) {
        instSelector.myProgram.functions.clear();
        ASMFunction function = instSelector.compileFunction(myProgram.funcMap.get(funcName));
        graphColoring.allocate_func(function);
        function.is_main = true;
        return function;
    }

    boolean checkAllCompiled(Function function) {
        for (var callee : function.callees) {
            if (!compiledFuncs.containsKey(callee.name) && !callee.name.equals(function.name)) {
                return false;
            }
        }
        return true;
    }

    public String compileFunction(Function function) {
        String name = function.name;

        if (!checkAllCompiled(function)) {
            throw new RuntimeException("Not all callees are compiled");
        }

        ASMFunction asmFunction = getCompiledFunction(name);
        compiledFuncs.put(name, asmFunction);

        ASMProgram program = instSelector.myProgram;

        for (var depend : function.callees) { // link all dependent callees
            if (depend.name.equals(name)) {
                continue;
            }

            ASMFunction dependFunc = compiledFuncs.get(depend.name);
            program.functions.add(dependFunc);
        }

        // load all dirty global variables back to registers
        ASMBlock exitBlock = asmFunction.exitBlock;

        for (int i = 0; i < instSelector.dirtyGlobal.get(name).size(); i++) {
            IRGlobalVar global = instSelector.dirtyGlobal.get(name).get(i);
            exitBlock.insert_front(new LoadInst(ASMProgram.registerMap.get("a" + i + 1), ASMProgram.registerMap.get("t0"), new Imm(0), global.type.size));
            exitBlock.insert_front(new LaInst(ASMProgram.registerMap.get("t0"), global.name));
        }

        asmFunction.is_main = false;
        compiledFuncsString.put(name, program.toString());

        return program.toString();
    }

    public void updateGlobalValue(String name, int value, int size) {
        instSelector.myProgram.globalVars.add(new GlobalValue(name, value, size));
    }

    public ASMFunction getFunction(String name) {
        return compiledFuncs.get(name);
    }

    public ArrayList<IRGlobalVar> getDirtyGlobal(String name) {
        return instSelector.dirtyGlobal.get(name);
    }

    public String getCompiledFunctionString(String name) {
        return compiledFuncsString.get(name);
    }
}