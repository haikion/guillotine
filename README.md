Guillotine 
==========

Convert ArmA 2 and 3 missions into headless compatible missions.

This program reads a mission.sqm and creates a new mission in which all of the AI units are spawned through headless client. The headless client compatible mission should be identical with the original mission as long as the headless client is present.

Currently working:

- Converting units, groups and manned vehicles
- Converting waypoints
- Converting synchronization 

Usage
=====

	1. Create new mission
	2. Create init.sqf script in the mission directory
	3. Add line: "//HEADLESS_SCRIPT" in the init.sqf. This line is replaced by the headless spawn script execution on conversion.
	4. Open the saved mission.sqm with Guillotine
	5. Done

History	
=======
This program was started as a fork of Arma2MapConverter which was developed by lotherk.
