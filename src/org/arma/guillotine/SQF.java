/**
 * Generates SQF code
 */

package org.arma.guillotine;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import org.apache.log4j.Logger;
import org.arma.guillotine.Synchronizable.SubTypes;

public class SQF {
	private String code;
	MissionTrimmer missionTrimmer;
	ArrayList<String> publicNames;
	private int groupCountWest;
	private int groupCountEast;
	private int groupCountGuer;
	private int groupCountCiv;
	private ArrayList<Item> modules = new ArrayList<Item>();
	private static Logger logger = Logger.getLogger(SQF.class);
	
	public SQF(SQM sqm) {
		File source = sqm.getSource();
		SQMClass rootType = sqm.getRootType();
		SQMClass triggers = sqm.getTriggers();
		missionTrimmer = sqm.getMissionTrimmer();
		publicNames = sqm.getPublicNames();
		
		SQF sqf = this;
		String code = ""
				+ "/**\n"
				+ " * Converted with Arma2MapConverter v"
				+ Arma2MapConverter.VERSION
				+ "\n"
				+ " *\n"
				+ " * Source: "
				+ source.getAbsolutePath()
				+ "\n"
				+ " * Date: "
				+ DateFormat.getInstance().format(
						Calendar.getInstance().getTime()) + "\n" + " */\n\n";
		code += "private[\"_westHQ\",\"_eastHQ\",\"_guerHQ\",\"_civHQ\",\"_createdUnits\",\"_wp\"];\n\n";
		code += "_westHQ = createCenter west;\n"
				+ "_eastHQ = createCenter east;\n"
				+ "_guerHQ = createCenter resistance;\n"
				+ "_civHQ  = createCenter civilian;\n";

		code += "\n_createdUnits = [];\n";

		code += "\n/*****************\n" + " * UNIT CREATION *\n"
				+ " *****************/\n";
		code += generateSQF(rootType);
		code += "\n/*****************************\n" + " * TRIGGER SYNCHRONIZATION *\n"
				+ "*****************************/\n";
		code += generateSQF(triggers);
		code += "\n/*****************************\n" + " * MODULE SYNCHRONIZATION *\n"
				+ "*****************************/\n";
		code += generateModuleSyncSQF(sqm.getModules());
		code += "\n/****************************\n"
				+  "* BROADCAST PUBLIC NAMES *\n"
				+  "****************************/\n";
		code += generateBroadcastSQF();
		code += "\n// return all created units in an array\n"
				+ "[_createdUnits]\n";
		//TODO: broadcast unit names
		sqf.setCode(code);
	}
	
	private String generateModuleSyncSQF(ArrayList<Item> modules) {
		String code = "";
		for (Item module : modules) {
			module.generatePublicName();
			module.setSubtype(SubTypes.UNIT);
			code += module.getSyncSQF();
		}
		return code;
	}
	
	private String generateBroadcastSQF() {
		String rVal = "";
		for (String name : publicNames) {
			rVal += "publicVariable \"" + name + "\";" + "\n";
		}
		return rVal;
	}
	
	private String getGroupCount(String side) {
		if (side.equals("west")) {
			++groupCountWest;
			return String.valueOf(groupCountWest);
		}
		if (side.equals("east")) {
			++groupCountEast;
			return String.valueOf(groupCountEast);
		}
		if (side.equals("guer")) {
			++groupCountGuer;
			return String.valueOf(groupCountGuer);
		}

		++groupCountCiv;
		return String.valueOf(groupCountCiv);

	}
	
