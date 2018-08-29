#!/bin/bash
#Author: Moses Ike. http://mosesike.org
#This script needs 2 argument. path to config file, and netid

PROG=Project2

#command line arguments
CONFIG=$1
netid=$2

#clear a custom debug file b4 each run/test
echo -e "" > debug.txt

#Something the TA is making us do
config_file_name=$(echo $CONFIG | rev | cut -f1 -d"/" | rev | cut -f1 -d".") #without extension

# extract the important lines from the config file. the ones with no '#' or empty lines
sed -e "s/#.*//" $CONFIG | sed -e "/^\s*$/d" > temp
# insert a new line to EOF # necessary for the while loop
echo  >> temp

quorum_cat=$(awk '/dc/{y=1;next}y' temp | tr '\n','\t' ' ')

node_count=0
nodes_location="" #Stores a # delimited string of Location of each node
host_names=() #Stores the hostname of each node
neighbors_dict=() # Stores the Token path of each node

central_process_hostname="csgrads1"
central_process_port="8888"


current_line=1
# Reading from the temp file created above
while read line; 
do
	#turn all spaces to single line spaces
	line=$(echo $line | tr -s ' ')
########Extract Number of nodes and, min and max per Active
	if [ $current_line -eq 1 ]; then
		#number of nodes
		node_count=$(echo $line | cut -f1 -d" ")
		#convert it to an integer
  		let node_count=$node_count+0 
  		
  		#root
  		interdelay=$(echo $line | cut -f2 -d" ")
		
		csEx=$(echo $line | cut -f3 -d" ")

		reqCount=$(echo $line | cut -f4 -d" ")
  		
  	else
#########Extract Location of each node
  		if [ $current_line -le $(expr $node_count + 1) ]; then
  			nodes_location+=$( echo -e $line"#" )	
  			node_id=$(echo $line | cut -f1 -d" ")
  			hostname=$(echo $line | cut -f2 -d" ")
  			host_names[$node_id]="$hostname"	
  		else
###########Extract Neighbors aka quorum
			
			let node_id=$current_line-$node_count-2
  			neighbors=$(echo $line)
  			neighbors_dict+=(['"$node_id"']="$neighbors")
  		fi
  	fi
  	let current_line+=1
done < temp

cd $(pwd)
java -jar CentralProcess.jar $central_process_port $node_count $reqCount &

# iterate through the date collected above and execute on the remote servers
for node_id in $(seq 0 $(expr $node_count - 1))
do
	host=${host_names[$node_id]}
	neighbors=${neighbors_dict["$node_id"]}
	#echo $netid@$host "java $PROG $node_id '$nodes_location' '$neighbors' '$root' '$maxPerActive' \
	#'$sendMinDelay' '$snapshotDelay' '$maxNumber' '$config_file_name' " &
	ssh -o StrictHostKeyChecking=no $netid@$host "cd $(pwd); java -jar AOS_2.jar  $node_id '$nodes_location' '$neighbors' '$node_count' '$interdelay' '$csEx' '$reqCount' '$config_file_name' '$quorum_cat' '$central_process_port' '$central_process_hostname'>> $node_id.out 2>&1" &
	#java -jar AOS_1.jar  '$node_id' '$nodes_location' '$neighbors' '$node_count' '$interdelay' '$csEx' '$reqCount' '$config_file_name' '$quorum_cat'
done

echo "Program running in background. Could take a few moments depending on request count. Result will be printed to this terminal."
#sample output
#mji120030@dc45 java Project2 0 '0 dc45 19999#1 dc44 19998#2 dc43 19997#3 dc42 19996#4 dc41 19995#5 dc40 19898#' \
#'1 3' '6' '10' 	'100' '2000' '15' 'config' 
