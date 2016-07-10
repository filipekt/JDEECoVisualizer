package cz.filipekt.jdcv.measuring;

import cz.filipekt.jdcv.Visualizer;
import javafx.application.Platform;

/**
 * Encapsulates the procedure whose performance is being measured.
 * 
 * @author Tomas Filipek <tom.filipek@seznam.cz>
 */
class MeasuredProcedure {
	
	/**
	 * Total time elapsed during all runs of the procedure given in 
	 * {@link MeasuredProcedure#run(Visualizer, Parameters)}. Can be reset to zero
	 * using {@link MeasuredProcedure#resetTotalTime()}.
	 */
	private long totalTime = 0;
	
	/**
	 * @return Total time elapsed during all runs of the procedure given in 
	 * {@link MeasuredProcedure#run(Visualizer, Parameters)}, starting from the
	 * Initialization or last call to {@link MeasuredProcedure#resetTotalTime()}.
	 */
	public long getTotalTime() {
		return totalTime;
	}

	/**
	 * Resets the counter of the total elapsed time {@link MeasuredProcedure#totalTime}
	 * to zero.
	 */
	public void resetTotalTime() {
		this.totalTime = 0L;
	}

	/**
	 * The procedure whose performance we measure. Starts the application with given 
	 * parameters and measures, how long it takes to load the visualization. The result
	 * is added to the total elapsed time held in {@link MeasuredProcedure#totalTime}.
	 */
	public void run(final Visualizer visualizer, final Parameters par) throws InterruptedException {
		visualizer.renewLatch();
		Platform.runLater(new Runnable() {
			
			@Override
			public void run() {
				visualizer.setParams(par);
				visualizer.processParameters();
			}
		});
		totalTime += visualizer.getMeasuredTime();
		Platform.runLater(new Runnable() {
			
			@Override
			public void run() {
				visualizer.clickCloseScene();
			}
		});
	}
	
}