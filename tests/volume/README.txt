----------
Scenario 1 : test the Oracle limitation on the number of elements in the clause 'IN' (1 000 elements)

# generate and analyze 1010 projects. Each project has 5 packages and 5*10 classes
mvn clean install -Pfirefox,oracle -DprojectsCount=1010 -DpackagesCount=5 -DclassesCount=10

# NOTE FOR MAC OS : add the parameter -Djava.io.tmpdir=/tmp