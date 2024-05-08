package Jorginho.JIT;

import llvmIR.Function;
import llvmIR.Program;
import IROptimize.Utils.*;
import IROptimize.*;

import java.io.FileNotFoundException;
import java.util.HashMap;


public class IROptimizer {
    private Program myProgram;
    private final static int INLINE_THRESHOLD = 9;
    boolean serverMode = false;
    ADCE ADCE_optimizer;
    AllocElimination Mem2Reg_optimizer;
    ConstPropagation SCCP_optimizer;
    CSE CSE_optimizer;
    GlobalToLocal globalToLocal;
    IVT IVT_optimizer;
    LoopInvariant LICM_optimizer;
    FuncInliner Inline_optimizer;
    RDE RDE_optimizer;

    HashMap<Function, Integer> level = new HashMap<>();

    public IROptimizer(Program myProgram, boolean serverMode) throws FileNotFoundException {
        this.myProgram = myProgram;
        this.serverMode = serverMode;

        init();
    }

    public boolean updateLevel(Function function) {
        if (level.get(level.get(function)) <= 2) {
            level.put(function, level.get(function) + 1);
            // do some optimizations
            optimizeFunction(function, level.get(function));
            return true;
        } else {
            return false;
        }
    }

    private void optimizeFunction(Function function, int level) {
        if (level == 1) {
            if (!serverMode) {
                Mem2Reg_optimizer.optimize_func(function);
            }
            ADCE_optimizer.work_on_func(function);
            SCCP_optimizer.propagateConst_function(function);
        } else {
            CSE_optimizer.workFunc(function);
            IVT_optimizer.transformFunc(function);
            LICM_optimizer.simplifyFunc(function);
        }
    }


    /*
     * In initialization stage, we do some inlining based on the size of the function
     * Moreover, if we're in the server mode, we will do global optimizations like Mem2Reg
     */

    private void init() throws FileNotFoundException {
        ADCE_optimizer = new ADCE(myProgram);
        Mem2Reg_optimizer = new AllocElimination(myProgram);
        SCCP_optimizer = new ConstPropagation(myProgram);
        CSE_optimizer = new CSE(myProgram);
        globalToLocal = new GlobalToLocal(myProgram);
        IVT_optimizer = new IVT(myProgram);
        LICM_optimizer = new LoopInvariant(myProgram);
        Inline_optimizer = new FuncInliner(myProgram, INLINE_THRESHOLD, INLINE_THRESHOLD, INLINE_THRESHOLD, INLINE_THRESHOLD);
        RDE_optimizer = new RDE(myProgram);

        if (serverMode) {
            globalToLocal.globalTransition();
            Mem2Reg_optimizer.eliminateAlloc();
            Inline_optimizer.work();
        } else {
            globalToLocal.globalTransition();
            // trade off for global2local
        }
    }
}