#/bin/bash
#	Just a small script to KILL all MASS.MProcess process for the
#	user on each of the machines listed in the machinefile.txt file
#	May need to run multiple times to kill all MASS related process  

filename="./machinefile.txt"

for host in `cat ${filename}`
do
	echo -e "\n${host}"
	echo -e   "============="
	tmp=`ssh ${USER}@${host} 'ps -ef  | grep "${USER}"
			' | grep "MASS" | grep -v "grep"` 
	echo -e ">>> $tmp"

	# Kill each MASS process.
	for pNum in `echo $tmp | awk '{print $2}' `
	do
		echo -e "> kill -9 ${pNum}" 
		ssh ${USER}@${host} "kill ${pNum}"
		echo -e "  -- DONE! \n"
	done

	# Remove all aeron shm files for the user.
	ssh ${USER}@${host} "rm -rf /dev/shm/aeron-${USER}-*"
done

echo -e "\n"
