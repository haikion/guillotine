/**
 * Reads SQM File
 */

package org.arma.guillotine;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import org.apache.log4j.Logger;

public class SQM {
	private TypeClass units = new TypeClass("units", null);
	private TypeClass triggers = new TypeClass("triggers", null);
	private ArrayList<Item> modules = new ArrayList<Item>();
	private static Logger logger = Logger.getLogger(SQM.class);
	private BufferedReader reader;
	private File source;
	private MissionTrimmer missionTrimmer;
	private ArrayList<String> publicNames = new ArrayList<String>();
	
	public ArrayList<String> getPublicNames() {
		return publicNames;
	}
	
	public TypeClass getRootType() {
		return units;
	}

	public TypeClass getTriggers() {
		return triggers;
	}

	public File getSource() {
		return source;
	}
	
	public ArrayList<Item> getModules() {
		return modules;
	}
	
	public void load(File mission) throws FileNotFoundException {
		//TODO: Integrate with SQMParser
		logger.debug("Loading SQM Mission: " + mission.getAbsolutePath());
		missionTrimmer = new MissionTrimmer(mission.getAbsolutePath());
		this.source = mission;
		reader = new BufferedReader(new FileReader(mission));
		String line;
		try {
			while ((line = reader.readLine()) != null) {
				String input = line.replaceAll("^\\s+", "");
				String type = null;
				if (input.startsWith("class")) {

					String[] spl = line.split(" ", 2);
					type = spl[1];
				}
				if (type != null) {
					if (type.equals("Groups")) {
						logger.debug("Processing groups... ");
						parse(line, units);
						logger.debug("Groups processed. "
								+ units.getFullCount()
								+ " Groups processed.");
					}
					if (type.equals("Sensors")) {
						logger.debug("Processing triggers... ");
						parse(line, triggers);
						logger.debug("triggers processed. "
								+ triggers.getFullCount()
								+ " triggers processed.");
					}
				}

			}
		} catch (IOException e) {
			logger.error(e);
		}

		logger.debug("Loaded.");
	}

	public MissionTrimmer getMissionTrimmer() {
		return missionTrimmer;
	}
	
