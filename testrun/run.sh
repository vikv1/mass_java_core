#login password port size maxTime interval processes threads dlbCount
#java -Xmx2g -cp ./DLB.jar:./MASS.jar:./jsch-0.1.44.jar:. Wave2DMass dslab ds1ab-302 32781 500 50 50 1 2 4
java -Xmx2g -cp ./DLB.jar:./MASS.jar:./jsch-0.1.44.jar:. Wave2DMass dslab ds1ab-302 32781 500 50 50 5 4
