cd ..
if Not Exist dist/JDEECoVisualizer.jar (
	call ant -buildfile build.xml just_dist
)
call java -cp "dist/JDEECoVisualizer.jar;plugins/*" cz.filipekt.jdcv.Visualizer
cd bin