	/**
	 * This is the parsing algorithm. If you know a better way, feel free to
	 * change it.
	 * 
	 * Please also send your changes to the author.
	 * 
	 * 
	 * @param input
	 * @throws IOException
	 */
	private void parse(String input, TypeClass parent) throws IOException {
		String line = input.replaceAll("^\\s+", "");
		if (line.startsWith("class")) {

			String[] spl = line.split(" ", 2);
			TypeClass typeClass = new TypeClass(spl[1], parent);
			parent.getChilds().add(typeClass);

			while (!(line = reader.readLine().replaceAll("^\\s+", ""))
					.startsWith("}")) {
				parse(line, typeClass);
			}

		}
		if (parent.getType().equals("Groups")) {

		}
		if (parent.toString().startsWith("Vehicles")) {
			if (parent.getObject() == null) {
				parent.setObject(new Vehicle());
			}
			TypeClass p = parent.getParent();
			if (p.toString().startsWith("Item")) {

				((Vehicle) parent.getObject()).setSide(((Item) p.getObject())
						.getSide());

			}

		} else if (parent.toString().startsWith("Waypoints")) {
			Waypoints waypoints = new Waypoints();
			if (parent.getObject() == null) {
				parent.setObject(waypoints);
			}
			TypeClass p = parent.getParent();
			if (p.toString().startsWith("Item")) {

				((Waypoints) parent.getObject()).setSide(((Item) p.getObject())
						.getSide());

			}

		} else if (parent.toString().startsWith("Markers")) {
			if (parent.getObject() == null) {
				parent.setObject(new Markers());
			}
			TypeClass p = parent.getParent();
			if (p.toString().startsWith("Item")) {

				((Markers) parent.getObject()).setSide(((Item) p.getObject())
						.getSide());

			}

		} else if (parent.toString().startsWith("Sensors")) {
			if (parent.getObject() == null) {
				parent.setObject(new Triggers());
			}
			TypeClass p = parent.getParent();
			if (p.toString().startsWith("Item")) {

				((Triggers) parent.getObject()).setSide(((Item) p.getObject())
						.getSide());

			}

		} else if (parent.toString().startsWith("Item")) {
			if (parent.getObject() == null) {
				parent.setObject(new Item(parent.toString()));
			}
			if (line.startsWith("position[]=")) {
				String[] tmp = line.split("=", 2);
				tmp = tmp[1].split(",", 3);
				String x = tmp[0].replaceAll("\\{", "");
				String z = tmp[1];
				String y = tmp[2].replaceAll("\\}\\;", "");
				((Item) parent.getObject())
						.setPosition(new Position(x, y, z));
			}
			if (line.startsWith("id=")) {
				String[] tmp = line.split("=", 2);
				String id = tmp[1].replaceAll("\\;", "");
				((Item) parent.getObject()).setId(id);
			} else if (line.startsWith("side=")) {
				String[] tmp = line.split("=", 2);
				((Item) parent.getObject()).setSide(tmp[1]
						.replaceAll("\\;", ""));
			} else if (line.startsWith("vehicle=")) {
				String[] tmp = line.split("=", 2);
				((Item) parent.getObject()).setVehicle(tmp[1].replaceAll("\\;",
						""));
			} else if (line.startsWith("skill=")) {
				String[] tmp = line.split("=", 2);
				((Item) parent.getObject()).setSkill(tmp[1].replaceAll("\\;",
						""));
			} else if (line.startsWith("leader=")) {
				String[] tmp = line.split("=", 2);
				((Item) parent.getObject()).setLeader(tmp[1].replaceAll("\\;",
						""));
			}  else if (line.startsWith("player=")) {
				String[] tmp = line.split("=", 2);
				((Item) parent.getObject()).setPlayer(tmp[1].replaceAll("\\;",
						""));
			} else if (line.startsWith("init=")) {
				String[] tmp = line.split("=", 2);
				((Item) parent.getObject()).setInit(tmp[1]);
			} else if (line.startsWith("name=")) {
				String[] tmp = line.split("=", 2);
				((Item) parent.getObject()).setName(tmp[1]
						.replaceAll("\\;", ""));
			} else if (line.startsWith("markerType=")) {
				String[] tmp = line.split("=", 2);
				((Item) parent.getObject()).setMarkerType(tmp[1].replaceAll(
						"\\;", ""));
			} else if (line.startsWith("type=")) {
				String[] tmp = line.split("=", 2);
				((Item) parent.getObject()).setType(tmp[1]
						.replaceAll("\\;", ""));
			} else if (line.startsWith("rank=")) {
				String[] tmp = line.split("=", 2);
				((Item) parent.getObject()).setRank(tmp[1]
						.replaceAll("\\;", ""));
			} else if (line.startsWith("presenceCondition=")) {
				String[] tmp = line.split("=", 2);
				((Item) parent.getObject()).setPresenceCondition(tmp[1]
						.replaceAll("\\;", ""));
			} else if (line.startsWith("azimut=")) {
				String[] tmp = line.split("=", 2);
				((Item) parent.getObject()).setAzimut(tmp[1].replaceAll("\\;",
						""));
			} else if (line.startsWith("colorName=")) {
				String[] tmp = line.split("=", 2);
				((Item) parent.getObject()).setColorName(tmp[1].replaceAll(
						"\\;", ""));
			} else if (line.startsWith("fillName=")) {
				String[] tmp = line.split("=", 2);
				((Item) parent.getObject()).setFillName(tmp[1].replaceAll(
						"\\;", ""));
			} else if (line.startsWith("a=")) {
				String[] tmp = line.split("=", 2);
				((Item) parent.getObject()).setA(tmp[1].replaceAll("\\;", ""));
			} else if (line.startsWith("b=")) {
				String[] tmp = line.split("=", 2);
				((Item) parent.getObject()).setB(tmp[1].replaceAll("\\;", ""));
			} else if (line.startsWith("angle=")) {
				String[] tmp = line.split("=", 2);
				((Item) parent.getObject()).setAngle(tmp[1].replaceAll("\\;",
						""));
			} else if (line.startsWith("text=")) {
				String[] tmp = line.split("=", 2);
				((Item) parent.getObject()).setText(tmp[1]
						.replaceAll("\\;", ""));
			} else if (line.startsWith("rectangular=")) {
				String[] tmp = line.split("=", 2);
				((Item) parent.getObject()).setRectangular(tmp[1].replaceAll(
						"\\;", ""));
			} else if (line.startsWith("age=")) {
				String[] tmp = line.split("=", 2);
				((Item) parent.getObject())
						.setAge(tmp[1].replaceAll("\\;", ""));
			} else if (line.startsWith("activationBy=")) {
				String[] tmp = line.split("=", 2);
				((Item) parent.getObject()).setActivationBy(tmp[1].replaceAll(
						"\\;$", ""));
			} else if (line.startsWith("expCond=")) {
				String[] tmp = line.split("=", 2);
				((Item) parent.getObject()).setExpCond(tmp[1].replaceAll(
						"\\;$", ""));
			} else if (line.startsWith("expActiv=")) {
				String[] tmp = line.split("=", 2);
				((Item) parent.getObject()).setExpActiv(tmp[1].replaceAll(
						"\\;$", ""));
			} else if (line.startsWith("expDesactiv=")) {
				String[] tmp = line.split("=", 2);
				((Item) parent.getObject()).setExpDesactiv(tmp[1].replaceAll(
						"\\;$", ""));
			} else if (line.startsWith("interruptable=")) {
				String[] tmp = line.split("=", 2);
				((Item) parent.getObject()).setInterruptable(tmp[1].replaceAll(
						"\\;", ""));
			} else if (line.startsWith("activationType=")) {
				String[] tmp = line.split("=", 2);
				((Item) parent.getObject()).setActivationType(tmp[1]
						.replaceAll("\\;", ""));
			} else if (line.startsWith("timeoutMin=")) {
				String[] tmp = line.split("=", 2);
				((Item) parent.getObject()).setTimeoutMin(tmp[1].replaceAll(
						"\\;", ""));
			} else if (line.startsWith("timeoutMid=")) {
				String[] tmp = line.split("=", 2);
				((Item) parent.getObject()).setTimeoutMid(tmp[1].replaceAll(
						"\\;", ""));
			} else if (line.startsWith("timeoutMax=")) {
				String[] tmp = line.split("=", 2);
				((Item) parent.getObject()).setTimeoutMax(tmp[1].replaceAll(
						"\\;", ""));
			} else if (line.startsWith("placement=")) {
				String[] tmp = line.split("=", 2);
				((Item) parent.getObject()).setPlacement(tmp[1].replaceAll(
						"\\;", ""));
			} else if (line.startsWith("completionRadius=")) {
				String[] tmp = line.split("=", 2);
				((Item) parent.getObject()).setCompletionRadius(tmp[1]
						.replaceAll("\\;", ""));
			} else if (line.startsWith("combatMode=")) {
				String[] tmp = line.split("=", 2);
				((Item) parent.getObject()).setCombatMode(tmp[1].replaceAll(
						"\\;", ""));
			} else if (line.startsWith("formation=")) {
				String[] tmp = line.split("=", 2);
				((Item) parent.getObject()).setFormation(tmp[1].replaceAll(
						"\\;", ""));
			} else if (line.startsWith("speed=")) {
				String[] tmp = line.split("=", 2);
				((Item) parent.getObject()).setSpeed(tmp[1].replaceAll("\\;",
						""));
			} else if (line.startsWith("combat=")) {
				String[] tmp = line.split("=", 2);
				((Item) parent.getObject()).setCombat(tmp[1].replaceAll("\\;",
						""));
			} else if (line.startsWith("description=")) {
				String[] tmp = line.split("=", 2);
				((Item) parent.getObject()).setDescription(tmp[1].replaceAll(
						"\\;", ""));
			} else if (line.startsWith("showWP=")) {
				String[] tmp = line.split("=", 2);
				((Item) parent.getObject()).setShowWP(tmp[1].replaceAll("\\;",
						""));
			} else if (line.startsWith("synchronizations[]=")) {
				((Item) parent.getObject()).setSyncArray(line);
			}
		} else {
			// unsupported class
		}

	}
}
