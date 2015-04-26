cd ..
if [ ! -f dist/JDEECoVisualizer.jar ]; then
    ant -buildfile build.xml just_dist
fi
java -cp "dist/JDEECoVisualizer.jar:plugins/*" cz.filipekt.jdcv.Visualizer
cd bin