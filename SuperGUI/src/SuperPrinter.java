import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

public class SuperPrinter {

	public static void printCourse(SuperPoint p, BufferedWriter commandWriter, BufferedWriter mapWriter) {
		printCourse(p, Math.toDegrees(p.getAngle()), commandWriter, mapWriter);
	}

	/**
	 * Prints the course
	 *
	 * @param point
	 *            - current point
	 * @param startAngle
	 *            - current angle (not bearing) in degrees
	 */
	public static void printCourse(SuperPoint point, double startAngle, BufferedWriter commandWriter, BufferedWriter mapWriter) {
		if (point == null) throw new IllegalArgumentException("Null point");

		double destinationAngle = Math.toDegrees(point.getAngle());
		try {
			if (mapWriter != null) {
				mapWriter.write((point.getPoint().x) + "\n");
				mapWriter.write((point.getPoint().y) + "\n");
				mapWriter.write("" + point.isBackwards() + "\n");
				for(SuperAction a : point.getActions()) {
					mapWriter.write(a.toString() + "\n");
				}
				mapWriter.write('\n');
			}

			double currAngle = startAngle;
			double angleDiff;
			for (SuperAction a : point.getActions()) {
				angleDiff = currAngle - Math.toDegrees(a.getAngle()); // -(destAngle - starAngle) angle -> bearing
				currAngle = Math.toDegrees(a.getAngle());
				//				if(a.getAction() == SuperEnum.SHOOT) {
				//					angleDiff += 180;
				//					currAngle += 180;
				//					while(currAngle > 180) currAngle -= 360;
				//					while(currAngle < -180) currAngle += 360;
				//				}
				while(angleDiff > 180) angleDiff -= 360;
				while(angleDiff < -180) angleDiff += 360;

				// turn to command
				if(angleDiff != 0){
					if(commandWriter != null) commandWriter.write("\t\taddSequential(new AutoRotateCommand(" + angleDiff + "));\n");
					System.out.println("Turn " + angleDiff);
				}

				// place gear/shoot
				switch (a.getAction()) {
				case SWITCH:
					if(commandWriter != null) commandWriter.write("\t\taddSequential(new SwitchCommand());\n");
					System.out.println("Place Switch");
					break;
				case SCALE:
					if(commandWriter != null) commandWriter.write("\t\taddSequential(new ScaleCommand());\n");
					System.out.println("Place Scale");
					break;
				case PICKUP:
					if(commandWriter != null) commandWriter.write("\t\taddSequential(new PickupCommand());\n");
					System.out.println("Pickup cube");
					break;
				case ROTATE:
					break;
				}
			}

			if(point.getNext() == null) {
				writeAutoChooser();
				return;
			}

			angleDiff = currAngle - destinationAngle;
			while(angleDiff > 180) angleDiff -= 360;
			while(angleDiff < -180) angleDiff += 360;

			if (angleDiff != 0) {
				if(commandWriter != null) commandWriter.write("\t\taddSequential(new AutoRotateCommand(" + angleDiff + "));\n");
				System.out.println("Turn " + angleDiff);
			}

			// drive distance to next point
			double distance = point.getPoint().distance(point.getNext().getPoint()) * 12;
			if (point.isBackwards()) distance = -distance;

			if (distance != 0) {
				if(commandWriter != null) commandWriter.write("\t\taddSequential(new CourseCorrect(" + distance + "));\n");
				System.out.println("Drive " + distance);
			}


		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println();

		printCourse(point.getNext(), destinationAngle, commandWriter, mapWriter);
	}

	/**
	 * Writes the autoChooser file in the robot code
	 */
	public static void writeAutoChooser(){
		if(!SuperGUI.WRITE_COMMAND)
			return;
		PrintWriter autoWriter;
		try {
			autoWriter = new PrintWriter(SuperGUI.AUTOCHOOSER_LOCATION, "UTF-8");
			File dir = new File(SuperGUI.MAPS_DIRECTORY);
			File[] mapsList =  dir.listFiles();

			autoWriter.write("package org.usfirst.frc.team2537.robot.auto;\n\n");

			if(mapsList != null){
				for(File map : mapsList){
					String mapName = map.getName();
					autoWriter.write("import org.usfirst.frc.team2537.maps." + mapName.substring(0, mapName.length() - 5) + ";\n");
				}
				autoWriter.write("\n");
			}

			autoWriter.write("import edu.wpi.first.wpilibj.command.Command;\n");
			autoWriter.write("import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;\n\n");

			autoWriter.write("public class AutoChooser extends SendableChooser<Command> {\n");
			autoWriter.write("\tpublic AutoChooser() {\n");

			if(mapsList != null){
				for(File map : mapsList){
					String mapName = map.getName();
					mapName = mapName.substring(0, mapName.length() - 5);
					if(mapName.equals("DefaultAuto") || mapName.equals("DriveForward"))
						autoWriter.write("\t\taddDefault(\"" + mapName + "\", new " + mapName + "());\n");
					else
						autoWriter.write("\t\taddObject(\"" + mapName + "\", new " + mapName + "());\n");
				}
			}

			autoWriter.write("\t}\n");
			autoWriter.write("}");

			autoWriter.close();
		} catch (FileNotFoundException | UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}
}
