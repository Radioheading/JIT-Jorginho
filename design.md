# Design of JIT

#### Due to features of Ravel

- We can only compile functions with such properties
  - all of its callees have been compiled, as we can't make context-switch in ravel
  - there's no `malloc`, `getString` or `getInt`, as separating the input file costs lots of time
- we use `<jni.h>` to enable c++ method calling in Java context
  
#### CodeGenerator

We use `CodeGenerator` to compile a given function before we actually call ravel to execute it. The full process contains the following steps:

1. analyze its dependencies and used global variables
2. ask VirtualMachine to overwrite the values of global variables