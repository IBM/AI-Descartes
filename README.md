# AI Descartes: Combining Data and Theory for Derivable Scientific Discovery

This repository contains the code and the data used for the experiments in the paper [AI Descartes: Combining Data and Theory for Derivable Scientific Discovery](https://arxiv.org/abs/2109.01634).

## Folders description:
* `data`: contains the 3 datasets used in the paper (Kepler’s third law of planetary motion, Einstein’s time-dilation formula, Langmuir’s adsorption equation)
* `reasoning`: contains the code for the Reasoning component of the system
* `symbolic-regression`: contains the code for the Symbolic Regression component of the system

## How to cite

```
@article{AI_Descartes,
author    = {Cristina Cornelio and Sanjeeb Dash and Vernon Austel and Tyler R. Josephson and Joao Goncalves and Kenneth L. Clarkson and Nimrod Megiddo and Bachir El Khadir and Lior Horesh},
title = {AI Descartes: Combining Data and Theory for Derivable Scientific Discovery},
journal   = {CoRR},
volume    = {abs/2109.01634},
year      = {2021},
url       = {https://arxiv.org/abs/2109.01634}
}
```

## Patents
* [**Generative Reasoning for Symbolic Discovery**](https://patents.google.com/patent/US20220108205A1), *C.Cornelio, L.Horesh, V.Pestun, R.Yan*.
* [**Symbolic Model Discovery based on a combination of Numerical Learning Methods and Reasoning**](https://patents.google.com/patent/US20220027775A1/en), *C.Cornelio, L.Horesh, A.Fokoue-Nkoutche, S.Dash*.
* [**Experimental Design for Symbolic Model Discovery**](https://patents.google.com/patent/US20210334432A1/en), *L.Horesh, K.Clarkson, C.Cornelio, S.Magliacane*.


## Approach overview 
Scientists have long aimed to discover meaningful formulae which accurately describe experimental data. One common approach is to manually create mathematical models of natural phenomena using domain knowledge, then fit these models to data. In contrast, machine-learning algorithms automate the construction of accurate data-driven models while consuming large amounts of data. Ensuring that such models are consistent with existing knowledge is an open problem. We develop a method for combining logical reasoning with symbolic regression, enabling principled derivations of models of natural phenomena. We demonstrate these concepts for Kepler’s third law of planetary motion, Einstein’s relativistic time-dilation law, and Langmuir’s theory of adsorption, automatically connecting experimental data with background theory in each case. We show that laws can be discovered from few data points when using formal logical reasoning to distinguish the correct formula from a set of plausible formulas that have similar error on the data. The combination of reasoning with machine learning provides generalizable insights into key aspects of natural phenomena. We envision that this combination will enable derivable discovery of fundamental laws of science. We believe that this is a crucial first step for connecting the missing links in automating the scientific method.

<p align="center">
<img src="https://github.com/IBM/AI-Descartes/tree/main/figures/discovery_cycle.png?raw=true" width="600">
</p>

The figure below describes a more detailed schematic of the system, where the bold lines and boundaries correspond to the system we presented in the paper, and the dashed lines and boundaries refer to standard techniques for scientific discovery (human-driven or artificial) that we have not yet integrated into our current implementation. The present system generates hypotheses from data using symbolic regression, which are posed as conjectures to an automated deductive reasoning system, which proves or disproves them based on background theory or provides reasoning-based quality measures

![alt text](https://github.com/jpgoncal1/AI-Descartes-temp/blob/main/figures/system_figure_v8.png?raw=true)


## Results Summary
We tested the different capabilities of our system with three problems (with real data). 

### First problem: Kepler’s third law of planetary motion
First, we considered the problem of deriving Kepler’s third law of planetary motion, providing reasoning-based measures to analyze the quality and generalizablity of the generated formulae. Extracting this law from experimental data is challenging, especially when the masses involved are of very different magnitudes. This is the case for the solar system, where the solar mass is much larger than the planetary masses. The reasoning module helps in choosing between different candidate formulae and identify the one that generalize better: using our data and theory integration we were able to re-discover Kepler’s third law. 

<p align="center">
<img src="https://github.com/jpgoncal1/AI-Descartes-temp/blob/main/figures/kepler_3d_v3.png?raw=true" width="600">
</p>


### Second problem: Einstein’s time-dilation formula
We then considered Einstein’s time-dilation formula. Although we did not recover this formula from data, we used the reasoning module to identify the formula that generalizes best. Moreover, analyzing the reasoning errors with two different set of axioms (one with “Newtonian” assumptions and one relativistic), we were able to identify the theory that better explain the phenomena. 

<p align="center">
<img src="https://github.com/jpgoncal1/AI-Descartes-temp/blob/main/figures/relativity.png?raw=true" width="600">
</p>

### Third problem: Langmuir’s adsorption equation
We considered Langmuir’s adsorption equation, whose background theory contains material-dependent coefficients. By relating these coefficients to the ones in the SR-generated models via existential quantification, we were able to logically prove one of the extracted formulae.

<p align="center">
<img src="https://github.com/jpgoncal1/AI-Descartes-temp/blob/main/figures/langmuir_equation.png?raw=true" width="400">
</p>
