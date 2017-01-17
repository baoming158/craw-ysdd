#!/bin/sh
#./etc/profile
min_heap_size="50m"
max_heap_size="100m"
path=$CLASSPATH:./
for file in lib/*.jar;
            do
            cpath=$cpath:$file
            done;
            export CLASSPATH=$cpath
#            echo $CLASSPATH
start()
{
        nohup /usr/local/jdk1.8.0_60/bin/java -Xms$min_heap_size -Xmx$max_heap_size -XX:PermSize=128m -Xloggc:gc.log -XX:+PrintGCTimeStamps -XX:-PrintGCDetails -cp $CLASSPATH -Dprograme=snaputil com.yssd.craw.launch.Launch > nohup.out 2>&1 &
    echo $! > wb.pid
}
stop(){
        kill `cat wb.pid`
 }

case $1 in
"restart")
stop
echo "stop the ysdd process sucess"
start
echo "start the ysdd process sucess"
;;
"start")
start
echo "start the ysdd process sucess"
;;
"stop")
stop
echo "stop the ysdd process sucess"
;;
*) echo "only accept params start|stop|restart" ;;
esac

