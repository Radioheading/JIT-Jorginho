package Jorginho.JIT;

import llvmIR.Function;
import llvmIR.Program;

import java.util.HashMap;

public class Profiler {
    private final static int ClientModeThreshold = 150;
    private final static int ServerModeThreshold = 200;

    FunctionCompiler functionCompiler;
    IROptimizer irOptimizer;

    boolean serverMode = false;

    HashMap<Function, Integer> functionCounter = new HashMap<>();

    public Profiler(Program myProgram, FunctionCompiler functionCompiler, IROptimizer irOptimizer, boolean serverMode) {
        this.functionCompiler = functionCompiler;
        this.irOptimizer = irOptimizer;
        this.serverMode = serverMode;

        for (var function : myProgram.functions) {
            if (functionCompiler.canCompile(function.name)) {
                functionCounter.put(function, 0);
            }
        }
    }

    public void updateCounter(Function function) {
        if (functionCounter.containsKey(function)) {
            int counter = functionCounter.get(function) + 1;
            functionCounter.put(function, functionCounter.get(function) + 1);

            Function hottest = getHottest();
            if (hottest != null) {
                optimizeFunction(function, counter);
            }
        }
    }

    private Function getHottest() {
        Function hottest = null;
        int max = serverMode ? ServerModeThreshold : ClientModeThreshold;
        for (var entry : functionCounter.entrySet()) {
            if (entry.getValue() > max) {
                max = entry.getValue();
                hottest = entry.getKey();
            }
        }

        return hottest;
    }

    private void optimizeFunction(Function function, int counter) {
        if (serverMode) {
            if (counter >= ServerModeThreshold) {
                if (irOptimizer.updateLevel(function)) {
                    // can still optimize, update counter
                    functionCounter.put(function, counter >> 1); // apply half decay
                } else {
                    // have already run all optimizations, compile!
                    functionCompiler.compileFunction(function);
                    functionCounter.remove(function);
                }
            }
        } else {
            if (counter >= ClientModeThreshold) {
                if (irOptimizer.updateLevel(function)) {
                    // can still optimize, update counter
                    functionCounter.put(function, counter >> 1); // apply half decay
                } else {
                    // have already run all optimizations, compile!
                    functionCompiler.compileFunction(function);
                    functionCounter.remove(function);
                }
            }
        }
    }
}