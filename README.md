![BrandHeader-STEM-BlackPrint.png](https://bitbucket.org/repo/b9GKML/images/1347427446-BrandHeader-STEM-BlackPrint.png)
# What is MASS?

(Multi-Agent Spatial Simulation)

For more than the last two decades, multi-agent simulations have been highlighted to model mega-scale social or biological agents and to simulate their emergent collective behavior that may be difficult only with mathematical and macroscopic approaches. A successful key for simulating megascale agents is to speed up the execution with parallelization. Although many parallelization attempts have been made to multiagent simulations, most work has been done on shared-memory programming environments such as OpenMP, CUDA, and Global Array, or still has left several programming problems specific to distributed-memory systems, such as machine unawareness, ghost space management, and cross-processor agent management (including migration, propagation, and termination). To address these parallelization challenges, we have been developing MASS, a new parallel-computing library for multi-agent and spatial simulation over a cluster of computing nodes. MASS composes a user application of distributed arrays and multi-agents, each representing an individual simulation place or an active entity. All computation is enclosed in each array element or agent; all communication is scheduled as periodic data exchanges among those entities, using machine-independent identifiers; and agents migrate to a remote array element for rendezvousing with each other. Our unique agent-based approach takes advantage of these merits for parallelizing big data analysis using climate change and biological network motif searches as well as individual-based simulation such as neural network simulation and influenza epidemic simulation as practical application examples.

More information about MASS can be found out the University of Washington Distributed Systems Lab [Homepage](http://depts.washington.edu/dslab/MASS).

# Documentation
You may find documentation regarding the Java version of the MASS library in the [Wiki](https://bitbucket.org/mass_library_developers/mass_java_core/wiki/Home). If you want to try out a version of the library that is still in active development, clone the "develop" branch and refer to the [MASS Developer's Guide](https://bitbucket.org/mass_library_developers/mass_java_core/wiki/LibraryDevelopersGuide) for instructions on building the library.

# Getting Started
## Maven-enabled Project
If you are using Maven, add the following repository to your POM:

	<repository>
		<id>uwb-css-dsl-release</id>
		<name>UWB CSS Distributed Systems Lab Maven Repository</name>
		<url>http://depts.washington.edu/dslab/maven</url>
	</repository>

... and add the library as a dependency:

	<dependency>
		<groupId>edu.uw.bothell.css.dsl.mass</groupId>
		<artifactId>mass-core</artifactId>
		<version>1.2.1-RELEASE</version>
		<scope>compile</scope>
	</dependency>

## Non-Maven Project
You may download the JAR from the [Downloads](https://bitbucket.org/mass_library_developers/mass_java_core/downloads/) section of this repository. All dependencies that MASS requires are built into the JAR; you do not need to add anything else to your project.