	private String generateSQF(SQMClass typeClass) {
		String code = "";

		for (SQMClass tc : typeClass.getChilds()) {
			if (tc.equals("Sensors")) {
				for (SQMClass items : tc.getChilds()) {

					Item item = (Item) items.getObject();
					item.generatePublicName();
					item.setSubtype(SubTypes.TRIGGER);
					missionTrimmer.updateTrigger(item);
					code += item.getSyncSQF() + "\n";
				}
			}
			if (tc.equals("Waypoints")) {
				int index = 0;
				String groupName = null;
				for (SQMClass tClass : tc.getParent().getChilds()) {
					if (tClass.getType().equals("Vehicles")) {
						groupName = ((Vehicle) tClass.getObject())
								.getGroupName();
					}
				}
				logger.debug("Adding waypoints for group " + groupName);
				code += "\n/**\n" + " * Waypoints for group " + groupName
						+ "\n" + " */\n";
				for (SQMClass items : tc.getChilds()) {
					++index;
					Item item = (Item) items.getObject();
					item.setSubtype(SubTypes.WAYPOINT);
					code += "// waypoint #" + index + "\n";
					code += "_wp = " + groupName + " addWaypoint[["
							+ item.getPosition().getX() + ", "
							+ item.getPosition().getY() + ", 0], "
							+ item.getPlacement() + ", " + index + "];\n";
					String wp = "[" + groupName + ", " + index + "]";
					item.setName(wp);
					if (item.getCombat() != null) {
						code += wp + " setWaypointBehaviour "
								+ item.getCombat() + ";\n";
					}
					if (item.getCombatMode() != null) {
						code += wp + " setWaypointCombatMode "
								+ item.getCombatMode() + ";\n";
					}
					if (item.getCompletionRadius() != null) {
						code += wp + " setWaypointCompletionRadius "
								+ item.getCompletionRadius() + ";\n";
					}
					if (item.getFormation() != null) {
						code += wp + " setWaypointFormation "
								+ item.getFormation() + ";\n";
					}
					if (item.getSpeed() != null) {
						code += wp + " setWaypointSpeed " + item.getSpeed()
								+ ";\n";
					}
					if (item.getExpCond() != null) {
						code += wp + " setWaypointStatements[\""
								+ item.getExpCond() + "\", " + item.getExpActiv()
								+ "];\n";
					}	
					if (item.getType() != null) {
						code += wp + " setWaypointType " + item.getType()
								+ ";\n";
					}
					code +=  item.getSyncSQF()  + "\n";
				}

			}
			
			if (tc.equals("Vehicles")) {

				String side = ((Vehicle) tc.getObject()).getSide()
						.toLowerCase();
				String group = "_group_" + side + "_" + getGroupCount(side);
				((Vehicle) tc.getObject()).setGroupName(group);
				code += "// group " + group + "\n";
				code += "private[\""+group+"\"];\n";
				code += group + " = createGroup _" + side + "HQ;\n";

				for (SQMClass items : tc.getChilds()) {

					Item item = (Item) items.getObject();
					//Do not include player slots or modules
					if ( item.getPlayer() != null ) {
						continue;
					}
					if (item.getSide().equals("LOGIC")) {
						//Registers unit as a logic module for syncing
						//Assures logic module has a name so it can be
						//referenced in the SQF-script
						modules.add(item);
						missionTrimmer.updateModule(item);
						continue;
					}
					item.setSubtype(SubTypes.UNIT);
					
					//Delete unit from the mission.sqm
					missionTrimmer.deleteUnit(item.getId());

					code += "// begin " + item.getName() + ", part of group "
							+ group + "\n";
					if (item.nameIsPrivate()) {
						//Auto-generated units are referenced through private vars
						code += "private[\"" + item.getName() + "\"];\n";
					} else {
						//Public name
						publicNames.add(item.getName());
					}
					code += "if (" + item.getPresenceCondition() + ") then\n{\n";
					if (item.getSide().equals("EMPTY")) {
						code += "\t" + item.getName()
								+ " = createVehicle [" + item.getVehicle()
								+ ", " + item.getPosition()
								+ ", [], 0, " + item.getSpecial() + "];\n";
					} else {
						code += "\n" + "\t"
								+ item.getName()
								+ " = "
								+ group
								+ " createUnit ["
								+ item.getVehicle()
								+ ", "
								+ item.getPosition()
								+ ", [], 0, \"CAN_COLLIDE\"];\n"
								// this is VERY dirty and only used because I don't want to create\n"
								// arrays for vehicles, units and stuff to check if the classname\n"
								// is a vehicle, an unit, and so on. this just works.\n"
								// what it does is if the unit is not alive after creation (because it should be a manned vehicle)\n"
								// it will be created with createVehicle and manned with the BIS_fnc_spawnCrew function.\n"
								+ "\t// Did not spawn as a unit try as a vehicle\n"
								+ "\tif(!alive " + item.getName()
								+ ") then {\n" + "\t\t" + item.getName()
								+ " = createVehicle [" + item.getVehicle()
								+ ", "
								+ item.getPosition()
								+ ", [], 0, \"CAN_COLLIDE\"];\n"
								// + "\t\t_group = createGroup _"
								// + item.getSide().toLowerCase() + "HQ;\n"
								+ "\t\t[" + item.getName()
								// + ", _group] call BIS_fnc_spawnCrew;\n"
								+ ", " + group + "] call BIS_fnc_spawnCrew;\n"
								+ "\t};\n\n";

					}
					if (item.getAzimut() != null) {
						code += "\t" + item.getName() + " setDir "
								+ item.getAzimut() + ";\n";
					}
					if (item.getSkill() != null
							&& !item.getSide().equals("EMPTY")) {
						code += "\t" + item.getName() + " setUnitAbility "
								+ item.getSkill() + ";\n";
					}
					if (item.getRank() != null
							&& !item.getSide().equals("EMPTY")) {
						code += "\t" + item.getName() + " setRank "
								+ item.getRank() + ";\n";
					}
					if (item.getLeader() != null
							&& !item.getSide().equals("EMPTY")) {
						code += "\t" + group + " selectLeader " + item.getName() + ";\n";
					}
					if (item.getInit() != null) {
						code += "\t" + fixInitCode(item.getInit()).replace("this",item.getName()) + "\n";
					}
					code +=  "\t" + item.getSyncSQF() + "\n";
					code += "\t_createdUnits = _createdUnits + ["
							+ item.getName() + "];\n";
					code += "};\n// end of " + item.getName() + "\n";

				}

			} else {
				code += generateSQF(tc);
			}
		}

		return code;
	}
	
	private String fixInitCode(String text) {
		String result = new String(text);
		
		if (result.startsWith("\""))
			result = result.substring(1);
		
		if (result.endsWith("\";"))
			result = result.substring(0, result.length()-2);

		//Init does not need to end with ";" but the code needs to.
		if (!result.endsWith(";"))
			result = result+";";
		
		//Replace Double quotes with single quotes
		result = result.replaceAll("\"\"","\"");
		
		return result;
	}
	
	public void save(File file) throws IOException {
		file.createNewFile();
		FileWriter fw = new FileWriter(file);
		fw.write(code);
		fw.flush();
		fw.close();
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}
	
	public String toString() {
		return getCode();
	}
}
