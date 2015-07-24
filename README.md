# Synopsis #

For more than the last two decades, multi-agent simulations have been highlighted to model mega-scale social or biological agents and to simulate their emergent collective behavior that may be difficult only with mathematical and macroscopic approaches. A successful key for simulating megascale agents is to speed up the execution with parallelization. Although many parallelization attempts have been made to multiagent simulations, most work has been done on shared-memory programming environments such as OpenMP, CUDA, and Global Array, or still has left several programming problems specific to distributed-memory systems, such as machine unawareness, ghost space management, and cross-processor agent management (including migration, propagation, and termination). To address these parallelization challenges, we have been developing MASS, a new parallel-computing library for multi-agent and spatial simulation over a cluster of computing nodes. MASS composes a user application of distributed arrays and multi-agents, each representing an individual simulation place or an active entity. All computation is enclosed in each array element or agent; all communication is scheduled as periodic data exchanges among those entities, using machine-independent identifiers; and agents migrate to a remote array element for rendezvousing with each other. Our unique agent-based approach takes advantage of these merits for parallelizing big data analysis using climate change and biological network motif searches as well as individual-based simulation such as neural network simulation and influenza epidemic simulation as practical application examples.

### What is this repository for? ###

* Quick summary
* Version
* [Learn Markdown](https://bitbucket.org/tutorials/markdowndemo)

### How do I get set up? ###

* Summary of set up
* Configuration
* Dependencies
* Database configuration
* How to run tests
* Deployment instructions

### Contribution guidelines ###

* Writing tests
* Code review
* Other guidelines

### Who do I talk to? ###

* Repo owner or admin
* Other community or team contact