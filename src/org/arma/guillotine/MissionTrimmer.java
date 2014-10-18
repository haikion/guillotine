/**
 * @author Niko Häikiö
 * Implements removal of elements from mission.sqf file.
 * Writes new mission directory in which AI spawning is transfered
 * to Headless Client.
 */
package org.arma.guillotine;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.arma.sqmparser.Parameter;
import org.arma.sqmparser.SQMParser;
import org.arma.sqmparser.ClassNode;
import org.apache.commons.io.FileUtils;

public class MissionTrimmer {
	private SQMParser parser_;
 	private File inputFile_;
 	private File inputDir_;
	private File outputDir_;
	private String newMissionName_;
	private static Logger logger = Logger.getLogger(MissionTrimmer.class);
	private static final String DIRECTORY_POSTFIX = "_headless";
	private static final String MISSION_POSTFIX = " (HC)";
	private static final String SCRIPT_MARKER = "//HEADLESS_SCRIPT";
	private static final String SCRIPT_FILE_NAME = "spawnHeadlessObjects.sqf";
	//Dedicated ArmA 2 server behaves strangely if mission name size exceeds this.
	private static final int MAX_MISSION_NAME_LENGHT = 62;
	
	public MissionTrimmer(String inputFilePath) {
		readFile(inputFilePath);
		inputFile_ = new File(inputFilePath);
		inputDir_ = inputFile_.getParentFile();
		String missionDirName = inputDir_.getName();
		String mapName = missionDirName.replaceFirst(".*\\.", "");
		String missionsDirPath = inputFile_.getParentFile().getParent();
		//Add postfix to the map name.
		String newDirName = missionDirName.replace("."+mapName, DIRECTORY_POSTFIX+"."+mapName);
		outputDir_ = new File(missionsDirPath+"/"+newDirName);
		//Get current mission name parameter
		Parameter nameParameter = parser_.getMissionRoot().getChildByName("Mission")
				.getChildByName("Intel").getParameter("briefingName");
		newMissionName_ = nameParameter.getValue();
		//Strip quotation marks
		newMissionName_ = newMissionName_.substring(1, newMissionName_.length()-1);
		newMissionName_ = "\"" + newMissionName_+MISSION_POSTFIX+"\"";
		nameParameter.setValue(newMissionName_);
		logger.debug("missionDirName="+missionDirName+
				" mapName="+mapName+" missionsDirPath="+missionsDirPath+
				" newDirName="+newDirName+
				" missionDir="+inputDir_.getAbsolutePath()+
				" newMissionsDir="+outputDir_.getAbsolutePath()+
				" newMissionName_="+newMissionName_
				);
	}
	
	public void readFile(String filePath) {
		parser_ = new SQMParser();
		parser_.parseFile(filePath);		
	}
	
	private String verify() throws IOException {
		File initFile = new File(inputDir_+"/init.sqf");
		if (!initFile.exists())
		{
			return "No init.sqf file found!";
		}
		String init = FileUtils.readFileToString(initFile);
		if (!init.contains(SCRIPT_MARKER)) {
			return "The headless spawn script location is not marked."+
					" Please add line: \""+SCRIPT_MARKER+"\" somewhere in your init.sqf.";
		}
		if (newMissionName_.length() > MAX_MISSION_NAME_LENGHT)
		{
			return "Error: Mission name should not be longer than " +
					Integer.toString(MAX_MISSION_NAME_LENGHT - MISSION_POSTFIX.length()) +
					" characters.";
		}
		return null;
	}
	
	public String writeMission() throws IOException {
		String errorMessage = verify();
		if (errorMessage != null) {
			return errorMessage;
		}
		FileUtils.copyDirectory(inputDir_, outputDir_);
		removeEmptyGroups();
		parser_.write(outputDir_+"/mission.sqm");
		File initFile = new File(outputDir_+"/init.sqf");
		String initString = FileUtils.readFileToString(initFile);
		initString = initString.replaceFirst(SCRIPT_MARKER, "[] execVM \""+SCRIPT_FILE_NAME+"\";");
		FileUtils.writeStringToFile(initFile, initString);
		return null;
	}
	
	public String getOutputDir() {
		return outputDir_.getAbsolutePath();
	}
	
	
	public void writeFile(String filePath) {
		parser_.write(filePath);
	}
	
	private void deleteByClassName(String className) {
		for ( ClassNode classNode : parser_.getAllClasses() ) {
			if ( className.equals(classNode.getName()))
			{
				classNode.delete();
			}
		}
	}
	
	public void deleteMarkers() {
		deleteByClassName("Markers");
	}
	
	public void deleteWaypoints() {
		deleteByClassName("Waypoints");
	}
	
	/**
	 * Updates mission.sqm trigger name
	 */
	public void updateTrigger(Item trigger) {
		Position pos = trigger.getPosition();
		ArrayList<String> values = new ArrayList<String>();
		values.add(pos.getX());
		values.add(pos.getZ());
		values.add(pos.getY());
		ClassNode trg = parser_.getClassByArray("position", values);
		trg.setParameter("name", "\"" + trigger.getName() + "\"");
		logger.debug("Renamed trg name="+trigger.getName());
	}
	
	/**
	 * Reads module's name and updates it to mission.sqm.
	 * To identify a correct module position is used.
	 * @param module is the module to be read from.
	 */
	public void updateModule(Item module) {
		ClassNode sqmMod = parser_.getClassByParameter("id", module.getId());
		module.generatePublicName();
		sqmMod.setParameter("name", "\"" + module.getName() + "\"");
		logger.debug("Renamed module name="+module.getName());
	}
	
	public void deleteTriggers() {
		deleteByClassName("sensors");
	}
	
	public void deleteUnit(String id) {
		parser_.deleteByParameter("id", id);
	}
	
	/**
	 * Adds forceHeadlessClient=1 on the HeadlessSlot
	 * @return true if headlessSlot was found and false if not.
	 */
	public boolean forceHeadlessOnSlot() {
		ClassNode headlessSlot = parser_.getClassByParameter("text", "\"HeadlessSlot\"");
		if ( headlessSlot == null )
		{
			return false;
		}
		headlessSlot.setParameter("forceHeadlessClient", "1");
		return true;
	}
	
	private void removeEmptyGroups() {
		ClassNode groups = parser_.getClassesByName("Groups").get(0);
		ArrayList<ClassNode> groupList = groups.getChildren();
		ArrayList<ClassNode> deletables = new ArrayList<ClassNode>();
		for (ClassNode group : groupList)
		{
			ClassNode vehicles = group.getChildByName("Vehicles");
			if (vehicles != null && vehicles.getChildren().size() == 0)
			{
				deletables.add(group);
			}
		}
		//Direct removal will lead to concurrent editing exception.
		//Remove after search.
		for (ClassNode group : deletables)
		{
			group.delete();
		}
	}
}