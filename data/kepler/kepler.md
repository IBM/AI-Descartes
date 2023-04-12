# PARAMETERS

## SOLAR_SYSTEM 

| Entity    | Label  | Conversion           | Units    |
| ---       |  ---   |    ----              |    --- |
| period    | p		 | 1000 * 24 * 60 * 60 	| period [days/1000] |
| mass      | M 	 | 1.9885 * 10^30		| sun masses |
| mass      | m 	 | 5.972 * 10^24		| Earth masses |
| distance  | d		 | 1.496 * 10^11	    | distance [astronomical units] |


## EXOPLANETS 

| Entity    | Label | Conversion            | Units    |
| ---       |  ---  |    ----               |    --- |
| period    | p 	| 1000 * 24 * 60 * 60	| period [days/1000] |
| mass      | M 	| 1.9885 * 10^30		| sun masses |
| mass      | m 	| 1.898 * 10^27			| Jupiter masses |
| Distance  | d		| 1.496 * 10^11			| distance [astronomical units] |


## BINARY STARS 

| Entity    | Label  | Conversion           | Units    |
| ---       |  ---   |    ----              |    --- |
| period    | p		 | 365 * 24 * 60 * 60 	| period [years] |
| mass      | M 	 | 1.9885 * 10^30		| sun masses |
| mass      | m 	 | 1.9885 * 10^30		| sun masses |
| distance  | d	     | 1.496 * 10^11		| distance [astronomical units] |


# CORRECT FORMULA
```
p^2  = (4 * (pi^2)) * d^3 /(G * (m1 + m2))
```