package frc.robot.commands.pathing;

import java.util.function.Function;

import edu.wpi.first.wpilibj2.command.CommandBase;
import frc.robot.Robot;
import frc.robot.RobotContainer;


public class RunPath extends CommandBase {
	private final double arcDivisor = 15;

	private double leftSpeed = 0;
	private double rightSpeed = 0;

	// private double length = -1;

	// private boolean reset = true;

	private Path path;

	private Function<Double, Double> speed;

	// private Animation animation;

	public RunPath(Path path, double speed) {
		// Use addRequirements() here to declare subsystem dependencies
		// eg. addRequirements(RobotContainer.chassis);
		addRequirements(RobotContainer.drive);
		this.path = path;
		this.leftSpeed = -speed;
		this.rightSpeed = -speed;
		this.speed = x -> speed;
		this.schedule(false); //makes it so that this command cannot be interrupted by another command that contains the same requirements
	}

	public RunPath(Path path, Function<Double, Double> speed) {
		// Uses addRequirements() here to declare subsystem dependencies
		// eg. addRequirements(RobotContainer.chassis);
		addRequirements(RobotContainer.drive);
		this.path = path;
		this.speed = speed;
		this.leftSpeed = speed.apply(0.0);
		this.rightSpeed = speed.apply(0.0);
		// this.schedule(false);
	}

	public RunPath(Path path) {
		this(path, path.getSpeed());
	}

	public RunPath(Path path, SpeedGenerator speedGenerator) {
		this(path, speedGenerator.getSpeedFunction(path));
	}

	// public RunPath(Path path, Function<Double, Double> speed, Animation animation) {
	// 	addRequirements(RobotContainer.drive);
	// 	this.path = path;
	// 	this.speed = speed;
	// 	this.leftSpeed = speed.apply(0.0);
	// 	this.rightSpeed = speed.apply(0.0);
	// 	this.animation = animation;

	// 	this.schedule(false);
	// }

	public double dydx(double s) {
		PathSegment segment = path.getPathAtDistance(s);
		return segment.getDerivative().apply((s - path.getTotalOfCompletedPaths(s)) / segment.getLength());
	}

	// Called just before this Command runs the first time
    public void initialize() {
        RobotContainer.drive.setDriveBoth(leftSpeed * .15, rightSpeed * .15);
        RobotContainer.drive.resetBothEncoders();
        RobotContainer.drive.gyroReset();
        System.out.println("RUNPATH INIT");
    }

	private double getDistance() {
		return Math.abs((RobotContainer.drive.getRightDistance() + RobotContainer.drive.getLeftDistance()) / 2);
	}

	private double deltaAngle(double currentAngle) {
		double currentSlope = Math.tan(currentAngle * Math.PI / 180);
		double nextSlope = dydx(getDistance());

		double angle = Math.atan((nextSlope - currentSlope) / (1 + currentSlope * nextSlope)) * 180 / Math.PI;

		System.out.println("m1: " + currentSlope + " m2: " + nextSlope + " dTheta: " + angle);
		System.out.println("Encoder: " + getDistance() + " dydx: " + dydx(getDistance()));
		return angle;
	}

	public double speed() {
		// System.out.println(-speed.apply(getDistance()/path.getTotalLength()));
		return -speed.apply(getDistance() / path.getTotalLength());
	}

	// Called repeatedly when this Command is scheduled to run
    public void execute() {
        double error = -deltaAngle(RobotContainer.drive.getRotation());

        leftSpeed = speed();
        rightSpeed = speed();
        // System.out.println("error: " + error);
        if (Math.abs(getDistance()) > 3) {
            double speed = leftSpeed;

            double ls = (leftSpeed + ((error) / (arcDivisor / Math.abs(speed))));
            double rs = (rightSpeed - (((error) / (arcDivisor / Math.abs(speed)))));
            RobotContainer.drive.setDriveBoth(ls * .69/* < .15 ? .15 : ls *.69 */, rs * .69/* < .15 ? .15 : rs *.69 */);

            // animate based off of distance, from 0.0 to 1.0
            // if (animation != null) {
            // animation.animate(getDistance() / path.getTotalLength());

            // for (Keyframe kf : animation) {
            // // addSequential(kf.getCommandConsumer().getCommand());
            // }
            // }

        } else {
            RobotContainer.drive.setDriveBoth(leftSpeed * .69/* < .15 ? .15 : leftSpeed *.69 */,
                    rightSpeed * .69/* < .15 ? .15 : rightSpeed *.69 */);
        }
    }

	// Make this return true when this Command no longer needs to run execute()
    public boolean isFinished() {
        try {
            // System.out.println(path.getPathAtDistance(RobotContainer.drive.getRightDistance()).getLength());
            return Math.abs(getDistance()) > (path.getTotalLength());
        } catch (Exception e) {
            System.err.println("Unexpected error in RunPath.isFinished()");
            System.err.println(e);
            return true;
        }
    }

	// Called once after isFinished returns true
	protected void end() {
		RobotContainer.drive.setDriveBoth(0, 0);
		// if (animation != null)
		// 	animation.reset();
	}

	// Called when another command which requires one or more of the same
	// subsystems is scheduled to run
	protected void interrupted() {
		System.out.println("RUNPATH INTERRUPTED");
		end();
	}
}